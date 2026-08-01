package com.alphagroup.surakshak.security

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "IntegrityManager"
    }

    enum class IntegrityStatus {
        MEETS_DEVICE_INTEGRITY,
        MEETS_BASIC_INTEGRITY,
        UNTRUSTED_DEVICE,
        UNKNOWN
    }

    suspend fun checkIntegrity(nonce: String): IntegrityStatus {
        try {
            val integrityManager = IntegrityManagerFactory.create(context)
            
            // Request integrity token
            val integrityTokenResponse = integrityManager.requestIntegrityToken(
                IntegrityTokenRequest.builder()
                    .setNonce(nonce)
                    // .setCloudProjectNumber(YOUR_PROJECT_NUMBER) // Required for real implementation
                    .build()
            ).await()

            val token = integrityTokenResponse.token()
            Log.d(TAG, "Integrity Token received: ${token.take(20)}...")
            
            // In a real app, you send this token to your server for decryption and verification.
            // For this project, we'll simulate a successful check since we don't have a backend.
            return IntegrityStatus.MEETS_DEVICE_INTEGRITY
            
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check failed", e)
            return IntegrityStatus.UNKNOWN
        }
    }
}
