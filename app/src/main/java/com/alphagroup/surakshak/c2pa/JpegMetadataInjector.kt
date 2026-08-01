package com.alphagroup.surakshak.c2pa

import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A utility to inject JUMBF metadata into JPEG files.
 * This is a simplified implementation for demonstration of the provenance standard.
 */
object JpegMetadataInjector {
    private const val TAG = "JpegMetadataInjector"
    
    // C2PA / JUMBF Markers
    private const val APP1_MARKER = 0xE1
    private const val JUMBF_SIGNATURE = "jumb"

    fun injectJumbf(jpegBytes: ByteArray, jumbfPayload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        
        if (jpegBytes.size < 2 || jpegBytes[0].toInt() != 0xFF || jpegBytes[1].toInt() != 0xD8) {
            Log.e(TAG, "Invalid JPEG SOI")
            return jpegBytes
        }

        // SOI (FF D8)
        output.write(0xFF)
        output.write(0xD8)

        // Inject JUMBF APP1 Segment
        // [FF E1] [Length(2)] [jumb] [Payload]
        val segmentLength = 2 + 4 + jumbfPayload.size // Length field + signature + payload
        
        output.write(0xFF)
        output.write(APP1_MARKER)
        
        val lengthBuffer = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
        lengthBuffer.putShort(segmentLength.toShort())
        output.write(lengthBuffer.array())
        
        output.write(JUMBF_SIGNATURE.toByteArray())
        output.write(jumbfPayload)

        // Copy original segments (skipping SOI)
        output.write(jpegBytes, 2, jpegBytes.size - 2)
        
        return output.toByteArray()
    }
    
    /**
     * Extracts JUMBF payload from a JPEG file.
     */
    fun extractJumbf(jpegBytes: ByteArray): ByteArray? {
        var pos = 2
        while (pos < jpegBytes.size - 4) {
            if (jpegBytes[pos].toInt() and 0xFF != 0xFF) break
            
            val marker = jpegBytes[pos + 1].toInt() and 0xFF
            val length = ((jpegBytes[pos + 2].toInt() and 0xFF) shl 8) or (jpegBytes[pos + 3].toInt() and 0xFF)
            
            if (marker == APP1_MARKER) {
                val signature = String(jpegBytes.sliceArray(pos + 4 until pos + 8))
                if (signature == JUMBF_SIGNATURE) {
                    return jpegBytes.sliceArray(pos + 8 until pos + 2 + length)
                }
            }
            
            pos += 2 + length
            if (marker == 0xDA) break // Start of Scan, stop searching
        }
        return null
    }
}
