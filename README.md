# Forest Flow (元气森林 Chi Forest) — Android

A native Android app (Kotlin + Jetpack Compose) for Chi Forest smart vending machines on the XJTLU campuses.
Students find chilled-drink nodes on a real campus map, lock a coil in escrow, show a QR code at the
machine, get restock alerts and use a VIP pass. Suppliers watch telemetry, dispatch runners, approve
clearance prices and confirm restocks.

Accounts and each account's data live in **Supabase** (Postgres + Auth), so an account created on one phone signs in
on any other phone and finds its points, escrow and saved stations there. The app keeps a copy in an on-device
**SQLite database** and works fully offline too: with no Supabase project configured it runs on SQLite alone.
Package name: `forest.flow`.

---

## Run it on your phone (one button)

1. Open **Android Studio** → **File ▸ Open…** → choose this `ForestFlowAndroid` folder.
   Wait for "Gradle sync" to finish (first time downloads dependencies, a few minutes).
2. On the phone: **Settings ▸ About phone ▸ tap _Build number_ 7 times**, then
   **Settings ▸ System ▸ Developer options ▸ USB debugging = On**. Plug the phone in by USB and tap **Allow**.
   (Or use *Wireless debugging* → Android Studio ▸ *Pair Devices Using Wi-Fi*.)
3. Pick your phone (or the *ForestFlow Pixel API 36* emulator) in the device drop-down at the top of Android Studio and press **▶ Run 'app'**.
   Android Studio compiles, installs and opens Forest Flow on the phone.

Requirements: Android 8.0 (API 26) or newer. Android Studio 2026.1 or newer (uses its bundled JDK 21).

## Demo logins

| Path | How |
|---|---|
| WeChat one-tap | Tick the agreement, tap the green WeChat button (demo guest session) |
| SMS | Any valid mainland number (e.g. `13800138000`) → **获取验证码** → code `482915` auto-fills → Sign In |
| **XJTLU CAS (real accounts: Supabase, or SQLite offline)** | `student@xjtlu.test` / `forest123` (Student) · `supplier@xjtlu.test` / `forest123` (Supplier). Or tap *Create account* |
| Visitor | Visitor tile (demo guest session) |

Choose **Student** or **Supplier** with the switch at the top of the login screen first. A CAS account only
opens the app that matches its role ("This is a Supplier account" otherwise).

## Demo-day script

* **Student** → Inventory: *Release* / *Lock* the escrow, *Notify Me* on Osmanthus, *I'm Here*, *Route*.
* Campus Map: pinch, pan, double-tap, flick; search `library`, `AIAC`, `图书馆`, `CB`, `Design`;
  switch **Taicang 太仓 / SIP 园区**; tap a pin → *Directions* (dashed walking line) → *View Stock*;
  *Layers* → Entrances / Points of interest; compass resets the view; locate button finds *You Are Here*.
* Notifications: filters, *Navigate & Hold*, *Re-order Now*, unsubscribe, preferences.
* VIP Pass: live claim countdown, real scannable QR (`chiforest://escrow/N01-3-CF9204`), *COPY* backup code,
  *Redeem Points* (deducts points).
* **Supplier** → Telemetry: tap coil **C3** → *Restock*; tap bars; *Dispatch Runner*.
  Dispatch: *Ping*, *Approve Price Cut*, *Simulate Foot-Traffic Surge*, *Confirm Restock Loaded*.
  Alerts: badge clears on open, filter chips, mute switch, new events rise into the feed.
* Avatar (top right) → profile sheet: **Recent activity read from the SQLite `activity_log` table**,
  *Reset demo data*, *Sign out*.

State survives closing the app (kill it and reopen: same tab, same escrow countdown, same points).

## The database (backend)

`forestflow.db` (SQLite, Android framework API, no extra libraries):

| Table | Holds |
|---|---|
| `users` | Local accounts. Passwords stored only as **PBKDF2-HMAC-SHA256** (random 16-byte salt, 120 000 iterations). |
| `app_state` | One-row JSON snapshot of the whole app state, written after every change |
| `activity_log` | Append-only audit: logins, locks, releases, redemptions, restocks, surges … |

See it live: Android Studio ▸ **View ▸ Tool Windows ▸ App Inspection ▸ Database Inspector** while the app runs.

### Shared accounts and cloud data (Supabase)

With a Supabase project connected, **anyone can create an account and use it on any phone**: the account lives in
Supabase Auth, and each account's data is a row in Postgres protected by Row Level Security (a user can only read
and write their own rows). Local SQLite stays as the offline copy; on sign-in the newer of the phone's copy and the
cloud copy wins, and changes reach the cloud about 1.5 s after the last tap. The two demo accounts are created in
the cloud automatically the first time someone signs in with them.

