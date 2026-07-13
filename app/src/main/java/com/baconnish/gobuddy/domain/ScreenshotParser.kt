package com.baconnish.gobuddy.domain

import kotlin.math.abs

data class ScanResult(
    val cp: Int? = null,
    val hpMax: Int? = null,
    val candy: Int? = null,
    val candyXl: Int? = null,
    val stardust: Int? = null,
    val speciesName: String? = null,
    val candyFamily: String? = null,
    val ivAtk: Int? = null,
    val ivDef: Int? = null,
    val ivSta: Int? = null,
) {
    val isEmpty: Boolean
        get() = cp == null && hpMax == null && candy == null && candyXl == null &&
            speciesName == null && candyFamily == null && ivAtk == null
}

object ScreenshotParser {

    private val CP_REGEX = Regex("""(?:C\s*P|©)\s*([0-9][0-9 ,.]{1,6})""", RegexOption.IGNORE_CASE)
    private val CP_ALONE = Regex("""^(?:C\s*P|©)$""", RegexOption.IGNORE_CASE)
    private val HP_SUFFIX_REGEX = Regex("""([0-9]{1,4})\s*/\s*([0-9]{1,4})\s*HP""", RegexOption.IGNORE_CASE)
    private val HP_PREFIX_REGEX = Regex("""HP\s*([0-9]{1,4})\s*/\s*([0-9]{1,4})""", RegexOption.IGNORE_CASE)
    private val CANDY_XL_LABEL = Regex("""(.+?)\s+CANDY\s+XL\b""", RegexOption.IGNORE_CASE)
    private val CANDY_LABEL = Regex("""(.+?)\s+CANDY\b""", RegexOption.IGNORE_CASE)
    private val POWER_UP_LABEL = Regex("""\bPOWER\s*UP\b""", RegexOption.IGNORE_CASE)
    private val NUMBER_LINE = Regex("""^[x×]?\s*([0-9][0-9,. ]{0,7})$""")

    fun parse(rawLines: List<String>, speciesNames: Collection<String>): ScanResult {
        val lines = rawLines.map { it.trim() }.filter { it.isNotEmpty() }

        var cp: Int? = null
        for (line in lines) {
            if (HP_SUFFIX_REGEX.containsMatchIn(line)) continue
            val match = CP_REGEX.find(line) ?: continue
            val value = match.groupValues[1].filter { it.isDigit() }.toIntOrNull()
            if (value != null && value >= 10) {
                cp = value
                break
            }
        }
        if (cp == null) {
            lines.forEachIndexed { index, line ->
                if (cp == null && CP_ALONE.matches(line.trim())) {
                    for (offset in listOf(1, -1)) {
                        val neighbor = lines.getOrNull(index + offset)?.trim() ?: continue
                        val value = NUMBER_LINE.find(neighbor)
                            ?.groupValues?.get(1)?.filter { it.isDigit() }?.toIntOrNull()
                        if (value != null && value in 10..9999) {
                            cp = value
                            break
                        }
                    }
                }
            }
        }

        var hpMax: Int? = null
        for (line in lines) {
            val match = HP_SUFFIX_REGEX.find(line) ?: HP_PREFIX_REGEX.find(line) ?: continue
            hpMax = match.groupValues[2].toIntOrNull()
            if (hpMax != null) break
        }

        var candyFamily: String? = null
        var candy: Int? = null
        var candyXl: Int? = null
        var stardust: Int? = null

        lines.forEachIndexed { index, line ->
            val xlMatch = CANDY_XL_LABEL.find(line)
            if (xlMatch != null) {
                val family = cleanFamily(xlMatch.groupValues[1])
                if (isKnownFamily(family, speciesNames)) {
                    if (candyFamily == null) candyFamily = family
                    if (candyXl == null) candyXl = numberNear(lines, index)
                }
                return@forEachIndexed
            }
            val candyMatch = CANDY_LABEL.find(line)
            if (candyMatch != null) {
                val family = cleanFamily(candyMatch.groupValues[1])
                if (isKnownFamily(family, speciesNames)) {
                    if (candyFamily == null) candyFamily = family
                    if (candy == null) candy = numberNear(lines, index)
                }
                return@forEachIndexed
            }
            if (POWER_UP_LABEL.containsMatchIn(line) && stardust == null) {
                stardust = powerUpDustCost(lines, index)
            }
        }

        val species = findSpecies(lines, speciesNames)

        return ScanResult(
            cp = cp,
            hpMax = hpMax,
            candy = candy,
            candyXl = candyXl,
            stardust = stardust,
            speciesName = species,
            candyFamily = candyFamily,
        )
    }

    private fun isKnownFamily(family: String, speciesNames: Collection<String>): Boolean =
        family.length >= 3 && speciesNames.any { it.equals(family, ignoreCase = true) }

    private fun cleanFamily(raw: String): String {
        val words = raw.trim().split(Regex("\\s+"))
        return words.takeLast(2).joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }.trim()
    }

    private fun powerUpDustCost(lines: List<String>, labelIndex: Int): Int? {
        val inline = NUMBER_LINE.find(lines[labelIndex].replace(POWER_UP_LABEL, "").trim())
            ?.groupValues?.get(1)?.filter { it.isDigit() }?.toIntOrNull()
        if (inline != null && inline in DUST_COST_RANGE) return inline
        for (offset in listOf(1, -1, 2, -2)) {
            val i = labelIndex + offset
            if (i !in lines.indices) continue
            val value = NUMBER_LINE.find(lines[i])
                ?.groupValues?.get(1)?.filter { it.isDigit() }?.toIntOrNull() ?: continue
            if (value in DUST_COST_RANGE) return value
        }
        return null
    }

    private val DUST_COST_RANGE = 100..20_000

    private fun numberNear(lines: List<String>, labelIndex: Int): Int? {
        val inline = NUMBER_LINE.find(
            lines[labelIndex].replace(CANDY_XL_LABEL, "").replace(CANDY_LABEL, "").trim(),
        )
        if (inline != null) return inline.groupValues[1].filter { it.isDigit() }.toIntOrNull()

        var best: Int? = null
        var bestDistance = Int.MAX_VALUE
        for (offset in listOf(-1, 1, -2, 2)) {
            val i = labelIndex + offset
            if (i !in lines.indices) continue
            val match = NUMBER_LINE.find(lines[i]) ?: continue
            val value = match.groupValues[1].filter { it.isDigit() }.toIntOrNull() ?: continue
            if (abs(offset) < bestDistance) {
                best = value
                bestDistance = abs(offset)
            }
        }
        return best
    }

    private fun findSpecies(lines: List<String>, speciesNames: Collection<String>): String? {
        val sorted = speciesNames.sortedByDescending { it.length }
        for (line in lines) {
            if (CANDY_LABEL.containsMatchIn(line)) continue
            val stripped = line.trim().trimEnd('♂', '♀', ' ')
            sorted.firstOrNull { it.equals(stripped, ignoreCase = true) }?.let { return it }
        }
        for (line in lines) {
            if (CANDY_LABEL.containsMatchIn(line)) continue
            sorted.firstOrNull { it.length >= 5 && line.contains(it, ignoreCase = true) }
                ?.let { return it }
        }
        return null
    }
}
