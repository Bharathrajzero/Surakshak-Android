# Surakshak: TrueProvenance Cam

**Surakshak** (meaning "Protector") is a high-integrity, open-source Android application designed for verifiable media capture. It leverages hardware-backed cryptography to attach signed **C2PA (Coalition for Content Provenance and Authenticity)** manifests at the exact moment of sensor capture.

By utilizing Android's **StrongBox TEE (Trusted Execution Environment)**, **Key Attestation**, and **Protected Confirmation**, Surakshak provides military-grade assurance that media is immutable and provably authentic.

---
## Screenshot
<!-- Screenshots Grid (3 images per row) -->
<table>
  <tr>
    <td width="33%"><img src="https://github.com/user-attachments/assets/60977bb2-84a0-406e-8b40-b5ad91646668" alt="Landing page" /></td>
    <td width="33%"><img src="https://github.com/user-attachments/assets/a0875642-2018-46bd-95de-7dcaba0db173" alt="Camera" /></td>
    <td width="33%"><img src="https://github.com/user-attachments/assets/f641a5e2-ad90-4f88-98ca-29fe23521e26" alt="Vault Pin" /></td>
  </tr>
  <tr>
    <td width="33%"><img src="https://github.com/user-attachments/assets/6892c842-3e6c-47ea-bf25-e5ca616701f8" alt="Secure Vault" /></td>
    <td width="33%"><img src="https://github.com/user-attachments/assets/508996da-94e1-41c1-afb0-d77b8f3d4671" alt="Persistent" /></td>
    <td width="33%"><img src="https://github.com/user-attachments/assets/e040cd8c-b22b-43d5-831c-47944f0c9f38" alt="Provenance Inspector" /></td>
  </tr>
</table>

<br />

<!-- Security Audit Report (Below Images) -->
<div align="center">
  <h3>📄 Security Audit Report</h3>
  <p>
    <a href="https://github.com/user-attachments/files/30617069/Surakshak_Report_1785588280384.pdf" target="_blank">
      <img src="https://img.shields.io/badge/Download_Report-PDF-red?style=for-the-badge&logo=adobeacrobatreader" alt="Download Report" />
    </a>
  </p>
  <p>
    <a href="https://github.com/user-attachments/files/30617069/Surakshak_Report_1785588280384.pdf" target="_blank">
      <b>Surakshak_Report_1785588280384.pdf</b>
    </a>
  </p>
</div>

---

## 🛡️ Core Features

### 1. Hardware-Backed Provenance
- **StrongBox Key Management**: Generates non-exportable EC P-256 keys inside dedicated hardware security modules (Google Titan / Samsung Knox).
- **Key Attestation**: Automatically extracts hardware-signed X.509 certificate chains to prove key residency in genuine hardware.
- **Hardware Status Badge**: Real-time dashboard showing device security integrity (StrongBox, TEE, or Software).

### 2. Advanced Security & Privacy
- **Hardware-Encrypted Vault**: Uses **Jetpack Security (AES-256 GCM)** to store media in a secure vault. Media is unreadable by other apps until explicitly exported.
- **Android Protected Confirmation (APC)**: Optional high-assurance mode where the device hardware renders a system-level prompt for the user to physically confirm high-stakes captures.
- **Biometric App Lock**: Mandatory authentication (Fingerprint, Face, or PIN) required every time the app is opened, ensuring the vault remains private.
- **Privacy Shield (Metadata Scrubbing)**: Optional mode to strip sensitive EXIF headers (device ID, technical specs) from the media while preserving the immutable hardware-backed signature.

### 3. C2PA & Media Pipeline
- **Structural JUMBF Injection**: Implements a custom JPEG segment injector that embeds JUMBF-compliant metadata boxes directly into file headers.
- **Dynamic Provenance Inspector**: A built-in forensic tool to verify signatures, inspect the multi-layer attestation tree, and validate manifest integrity.
- **Hybrid CameraX Pipeline**: High-performance viewfinder with support for **Secure Photos** (Tap) and **Secure Video** (Long Press).

---

## 💡 Real-World Use Cases

Surakshak is designed for scenarios where the authenticity of media is critical:

*   **Investigative Journalism**: Journalists can capture photos in conflict zones or sensitive areas, providing proof that the images were not AI-generated or edited after capture.
*   **Insurance Claims**: Homeowners or drivers can document damage with a tamper-proof hardware signature and GPS location, making claims harder to dispute.
*   **Citizen Evidence**: Activists or witnesses can capture high-integrity evidence of events, with the hardware attestation chain providing legal-grade proof of the media's origin.
*   **Scientific Research**: Researchers can document field findings with immutable timestamps and location data cryptographically bound to the sensor data.

---

## 🛠️ Technology Stack

- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture + Hilt DI
- **Camera**: Jetpack CameraX (ImageCapture & VideoCapture)
- **Security**: Android Keystore (StrongBox) + `androidx.security` (EncryptedFile) + APC
- **Metadata**: Custom Kotlin JUMBF/C2PA Engine + C++ JNI Bridge
- **Image Loading**: Coil
- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 37 (Android 15)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer.
- A physical device with **StrongBox** and **Biometrics** support is highly recommended for full feature verification.

### Build & Run
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle (requires internet for dependency resolution).
4. Run the `app` module on your device.

### Usage
- **Normal Capture**: Tap the shutter button.
- **Video Recording**: Long-press the shutter button.
- **Secure Vault**: Toggle the "Lock" icon to encrypt media in the vault.
- **High-Assurance**: Toggle the "Shield" icon to enable Android Protected Confirmation.
- **Privacy Shield**: Toggle the "Eye" icon to scrub sensitive metadata from the captured file.

---

## 📜 License
This project is licensed under the MIT License © 2026 Bharath Raj, AlphaGroup.

---

Developed with ❤️ by the Bharath raj.