1. Create a free project at <https://supabase.com> (Dashboard ▸ New project).
2. **SQL Editor ▸ New query**, paste the whole of [`supabase/schema.sql`](supabase/schema.sql), **Run**.
   It creates `profiles`, `user_state`, `activity_log` and `delete_my_account()`, all with Row Level Security.
3. **Authentication ▸ Providers ▸ Email**: keep Email **on** and switch **Confirm email off** for the demo
   (if it stays on, the app tells the user to confirm the e-mail first, then sign in).
4. **Project Settings ▸ API**: copy the *Project URL* and the *anon / publishable key*.
5. Add both to `local.properties` (not committed), then **Sync Gradle and Run**:

   ```properties
   supabase.url=https://YOUR-PROJECT.supabase.co
   supabase.anonKey=YOUR-ANON-OR-PUBLISHABLE-KEY
   ```

   (or put them in `supabase.properties`, see `supabase.properties.example`, or in the `SUPABASE_URL` /
   `SUPABASE_ANON_KEY` environment variables). The anon key is meant to be public; Row Level Security is what
   protects the data. Never put the `service_role` key in the app.

Without those two values the app builds and runs fully offline on SQLite. The profile sheet shows which
backend is active (*XJTLU CAS · Supabase* or *SQLite*).

**Try it without an account:** `python3 tools/fake_supabase.py` starts a local stand-in that speaks the same
protocol; run the app or tests against it with
`SUPABASE_URL=http://10.0.2.2:54321 SUPABASE_ANON_KEY=test-anon-key ./gradlew connectedDebugAndroidTest`
(the emulator reaches the host as `10.0.2.2`).

## Campus map

Real XJTLU maps: Taicang (`tc_campus.png`, from the official illustrated map) and SIP (`sip_campus.png`,
rendered from *XJTLU-SIP-Campus-Map-March2026-v2.pdf*). Buildings, entrances, points of interest and the
11 vending nodes are in `app/src/main/assets/maps/map_data.json` as fractions of image width/height —
edit that file to move a machine. Node locations are **demo placeholders** (`"demo": true`).

Directions draw a **straight** dashed line from *You Are Here* to the node with the walking time: the maps
are illustrations without a path network, so there is no turn-by-turn routing.

## Tests

* `./gradlew test` — 73 JVM tests: every reducer action, map geometry (clamp, rubber-band, momentum
  projection), bilingual search, password hashing, and that every vending node sits on its own building in `map_data.json`.
* `./gradlew connectedDebugAndroidTest` (phone or emulator attached) — 15 instrumented tests (+3 cloud tests when Supabase is configured) on the real
  SQLite backend (tables, auth, persistence across restarts, escrow expiry after reopen, role mismatch) and
  end-to-end UI flows that tap through every Student and Supplier feature, and real multi-touch on the
  map (double-tap zoom, pinch in/out, pan, an over-the-edge fling that must settle inside the image).
* `./gradlew lint` — no issues.

* `SupabaseBackendTest` (11 JVM tests) runs the Supabase client against an in-process fake that speaks the real
  GoTrue / PostgREST protocol: sign-up and sign-in on a second phone, friendly errors, the e-mail-confirmation
  switch, demo-account provisioning, per-account Row Level Security, token refresh, retries, offline, account deletion.
* `SupabaseCloudTest` (3 instrumented tests) runs when the build has a Supabase URL and key: accounts across phones,
  and the app's data following the account to a brand-new phone (no local database, no saved session).

* `./gradlew connectedReleaseAndroidTest -PtestRelease` — the same instrumented tests against the R8-minified
  release build (test-only keep rules in `app/proguard-test-rules.pro`).

When running UI tests on a phone, keep the screen unlocked and awake (Developer options ▸ Stay awake).

All of the above pass on a physical phone (Nothing A059, Android 17 / API 37) — debug and release builds — and on an Android 16 / API 36 emulator.
Build outputs are written to `app/build.nosync/` (kept out of iCloud sync).

## Project layout

```
app/src/main/java/com/chiforest/forestflow/
  MainActivity.kt, AppRoot.kt        shell, effects (toast, haptics, clipboard, share), sheets
  core/       AppState, Action, Reducer (pure), AppViewModel, ForestDatabase (SQLite), MapData, seed
  auth/       AuthRepository, LocalAuthRepository (SQLite + PBKDF2), SupabaseApi + SupabaseBackend (Auth + cloud sync)
  ui/theme/   colour tokens (light + dark), type ramp, motion springs
  ui/components/  glass nav pill, glass header, toasts, sheets, segmented control, switch, QR, charts, icons
  feature/login, feature/customer (Inventory, Map, Notifications, VIP), feature/supplier (Telemetry, Dispatch, Alerts)
docs/DECISIONS.md   every judgment call
docs/PARITY.md      action → screen → test map
```

Fonts: Plus Jakarta Sans, Inter, JetBrains Mono (SIL OFL). Libraries: Compose, Haze (glass blur), ZXing (QR),
kotlinx.serialization, optional Supabase. All free and open source.
