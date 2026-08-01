package com.alphagroup.surakshak.storage

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    fun saveEncryptedMedia(bytes: ByteArray, fileName: String): Uri {
        val file = File(context.filesDir, "vault/$fileName")
        file.parentFile?.mkdirs()

        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(bytes)
        }

        return Uri.fromFile(file)
    }

    fun savePersistentPublicMedia(bytes: ByteArray, fileName: String): Uri {
        val file = File(context.filesDir, "persistent_captures/$fileName")
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        return Uri.fromFile(file)
    }

    fun loadEncryptedMedia(uri: Uri): ByteArray? {
        val path = uri.path ?: return null
        val file = File(path)
        if (!file.exists()) return null

        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return try {
            encryptedFile.openFileInput().use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun listPublicFiles(): List<Uri> {
        val publicDir = File(context.filesDir, "persistent_captures")
        if (!publicDir.exists()) return emptyList()
        
        // Filter for valid image files and sort by date (from filename timestamp)
        return publicDir.listFiles { _, name -> name.endsWith(".jpg") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { Uri.fromFile(it) } ?: emptyList()
    }

    fun listVaultFiles(): List<Uri> {
        val vaultDir = File(context.filesDir, "vault")
        if (!vaultDir.exists()) return emptyList()
        
        return vaultDir.listFiles { _, name -> name.endsWith(".enc") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { Uri.fromFile(it) } ?: emptyList()
    }

    fun exportToPublicGallery(uri: Uri, publicFileName: String): Uri? {
        val bytes = if (uri.path?.contains("/vault/") == true) {
            loadEncryptedMedia(uri)
        } else {
            File(uri.path ?: return null).readBytes()
        } ?: return null
        
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, publicFileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Surakshak")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageUri = resolver.insert(collection, values) ?: return null

        try {
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                outputStream.write(bytes)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.clear()
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, values, null, null)
            }

            // Force a scan of the file to make it visible
            // For older devices, we might need a physical path. 
            // For Scoped Storage (Q+), the IS_PENDING = 0 usually handles it, but scan won't hurt.
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).absolutePath + "/Surakshak/" + publicFileName),
                arrayOf("image/jpeg")
            ) { _, _ -> }

            return imageUri
        } catch (e: Exception) {
            resolver.delete(imageUri, null, null)
            return null
        }
    }
}
