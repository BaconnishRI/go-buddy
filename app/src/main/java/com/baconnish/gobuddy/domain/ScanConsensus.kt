package com.baconnish.gobuddy.domain

object ScanConsensus {

    fun settled(earlier: ScanResult, later: ScanResult): Boolean =
        (later.cp != null || later.ivAtk != null) &&
            earlier.cp == later.cp &&
            earlier.hpMax == later.hpMax &&
            earlier.ivAtk == later.ivAtk &&
            earlier.ivDef == later.ivDef &&
            earlier.ivSta == later.ivSta

    fun pick(results: List<ScanResult>): Int {
        if (results.isEmpty()) return -1
        for (i in results.indices.reversed()) {
            for (j in i - 1 downTo 0) {
                if (settled(results[j], results[i])) return i
            }
        }
        val bestCp = results.withIndex()
            .filter { it.value.cp != null }
            .maxWithOrNull(compareBy({ it.value.cp }, { it.index }))
        if (bestCp != null) return bestCp.index
        for (i in results.indices.reversed()) {
            if (!results[i].isEmpty) return i
        }
        return results.lastIndex
    }
}
