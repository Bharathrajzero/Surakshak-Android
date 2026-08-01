package com.alphagroup.surakshak.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.ConfirmationCallback
import android.security.ConfirmationPrompt
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrongBoxKeyManager @Inject constructor(
    private val packageManager: PackageManager
) {
    companion object {
        private const val KEY_ALIAS = "SurakshakProvenanceKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    enum class SecurityLevel {
        STRONGBOX,
        TEE,
        SOFTWARE,
        UNKNOWN
    }

    fun isProtectedConfirmationSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ConfirmationPrompt.isSupported(context)
        } else {
            false
        }
    }

    fun signWithConfirmation(
        context: Context,
        dataToSign: ByteArray,
        promptText: String,
        executor: Executor,
        onSuccess: (ByteArray) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onFailure("Protected Confirmation not supported on this API level")
            return
        }

        val callback = object : ConfirmationCallback() {
            override fun onConfirmed(dataThatWasConfirmed: ByteArray) {
                super.onConfirmed(dataThatWasConfirmed)
                try {
                    // In a real implementation, we sign 'dataThatWasConfirmed' 
                    // which contains the prompt text and extra data
                    val signature = signData(dataThatWasConfirmed)
                    onSuccess(signature)
                } catch (e: Exception) {
                    onFailure(e.message ?: "Signing failed")
                }
            }

            override fun onDismissed() {
                super.onDismissed()
                onFailure("Prompt dismissed")
            }

            override fun onCanceled() {
                super.onCanceled()
                onFailure("Prompt canceled")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                onFailure(e.message ?: "Unknown error")
            }
        }

        val prompt = ConfirmationPrompt.Builder(context)
            .setPromptText(promptText)
            .setExtraData(dataToSign)
            .build()

        prompt.presentPrompt(executor, callback)
    }

    fun getSecurityLevel(): SecurityLevel {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) generateKey()
        
        val key = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey ?: return SecurityLevel.UNKNOWN
        val factory = java.security.KeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
        val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (keyInfo.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> SecurityLevel.STRONGBOX
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> SecurityLevel.TEE
                else -> SecurityLevel.SOFTWARE
            }
        } else {
            // Fallback for API 28-30
            if (keyInfo.isInsideSecureHardware) {
                // If we know the device has FEATURE_STRONGBOX_KEYSTORE, it's likely StrongBox
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && 
                    packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                    SecurityLevel.STRONGBOX
                } else {
                    SecurityLevel.TEE
                }
            } else {
                SecurityLevel.SOFTWARE
            }
        }
    }

    fun generateKey(requireAuthentication: Boolean = false) {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))

        if (requireAuthentication) {
            builder.setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1) // Requires auth for every use
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                builder.setIsStrongBoxBacked(true)
            }
            // Attestation challenge (required for extracting attestation certificate)
            builder.setAttestationChallenge("SurakshakAttestation".toByteArray())
        }

        kpg.initialize(builder.build())
        kpg.generateKeyPair()
    }

    fun getAttestationCertificateChain(): List<X509Certificate> {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val certificates = keyStore.getCertificateChain(KEY_ALIAS)
        return certificates?.map { it as X509Certificate } ?: emptyList()
    }

    /**
     * Creates a Signature object initialized for signing with the provenance key.
     * Useful for BiometricPrompt.CryptoObject.
     */
    fun getSignatureObject(): Signature? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey ?: return null
        
        return Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
        }
    }

    fun signData(data: ByteArray): ByteArray {
        val signature = getSignatureObject() ?: throw IllegalStateException("Key not found")
        return signature.run {
            update(data)
            sign()
        }
    }
}
