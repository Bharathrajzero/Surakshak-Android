package com.alphagroup.surakshak

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alphagroup.surakshak.security.BiometricLockManager
import com.alphagroup.surakshak.storage.SecureStorageManager
import com.alphagroup.surakshak.ui.MainViewModel
import com.alphagroup.surakshak.ui.camera.CameraScreen
import com.alphagroup.surakshak.ui.gallery.GalleryScreen
import com.alphagroup.surakshak.ui.inspector.MediaInspectorScreen
import com.alphagroup.surakshak.ui.theme.SurakshakTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var secureStorageManager: SecureStorageManager
    @Inject lateinit var biometricLockManager: BiometricLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SurakshakTheme {
                var isAppAuthenticated by remember { mutableStateOf(false) }

                if (!isAppAuthenticated) {
                    LockScreen(
                        biometricLockManager = biometricLockManager,
                        onAuthenticated = { isAppAuthenticated = true }
                    )
                } else {
                    PermissionProvider {
                        MainApp(secureStorageManager)
                    }
                }
            }
        }
    }
}

@Composable
fun LockScreen(
    biometricLockManager: BiometricLockManager,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        if (activity != null && biometricLockManager.canAuthenticate(activity)) {
            biometricLockManager.authenticate(
                activity = activity,
                onSuccess = onAuthenticated,
                onError = { /* Handle error if needed */ }
            )
        } else {
            // No biometrics or PIN set up, just let them through or show a warning
            onAuthenticated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Surakshak is Locked", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (activity != null) {
                    biometricLockManager.authenticate(activity, onAuthenticated, {})
                }
            }) {
                Text("Unlock App")
            }
        }
    }
}

@Composable
fun PermissionProvider(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
            // We can track location permission separately or just proceed
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    if (hasCameraPermission) {
        content()
    } else {
        Scaffold { padding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("Camera permission is required to use Surakshak")
            }
        }
    }
}

@Composable
fun MainApp(secureStorageManager: SecureStorageManager) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "camera",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("camera") {
                CameraScreen(
                    viewModel = viewModel,
                    onNavigateToInspector = { navController.navigate("inspector") },
                    onNavigateToGallery = { navController.navigate("gallery") }
                )
            }
            composable("gallery") {
                GalleryScreen(
                    viewModel = viewModel,
                    secureStorageManager = secureStorageManager,
                    onNavigateToInspector = { navController.navigate("inspector") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("inspector") {
                MediaInspectorScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
