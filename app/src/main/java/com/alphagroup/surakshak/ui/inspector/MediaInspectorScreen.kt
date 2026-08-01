package com.alphagroup.surakshak.ui.inspector

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alphagroup.surakshak.R
import com.alphagroup.surakshak.c2pa.ProvenanceReport
import com.alphagroup.surakshak.c2pa.VerificationStatus
import com.alphagroup.surakshak.security.StrongBoxKeyManager
import com.alphagroup.surakshak.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInspectorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val capturedImageUri by viewModel.capturedImageUri.collectAsState()
    val provenanceReport by viewModel.provenanceReport.collectAsState()
    val isVaultEnabled by viewModel.isVaultEnabled.collectAsState()

    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(capturedImageUri) {
        capturedImageUri?.let { uri ->
            imageBytes = viewModel.loadBytes(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provenance Inspector") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareReport(context) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Share Report")
                    }
                    val isVaultItem = capturedImageUri?.path?.contains("/vault/") == true || capturedImageUri?.path?.endsWith(".enc") == true
                    if (!isVaultItem) {
                        IconButton(onClick = { viewModel.exportToGallery(context) }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            capturedImageUri?.let { uri ->
                AsyncImage(
                    model = imageBytes ?: uri,
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )

                provenanceReport?.let { report ->
                    ProvenanceDetailsSection(report)
                    CertificateChainSection(report)
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No image captured yet")
            }
        }
    }
}

@Composable
fun ProvenanceDetailsSection(report: ProvenanceReport) {
    val (icon, color, text) = when (report.status) {
        VerificationStatus.VERIFIED -> Triple(Icons.Default.Verified, Color.Green, "Verified")
        VerificationStatus.TAMPERED -> Triple(Icons.Default.Warning, Color.Red, "Tampered")
        VerificationStatus.NOT_SIGNED -> Triple(Icons.Default.Info, Color.Gray, "Unsigned")
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = text, tint = color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("C2PA Content $text", style = MaterialTheme.typography.titleMedium)
                }

                Icon(
                    painter = painterResource(id = R.drawable.logo_full),
                    contentDescription = "Logo",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            DetailItem(label = "Signing Entity", value = report.signingEntity)
            DetailItem(label = "Action", value = report.metadata["action"] ?: "N/A")
            
            val securityLabel = when (report.securityLevel) {
                StrongBoxKeyManager.SecurityLevel.STRONGBOX -> "StrongBox (Google Titan/Samsung Knox)"
                StrongBoxKeyManager.SecurityLevel.TEE -> "Standard TEE"
                else -> "Software-Backed (Insecure)"
            }
            DetailItem(label = "Security Level", value = securityLabel)
            DetailItem(label = "Device Integrity", value = report.deviceIntegrity.name)
            DetailItem(label = "Hash Algorithm", value = report.hashAlgorithm)
            
            val lat = report.metadata["latitude"]
            val lon = report.metadata["longitude"]
            if (lat != null && lon != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Location: $lat, $lon", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun CertificateChainSection(report: ProvenanceReport) {
    Text(
        text = "Certificate Attestation Chain",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    report.certificateChain.forEachIndexed { index, cert ->
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Layer ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (cert.isHardwareBacked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Hardware",
                            modifier = Modifier.size(12.dp),
                            tint = Color.Green
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Subject: ${cert.subject}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Issuer: ${cert.issuer}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Serial: ${cert.serialNumber}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
