package com.baconnish.gobuddy.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.core.graphics.get
import kotlin.math.abs
import kotlin.math.roundToInt

data class OcrLine(val text: String, val box: Rect)

object AppraisalReader {

    data class Ivs(val attack: Int, val defense: Int, val stamina: Int)

    fun read(bitmap: Bitmap, lines: List<OcrLine>): Ivs? {
        val attack = findLabel(lines, "Attack") ?: return null
        val defense = findLabel(lines, "Defense") ?: return null
        val tolerance = attack.box.height() * 2
        val hp = lines.firstOrNull {
            it.text.trim().equals("HP", ignoreCase = true) &&
                abs(it.box.left - attack.box.left) <= tolerance &&
                it.box.top > defense.box.top
        } ?: return null
        if (defense.box.top <= attack.box.top) return null
        if (abs(defense.box.left - attack.box.left) > tolerance) return null

        val a = readBar(bitmap, attack.box) ?: return null
        val d = readBar(bitmap, defense.box) ?: return null
        val s = readBar(bitmap, hp.box) ?: return null
        return Ivs(a, d, s)
    }

    private fun findLabel(lines: List<OcrLine>, text: String): OcrLine? =
        lines.firstOrNull { it.text.trim().equals(text, ignoreCase = true) }

    private fun readBar(bitmap: Bitmap, label: Rect): Int? {
        val height = label.height().coerceAtLeast(8)
        val startX = (label.left - height).coerceIn(0, bitmap.width - 1)
        val maxX = (label.left + height * 14).coerceAtMost(bitmap.width - 1)

        val fractions = mutableListOf<Double>()
        for (rowFactor in listOf(0.5, 0.8, 1.1, 1.4)) {
            val y = label.bottom + (height * rowFactor).roundToInt()
            if (y !in 0 until bitmap.height) continue

            var firstBar = -1
            var lastFill = -1
            var lastBar = -1
            var gapRun = 0
            for (x in startX..maxX) {
                val pixel = bitmap[x, y]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                when {
                    isFill(r, g, b) -> {
                        if (firstBar < 0) firstBar = x
                        lastFill = x
                        lastBar = x
                        gapRun = 0
                    }
                    isTrack(r, g, b) -> {
                        if (firstBar < 0) firstBar = x
                        lastBar = x
                        gapRun = 0
                    }
                    firstBar >= 0 -> {
                        gapRun++
                        if (gapRun > height) break
                    }
                }
            }

            if (firstBar >= 0 && lastBar - firstBar > height * 4) {
                fractions.add(
                    if (lastFill < firstBar) {
                        0.0
                    } else {
                        (lastFill - firstBar + 1).toDouble() / (lastBar - firstBar + 1)
                    },
                )
                if (fractions.size == 3) break
            }
        }

        if (fractions.isEmpty()) return null
        val median = fractions.sorted()[fractions.size / 2]
        return (median * 15).roundToInt().coerceIn(0, 15)
    }

    private fun isFill(r: Int, g: Int, b: Int): Boolean =
        r >= 200 && b <= 165 && (r - b) >= 60

    private fun isTrack(r: Int, g: Int, b: Int): Boolean =
        abs(r - g) <= 20 && abs(g - b) <= 20 && r in 185..244
}
