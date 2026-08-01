package com.alphagroup.surakshak.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphagroup.surakshak.c2pa.C2PAManifestBuilder
import com.alphagroup.surakshak.c2pa.ProvenanceReport
import com.alphagroup.surakshak.camera.CameraManager
import com.alphagroup.surakshak.reporting.PdfReportGenerator
import com.alphagroup.surakshak.security.StrongBoxKeyManager
import com.alphagroup.surakshak.storage.SecureStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    val strongBoxKeyManager: StrongBoxKeyManager,
    private val c2paManifestBuilder: C2PAManifestBuilder,
    private val secureStorageManager: SecureStorageManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs by lazy {
        val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
        androidx.security.crypto.EncryptedSharedPreferences.create(
            "surakshak_vault_prefs",
            masterKeyAlias,
            context,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _hasVaultPin = MutableStateFlow(false)
    val hasVaultPin: StateFlow<Boolean> = _hasVaultPin.asStateFlow()

    private val _securityLevel = MutableStateFlow(StrongBoxKeyManager.SecurityLevel.UNKNOWN)
    val securityLevel: StateFlow<StrongBoxKeyManager.SecurityLevel> = _securityLevel.asStateFlow()

    private val _capturedImageUri = MutableStateFlow<Uri?>(null)
    val capturedImageUri: StateFlow<Uri?> = _capturedImageUri.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isVaultEnabled = MutableStateFlow(false)
    val isVaultEnabled: StateFlow<Boolean> = _isVaultEnabled.asStateFlow()

    private val _isProtectedMode = MutableStateFlow(false)
    val isProtectedMode: StateFlow<Boolean> = _isProtectedMode.asStateFlow()

    private val _isPrivacyShieldEnabled = MutableStateFlow(false)
    val isPrivacyShieldEnabled: StateFlow<Boolean> = _isPrivacyShieldEnabled.asStateFlow()

    private val _provenanceReport = MutableStateFlow<ProvenanceReport?>(null)
    val provenanceReport: StateFlow<ProvenanceReport?> = _provenanceReport.asStateFlow()

    private val _vaultItems = MutableStateFlow<List<Uri>>(emptyList())
    val vaultItems: StateFlow<List<Uri>> = _vaultItems.asStateFlow()

    private val _publicItems = MutableStateFlow<List<Uri>>(emptyList())
    val publicItems: StateFlow<List<Uri>> = _publicItems.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _shutterEffect = MutableStateFlow(false)
    val shutterEffect: StateFlow<Boolean> = _shutterEffect.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        checkSecurityStatus()
        refreshVault()
        refreshPublic()
        checkVaultPinStatus()
    }

    private fun checkVaultPinStatus() {
        _hasVaultPin.value = sharedPrefs.contains("vault_pin")
    }

    fun setVaultPin(pin: String) {
        sharedPrefs.edit().putString("vault_pin", pin).apply()
        _hasVaultPin.value = true
        _isVaultUnlocked.value = true
        _toastMessage.value = "Vault PIN Set Successfully"
    }

    fun verifyVaultPin(pin: String) {
        val savedPin = sharedPrefs.getString("vault_pin", null)
        if (savedPin == pin) {
            _isVaultUnlocked.value = true
            _toastMessage.value = "Vault Unlocked"
        } else {
            _toastMessage.value = "Incorrect PIN"
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun refreshVault() {
        _vaultItems.value = secureStorageManager.listVaultFiles()
    }

    fun refreshPublic() {
        _publicItems.value = secureStorageManager.listPublicFiles()
    }

    fun deleteItem(uri: Uri) {
        viewModelScope.launch {
            try {
                val file = File(uri.path ?: return@launch)
                if (file.exists()) {
                    file.delete()
                    refreshVault()
                    refreshPublic()
                    if (_capturedImageUri.value == uri) {
                        _capturedImageUri.value = null
                    }
                    _toastMessage.value = "Item Deleted Permanently"
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Delete failed", e)
            }
        }
    }

    fun selectMedia(uri: Uri, context: Context) {
        _capturedImageUri.value = uri
        generateProvenanceReport(context, uri)
    }

    suspend fun loadBytes(uri: Uri): ByteArray? {
        return if (uri.path?.contains("/vault/") == true || uri.path?.endsWith(".enc") == true) {
            secureStorageManager.loadEncryptedMedia(uri)
        } else {
            val file = File(uri.path ?: "")
            if (file.exists()) {
                file.readBytes()
            } else {
                null
            }
        }
    }

    private fun checkSecurityStatus() {
        viewModelScope.launch {
            _securityLevel.value = strongBoxKeyManager.getSecurityLevel()
        }
    }

    fun toggleVault(enabled: Boolean) {
        _isVaultEnabled.value = enabled
        _toastMessage.value = if (enabled) "Vault Enabled (Secure Storage)" else "Vault Disabled (Public Gallery)"
    }

    fun toggleProtectedMode(context: Context, enabled: Boolean) {
        if (enabled && !strongBoxKeyManager.isProtectedConfirmationSupported(context)) {
            _toastMessage.value = "Hardware Protected Confirmation not supported on this device"
            return
        }
        _isProtectedMode.value = enabled
        _toastMessage.value = if (enabled) "Shield Enabled (TEE Confirmation)" else "Shield Disabled"
    }

    fun togglePrivacyShield(enabled: Boolean) {
        _isPrivacyShieldEnabled.value = enabled
        _toastMessage.value = if (enabled) "Privacy Shield ON (Metadata Scrubbing)" else "Privacy Shield OFF"
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraManager.startCamera(context, lifecycleOwner, previewView)
    }

    fun capturePhoto(context: Context) {
        _shutterEffect.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            _shutterEffect.value = false
        }
        
        _isProcessing.value = true
        if (_isProtectedMode.value && context is android.app.Activity) {
            captureWithProtection(context)
        } else {
            cameraManager.takePhoto(
                context,
                isPrivacyShieldEnabled = _isPrivacyShieldEnabled.value,
                onImageCaptured = { uri ->
                    processCaptureResult(context, uri)
                },
                onError = { exc ->
                    Log.e("MainViewModel", "Capture error", exc)
                    _isProcessing.value = false
                }
            )
        }
    }

    private fun captureWithProtection(activity: android.app.Activity) {
        val promptText = "Confirm capture of secure provenance media."
        val dataToSign = "Surakshak Protected Capture ${System.currentTimeMillis()}".toByteArray()
        
        strongBoxKeyManager.signWithConfirmation(
            activity,
            dataToSign,
            promptText,
            activity.mainExecutor,
            onSuccess = { signature ->
                // Proceed with normal capture but mark it as confirmed
                cameraManager.takePhoto(
                    activity,
                    isPrivacyShieldEnabled = _isPrivacyShieldEnabled.value,
                    onImageCaptured = { uri ->
                        processCaptureResult(activity, uri)
                    },
                    onError = { 
                        Log.e("MainViewModel", "Capture error", it)
                        _isProcessing.value = false
                    }
                )
            },
            onFailure = { 
                Log.e("MainViewModel", "Confirmation failed: $it")
                _toastMessage.value = "Secure Confirmation Failed: $it"
                _isProcessing.value = false
            }
        )
    }

    private fun processCaptureResult(context: Context, uri: Uri?) {
        if (uri == null) {
            _isProcessing.value = false
            return
        }
        
        viewModelScope.launch {
            try {
                val file = File(uri.path ?: return@launch)
                val bytes = file.readBytes()
                
                if (_isVaultEnabled.value) {
                    val vaultUri = secureStorageManager.saveEncryptedMedia(bytes, "photo_${System.currentTimeMillis()}.enc")
                    _capturedImageUri.value = vaultUri
                    generateProvenanceReport(context, vaultUri)
                    refreshVault()
                    _toastMessage.value = "Photo Secured in Hardware-Encrypted Vault"
                } else {
                    val persistentUri = secureStorageManager.savePersistentPublicMedia(bytes, "photo_${System.currentTimeMillis()}.jpg")
                    _capturedImageUri.value = persistentUri
                    generateProvenanceReport(context, persistentUri)
                    refreshPublic()
                    _toastMessage.value = "Photo Saved Persistently"
                }
                
                // Delete the temporary cache file
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Save failed", e)
                _toastMessage.value = "Save Failed: ${e.localizedMessage}"
                _capturedImageUri.value = uri
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportToGallery(context: Context) {
        val currentUri = _capturedImageUri.value ?: return
        viewModelScope.launch {
            val exportedUri = secureStorageManager.exportToPublicGallery(currentUri, "Surakshak_Export_${System.currentTimeMillis()}.jpg")
            if (exportedUri != null) {
                // If we exported from vault, we might want to keep the capturedImageUri pointing to the new public file
                // so it's visible in public galleries.
                _capturedImageUri.value = exportedUri
                _toastMessage.value = "Successfully Exported to Gallery (Pictures/Surakshak)"
                generateProvenanceReport(context, exportedUri)
            } else {
                _toastMessage.value = "Export Failed"
            }
        }
    }

    fun shareReport(context: Context) {
        val uri = _capturedImageUri.value ?: return
        val report = _provenanceReport.value ?: return
        
        viewModelScope.launch {
            try {
                val bytes = if (uri.path?.contains("/vault/") == true) {
                    secureStorageManager.loadEncryptedMedia(uri)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }

                if (bytes != null) {
                    val pdfFile = PdfReportGenerator.generateReport(context, uri, report, bytes)
                    if (pdfFile != null) {
                        val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            pdfFile
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Provenance Report"))
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Share report failed", e)
            }
        }
    }

    private fun generateProvenanceReport(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = if (uri.path?.contains("/vault/") == true || uri.path?.endsWith(".enc") == true) {
                    secureStorageManager.loadEncryptedMedia(uri)
                } else {
                    // Try direct file read first, then contentResolver
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        file.readBytes()
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                }
                
                if (bytes != null) {
                    _provenanceReport.value = c2paManifestBuilder.verifyProvenance(bytes)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating report", e)
            }
        }
    }

    fun toggleRecording(context: Context) {
        if (_isRecording.value) {
            cameraManager.stopRecording()
            _isRecording.value = false
        } else {
            _isRecording.value = true
            cameraManager.startRecording(
                context,
                onVideoSaved = { uri ->
                    _isRecording.value = false
                    _capturedImageUri.value = uri
                    if (uri != null) {
                        generateProvenanceReport(context, uri)
                    }
                },
                onError = { _isRecording.value = false }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.shutdown()
    }
}
