package com.alphagroup.surakshak.ui.camera

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.alphagroup.surakshak.R
import com.alphagroup.surakshak.security.StrongBoxKeyManager
import com.alphagroup.surakshak.ui.MainViewModel

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    onNavigateToInspector: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val securityLevel by viewModel.securityLevel.collectAsState()
    val capturedImageUri by viewModel.capturedImageUri.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isVaultEnabled by viewModel.isVaultEnabled.collectAsState()
    val isProtectedMode by viewModel.isProtectedMode.collectAsState()
    val isPrivacyShieldEnabled by viewModel.isPrivacyShieldEnabled.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val shutterEffect by viewModel.shutterEffect.collectAsState()

    // Handle Toasts
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        key(securityLevel) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        viewModel.startCamera(ctx, lifecycleOwner, this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            SecurityStatusBadge(securityLevel = securityLevel)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Toggles
            Row {
                IconButton(
                    onClick = { viewModel.toggleVault(!isVaultEnabled) },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isVaultEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Vault",
                        tint = if (isVaultEnabled) Color.Yellow else Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { viewModel.toggleProtectedMode(context, !isProtectedMode) },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    val apcSupported = remember { viewModel.strongBoxKeyManager.isProtectedConfirmationSupported(context) }
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Protected",
                        tint = if (isProtectedMode) Color.Cyan else if (apcSupported) Color.White else Color.Gray.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.togglePrivacyShield(!isPrivacyShieldEnabled) },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPrivacyShieldEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Privacy Shield",
                        tint = if (isPrivacyShieldEnabled) Color.Green else Color.White
                    )
                }
            }
        }

        // Gallery Button
        IconButton(
            onClick = onNavigateToGallery,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Folder, contentDescription = "Gallery", tint = Color.White)
        }

        // Branding Logo
        Icon(
            painter = painterResource(id = R.drawable.logo_full),
            contentDescription = "Logo",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp),
            tint = Color.Unspecified
        )

        // Thumbnail of last captured image
        capturedImageUri?.let { uri ->
            var thumbnailBytes by remember { mutableStateOf<ByteArray?>(null) }
            LaunchedEffect(uri) {
                thumbnailBytes = viewModel.loadBytes(uri)
            }

            Surface(
                onClick = onNavigateToInspector,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(64.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.3f)
            ) {
                AsyncImage(
                    model = thumbnailBytes ?: uri,
                    contentDescription = "Last captured",
                    contentScale = ContentScale.Crop
                )
            }
        }

        ShutterButton(
            onClick = { viewModel.capturePhoto(context) },
            onLongClick = { viewModel.toggleRecording(context) },
            isRecording = isRecording,
            isProcessing = isProcessing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        // Shutter Flash Effect
        if (shutterEffect) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        }
    }
}

@Composable
fun SecurityStatusBadge(
    securityLevel: StrongBoxKeyManager.SecurityLevel,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (securityLevel) {
        StrongBoxKeyManager.SecurityLevel.STRONGBOX -> Color.Green to "StrongBox (Hardware HSM)"
        StrongBoxKeyManager.SecurityLevel.TEE -> Color.Green to "Hardware Protected (TEE)"
        StrongBoxKeyManager.SecurityLevel.SOFTWARE -> Color.Red to "Software Only (Insecure)"
        else -> Color.Gray to "Hardware Status Unknown"
    }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.6f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Security",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isRecording: Boolean,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .background(Color.White.copy(alpha = 0.3f), CircleShape)
            .padding(4.dp)
            .background(if (isRecording) Color.Red else Color.White, CircleShape)
            .combinedClickable(
                enabled = !isProcessing,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = if (isRecording) Color.White else Color.Black,
                strokeWidth = 3.dp
            )
        } else {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Camera,
                contentDescription = "Shutter",
                modifier = Modifier.size(40.dp),
                tint = if (isRecording) Color.White else Color.Black
            )
        }
    }
}
