package com.baconnish.gobuddy.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import com.baconnish.gobuddy.domain.ScanResult
import com.baconnish.gobuddy.domain.ScreenshotParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class ScanCapture(val result: ScanResult, val lines: List<String>)

class ScreenshotScanner(
    private val context: Context,
    private val speciesRepository: SpeciesRepository,
) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun scan(uri: Uri): ScanCapture {
        val bitmap = context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it) }
            ?: throw IOException("Couldn't open image")
        return scan(bitmap)
    }

    suspend fun scan(bitmap: Bitmap): ScanCapture {
        val ocrLines = recognize(InputImage.fromBitmap(bitmap, 0))
        val lines = ocrLines.map { it.text }
        var result = ScreenshotParser.parse(lines, speciesRepository.all.map { it.name })
        AppraisalReader.read(bitmap, ocrLines)?.let { ivs ->
            result = result.copy(
                ivAtk = ivs.attack,
                ivDef = ivs.defense,
                ivSta = ivs.stamina,
                candy = null,
                candyXl = null,
                stardust = null,
            )
        }
        try {
            File(context.getExternalFilesDir(null), "last_scan.txt").writeText(
                lines.joinToString("\n") + "\n---\n" + result,
            )
        } catch (_: Exception) {
        }
        return ScanCapture(result, lines)
    }

    private suspend fun recognize(image: InputImage): List<OcrLine> =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    continuation.resume(
                        text.textBlocks.flatMap { block ->
                            block.lines.mapNotNull { line ->
                                line.boundingBox?.let { OcrLine(line.text, it) }
                            }
                        },
                    )
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
}
