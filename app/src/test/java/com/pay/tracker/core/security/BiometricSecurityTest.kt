package com.pay.tracker.core.security

import com.pay.tracker.ui.viewmodel.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricSecurityTest {

    @Test
    fun biometricPreferencesConstants_areCorrect() {
        assertEquals("pay_tracker_prefs", SettingsViewModel.PREFS_NAME)
        assertEquals("pref_biometric_lock_enabled", SettingsViewModel.KEY_BIOMETRIC_LOCK)
    }
}
