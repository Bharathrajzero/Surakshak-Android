package com.alphagroup.surakshak.ui.gallery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alphagroup.surakshak.storage.SecureStorageManager
import com.alphagroup.surakshak.ui.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    secureStorageManager: SecureStorageManager,
    onNavigateToInspector: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vaultItems by viewModel.vaultItems.collectAsState()
    val publicItems by viewModel.publicItems.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val hasVaultPin by viewModel.hasVaultPin.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.refreshVault()
        viewModel.refreshPublic()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Gallery") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (selectedTab == 0 && isVaultUnlocked) {
                            IconButton(onClick = { viewModel.lockVault() }) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock Vault")
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Secure Vault") },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Persistent") },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (selectedTab == 0 && !isVaultUnlocked) {
            VaultLockScreen(viewModel = viewModel, hasPin = hasVaultPin)
        } else {
            val items = if (selectedTab == 0) vaultItems else publicItems
            
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTab == 0) "No secure media found" else "No persistent media found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items) { uri ->
                        MediaThumbnail(
                            uri = uri,
                            isSecure = selectedTab == 0,
                            secureStorageManager = secureStorageManager,
                            onClick = {
                                viewModel.selectMedia(uri, context)
                                onNavigateToInspector()
                            },
                            onDelete = {
                                viewModel.deleteItem(uri)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaThumbnail(
    uri: Uri,
    isSecure: Boolean,
    secureStorageManager: SecureStorageManager,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var imageData by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(uri) {
        if (isSecure) {
            imageData = secureStorageManager.loadEncryptedMedia(uri)
        } else {
            // For persistent public media, we can pass the Uri directly to AsyncImage
            // But we already have a readBytes mechanism for simplicity here
            val file = File(uri.path ?: "")
            if (file.exists()) {
                imageData = file.readBytes()
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color.DarkGray)
            .clickable(onClick = onClick)
    ) {
        imageData?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
            }
            if (isSecure) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
