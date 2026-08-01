package com.alphagroup.surakshak.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.alphagroup.surakshak.ui.MainViewModel

@Composable
fun VaultLockScreen(
    viewModel: MainViewModel,
    hasPin: Boolean
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val isSettingUp = !hasPin

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Yellow
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (isSettingUp) "Set Vault PIN" else "Vault Locked",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                text = if (isSettingUp) "Create a 4-digit PIN to secure your vault" else "Enter your 4-digit PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.width(200.dp)
            )

            if (isSettingUp) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.width(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isSettingUp) {
                        if (pin.length == 4 && pin == confirmPin) {
                            viewModel.setVaultPin(pin)
                        }
                    } else {
                        if (pin.length == 4) {
                            viewModel.verifyVaultPin(pin)
                        }
                    }
                },
                enabled = if (isSettingUp) (pin.length == 4 && pin == confirmPin) else pin.length == 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSettingUp) "Save and Unlock" else "Unlock Vault")
            }
        }
    }
}
