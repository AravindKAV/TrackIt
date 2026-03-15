package com.upipulse

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.upipulse.domain.usecase.ObserveTrackingSettingsUseCase
import com.upipulse.ui.TrackItAppRoot
import com.upipulse.ui.theme.TrackItTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var observeTrackingSettingsUseCase: ObserveTrackingSettingsUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settings = runBlocking { observeTrackingSettingsUseCase().first() }
        val isLockEnabled = settings.lockEnabled

        setContent {
            var isUnlocked by remember { mutableStateOf(!isLockEnabled) }
            val currentSettings by observeTrackingSettingsUseCase().collectAsState(initial = settings)
            
            TrackItTheme(appTheme = currentSettings?.theme ?: com.upipulse.domain.model.AppTheme.SYSTEM) {
                if (isUnlocked) {
                    TrackItAppRoot()
                } else {
                    // Blank screen while authenticating
                }
            }

            LaunchedEffect(isUnlocked) {
                if (!isUnlocked) {
                    showBiometricPrompt {
                        isUnlocked = true
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        finish()
                    } else {
                        Toast.makeText(applicationContext, "Auth error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("TrackIt Lock")
            .setSubtitle("Unlock to manage your expenses")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
