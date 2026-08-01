# Implementation Plan - Vault Lockdown & Multi-Layer Security

Implement a dedicated 4-digit PIN for the Secure Vault, remove the export option for vaulted items, and fix the "disappearing media" issue for persistent photos.

## User Review Required

> [!IMPORTANT]
> **Vault PIN**: This is a secondary 4-digit PIN specifically for the vault. You will be asked to set it the first time you try to access the vault.
> **Export Restriction**: As requested, the "Download" (Export to Gallery) button will be **removed** for all vaulted items. Vaulted items can only be shared as signed PDF reports.
> **Persistent Media**: I will fix the bug where "Persistent" captures (non-vaulted) were not correctly saved/loaded between app restarts.

## Proposed Changes

### 1. Vault PIN Security
#### [MODIFY] [MainViewModel.kt](file:///C:/Users/Bharath/AndroidStudioProjects/Surakshak/app/src/main/java/com/alphagroup/surakshak/ui/MainViewModel.kt)
- Add state to track if the vault is currently "unlocked" with the 4-digit PIN.
- Add `setVaultPin(pin: String)` and `verifyVaultPin(pin: String)` methods.
- Store the PIN hash securely using `EncryptedSharedPreferences`.

#### [NEW] [VaultLockScreen.kt](file:///C:/Users/Bharath/AndroidStudioProjects/Surakshak/app/src/main/java/com/alphagroup/surakshak/ui/gallery/VaultLockScreen.kt)
- A specialized UI for setting/entering the 4-digit vault PIN.
- Prevents any vault thumbnails or content from rendering until verified.

### 2. Export Policy
#### [MODIFY] [MediaInspectorScreen.kt](file:///C:/Users/Bharath/AndroidStudioProjects/Surakshak/app/src/main/java/com/alphagroup/surakshak/ui/inspector/MediaInspectorScreen.kt)
- Remove the `IconButton` for "Export" (Download) when the current media is from the vault.
- Keep only the "Share Report" (PDF) button for vaulted media.

### 3. Persistent Storage Fix
#### [MODIFY] [SecureStorageManager.kt](file:///C:/Users/Bharath/AndroidStudioProjects/Surakshak/app/src/main/java/com/alphagroup/surakshak/storage/SecureStorageManager.kt)
- Ensure the `persistent_captures` directory is correctly indexed by the `listPublicFiles()` method.
- Fix path resolution for `AsyncImage` to ensure photos don't disappear after closing the app.

### 4. UI Refinement
#### [MODIFY] [GalleryScreen.kt](file:///C:/Users/Bharath/AndroidStudioProjects/Surakshak/app/src/main/java/com/alphagroup/surakshak/ui/gallery/GalleryScreen.kt)
- Integrate the `VaultLockScreen` as a modal overlay when the "Secure Vault" tab is selected.

## Verification Plan

### Manual Verification
- **Vault PIN**: Tap the "Secure Vault" tab. Verify a 4-digit PIN prompt appears.
- **PIN Setup**: Verify that on first-time use, you are prompted to set a new PIN.
- **Export Policy**: Open a vaulted photo in the Inspector. Verify the "Download" button is missing.
- **Persistence**: Take a normal photo, restart the app, and verify it still exists in the "Persistent" gallery tab.
- **Smoothness**: Verify that deleting an item (both types) provides immediate UI feedback and refreshes the grid.
