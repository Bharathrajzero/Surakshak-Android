package com.alphagroup.surakshak.c2pa

interface C2PAManifestBuilder {
    /**
     * Constructs a C2PA manifest and embeds it into the image data.
     * @param imageBytes The raw JPEG image bytes.
     * @param metadata Map of metadata to include in the manifest (e.g., location, timestamp).
     * @return The JPEG image bytes with the embedded C2PA manifest.
     */
    suspend fun embedManifest(imageBytes: ByteArray, metadata: Map<String, Any>): ByteArray

    /**
     * Extracts and verifies provenance information from the media bytes.
     */
    suspend fun verifyProvenance(imageBytes: ByteArray): ProvenanceReport
}
