package com.chiforest.forestflow

import com.chiforest.forestflow.auth.AuthRules
import com.chiforest.forestflow.auth.Passwords
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordsTest {
    @Test fun hash_verifies_and_rejects() {
        val salt = Passwords.newSalt()
        val h = Passwords.hash("forest123", salt, 1000)
        assertTrue(Passwords.verify("forest123", salt, h, 1000))
        assertFalse(Passwords.verify("forest124", salt, h, 1000))
        assertNotEquals(h, Passwords.hash("forest123", Passwords.newSalt(), 1000))
        assertFalse(h.contains("forest123"))
    }

    @Test fun rules() {
        assertNull(AuthRules.validate("student@xjtlu.test", "forest123"))
        assertNotNull(AuthRules.validate("nope", "forest123"))
        assertNotNull(AuthRules.validate("a@b.cn", "123"))
    }
}
