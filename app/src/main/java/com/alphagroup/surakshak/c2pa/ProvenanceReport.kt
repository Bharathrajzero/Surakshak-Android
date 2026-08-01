package com.alphagroup.surakshak.c2pa

import com.alphagroup.surakshak.security.IntegrityManager
import com.alphagroup.surakshak.security.StrongBoxKeyManager
import java.security.cert.X509Certificate

data class ProvenanceReport(
    val status: VerificationStatus,
    val securityLevel: StrongBoxKeyManager.SecurityLevel,
    val deviceIntegrity: IntegrityManager.IntegrityStatus = IntegrityManager.IntegrityStatus.UNKNOWN,
    val signingEntity: String,
    val hashAlgorithm: String = "SHA-256",
    val certificateChain: List<CertificateInfo>,
    val metadata: Map<String, String>
)

enum class VerificationStatus {
    VERIFIED,
    TAMPERED,
    NOT_SIGNED
}

data class CertificateInfo(
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val isHardwareBacked: Boolean
)

fun X509Certificate.toInfo(isHardware: Boolean): CertificateInfo {
    return CertificateInfo(
        subject = subjectDN.name,
        issuer = issuerDN.name,
        serialNumber = serialNumber.toString(16).uppercase(),
        isHardwareBacked = isHardware
    )
}
