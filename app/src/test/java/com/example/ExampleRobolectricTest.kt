package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Store Manager", appName)
    }

    @Test
    fun `pin hashing verifies valid and rejects invalid`() {
        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPin("4829", salt)
        assertTrue(CryptoManager.verifyPin("4829", salt, hash))
        assertFalse(CryptoManager.verifyPin("9999", salt, hash))
    }
}
