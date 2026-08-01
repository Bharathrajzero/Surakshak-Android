package com.alphagroup.surakshak.reporting

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.alphagroup.surakshak.c2pa.ProvenanceReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateReport(
        context: Context,
        imageUri: Uri,
        report: ProvenanceReport,
        imageBytes: ByteArray
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Title
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Surakshak Media Provenance Report", 50f, 50f, paint)

        // Subtitle
        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $timestamp", 50f, 75f, paint)

        // Image Preview
        paint.color = Color.BLACK
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (bitmap != null) {
            val scaledBitmap = scaleBitmap(bitmap, 400, 300)
            canvas.drawBitmap(scaledBitmap, 50f, 100f, paint)
        }

        // Provenance Data
        var yPos = 450f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Verification Details", 50f, yPos, paint)
        yPos += 30f

        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Verification Status: ${report.status}", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Signing Entity: ${report.signingEntity}", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Security Level: ${report.securityLevel}", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Hash Algorithm: ${report.hashAlgorithm}", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Location: ${report.metadata["latitude"]}, ${report.metadata["longitude"]}", 50f, yPos, paint)
        yPos += 40f

        // Attestation Chain
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Hardware Attestation Chain", 50f, yPos, paint)
        yPos += 30f

        paint.textSize = 8f
        paint.isFakeBoldText = false
        report.certificateChain.forEachIndexed { index, cert ->
            canvas.drawText("Layer ${index + 1}: ${cert.subject.take(80)}", 50f, yPos, paint)
            yPos += 15f
            canvas.drawText("Issuer: ${cert.issuer.take(80)}", 70f, yPos, paint)
            yPos += 15f
            canvas.drawText("Serial: ${cert.serialNumber}", 70f, yPos, paint)
            yPos += 20f
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "Surakshak_Report_${System.currentTimeMillis()}.pdf")
        return try {
            document.writeTo(FileOutputStream(file))
            document.close()
            file
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    private fun scaleBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = source.width.toFloat() / source.height.toFloat()
        var width = maxWidth
        var height = (maxWidth / ratio).toInt()

        if (height > maxHeight) {
            height = maxHeight
            width = (maxHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}
