package com.example.smartautofiller.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PinManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context, "secure_pin_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PIN             = "user_pin"
        private const val KEY_ATTEMPTS        = "wrong_attempts"
        private const val KEY_LOCKED_UNTIL    = "locked_until"
        private const val KEY_LOCK_TIMEOUT    = "lock_timeout_mins"
        private const val MAX_ATTEMPTS        = 5
        private const val LOCKOUT_DURATION_MS = 30_000L  // 30 seconds
    }

    // ── PIN ───────────────────────────────────────────────────
    fun isPinSet(): Boolean = prefs.getString(KEY_PIN, null) != null

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
        resetAttempts()
    }

    fun clearPin() { prefs.edit().remove(KEY_PIN).apply() }

    // ── Verify with attempt limit ─────────────────────────────
    fun verifyPin(pin: String): VerifyResult {
        // Check lockout
        val lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0)
        val now = System.currentTimeMillis()
        if (now < lockedUntil) {
            val remainingSecs = ((lockedUntil - now) / 1000).toInt()
            return VerifyResult.LockedOut(remainingSecs)
        }

        val savedPin = prefs.getString(KEY_PIN, null) ?: return VerifyResult.NoPinSet

        return if (savedPin == pin) {
            resetAttempts()
            VerifyResult.Success
        } else {
            val attempts = incrementAttempts()
            val remaining = MAX_ATTEMPTS - attempts
            if (attempts >= MAX_ATTEMPTS) {
                // Lock kar do
                prefs.edit().putLong(KEY_LOCKED_UNTIL, now + LOCKOUT_DURATION_MS).apply()
                resetAttempts()
                VerifyResult.LockedOut(30)
            } else {
                VerifyResult.WrongPin(remaining)
            }
        }
    }

    private fun incrementAttempts(): Int {
        val current = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        prefs.edit().putInt(KEY_ATTEMPTS, current).apply()
        return current
    }

    private fun resetAttempts() {
        prefs.edit().putInt(KEY_ATTEMPTS, 0).putLong(KEY_LOCKED_UNTIL, 0).apply()
    }

    fun getRemainingLockoutSeconds(): Int {
        val lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0)
        val remaining = (lockedUntil - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
    }

    // ── Biometric ─────────────────────────────────────────────
    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)
    fun setBiometricEnabled(enabled: Boolean) { prefs.edit().putBoolean("biometric_enabled", enabled).apply() }

    // ── Lock Timeout ──────────────────────────────────────────
    // 0 = instant, 1 = 1 min, 5 = 5 min, 15 = 15 min
    fun getLockTimeoutMins(): Int = prefs.getInt(KEY_LOCK_TIMEOUT, 0)

    fun setLockTimeoutMins(mins: Int) {
        prefs.edit().putInt(KEY_LOCK_TIMEOUT, mins).apply()
    }

    fun getLockTimeoutMillis(): Long = getLockTimeoutMins() * 60 * 1000L
}

sealed class VerifyResult {
    object Success : VerifyResult()
    object NoPinSet : VerifyResult()
    data class WrongPin(val attemptsRemaining: Int) : VerifyResult()
    data class LockedOut(val secondsRemaining: Int) : VerifyResult()
}