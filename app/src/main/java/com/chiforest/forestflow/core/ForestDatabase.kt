package com.chiforest.forestflow.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class UserRow(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: Role,
    val salt: String,
    val hash: String,
    val iterations: Int,
    val createdAt: Long,
)

data class LogRow(val id: Long, val ts: Long, val role: String, val action: String, val detail: String)

/**
 * On-device SQLite database (inspect it live in Android Studio: App Inspection → Database Inspector).
 *
 * Tables
 *  - users         local accounts, PBKDF2 password hashes (never plain text)
 *  - app_state     one-row JSON snapshot of AppState so everything survives restarts
 *  - activity_log  append-only audit of meaningful actions (locks, redeems, restocks …)
 */
class ForestDatabase(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users (
                uid TEXT PRIMARY KEY,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                display_name TEXT NOT NULL,
                role TEXT NOT NULL,
                salt TEXT NOT NULL,
                hash TEXT NOT NULL,
                iterations INTEGER NOT NULL,
                created_at INTEGER NOT NULL)""",
        )
        db.execSQL("CREATE TABLE app_state (id INTEGER PRIMARY KEY CHECK (id = 1), json TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL(
            """CREATE TABLE activity_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts INTEGER NOT NULL,
                role TEXT NOT NULL,
                action TEXT NOT NULL,
                detail TEXT NOT NULL)""",
        )
        db.execSQL("CREATE INDEX idx_activity_ts ON activity_log(ts DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS app_state")
        db.execSQL("DROP TABLE IF EXISTS activity_log")
        onCreate(db)
    }

    // ---- app_state ----
    fun loadState(): String? = readableDatabase.rawQuery("SELECT json FROM app_state WHERE id = 1", null).use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }

    fun saveState(json: String, now: Long) {
        val v = ContentValues().apply {
            put("id", 1)
            put("json", json)
            put("updated_at", now)
        }
        writableDatabase.insertWithOnConflict("app_state", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---- activity_log ----
    fun log(ts: Long, role: String, action: String, detail: String) {
        val v = ContentValues().apply {
            put("ts", ts)
            put("role", role)
            put("action", action)
            put("detail", detail)
        }
        writableDatabase.insert("activity_log", null, v)
    }

    fun recentLogs(limit: Int): List<LogRow> =
        readableDatabase.rawQuery(
            "SELECT id, ts, role, action, detail FROM activity_log ORDER BY id DESC LIMIT ?",
            arrayOf(limit.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(LogRow(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4)))
            }
        }

    fun logCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM activity_log", null).use { c ->
        if (c.moveToFirst()) c.getInt(0) else 0
    }

    // ---- users ----
    fun findUser(email: String): UserRow? = readableDatabase.rawQuery(
        "SELECT uid, email, display_name, role, salt, hash, iterations, created_at FROM users WHERE email = ? COLLATE NOCASE",
        arrayOf(email.trim()),
    ).use { c ->
        if (!c.moveToFirst()) null else UserRow(
            uid = c.getString(0), email = c.getString(1), displayName = c.getString(2),
            role = runCatching { Role.valueOf(c.getString(3)) }.getOrDefault(Role.Student),
            salt = c.getString(4), hash = c.getString(5), iterations = c.getInt(6), createdAt = c.getLong(7),
        )
    }

    fun insertUser(u: UserRow): Boolean {
        val v = ContentValues().apply {
            put("uid", u.uid)
            put("email", u.email.trim().lowercase())
            put("display_name", u.displayName)
            put("role", u.role.name)
            put("salt", u.salt)
            put("hash", u.hash)
            put("iterations", u.iterations)
            put("created_at", u.createdAt)
        }
        return writableDatabase.insertWithOnConflict("users", null, v, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    fun userCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM users", null).use { c ->
        if (c.moveToFirst()) c.getInt(0) else 0
    }

    companion object {
        const val NAME = "forestflow.db"
        const val VERSION = 1
    }
}
