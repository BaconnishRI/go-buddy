package com.baconnish.gobuddy

import com.baconnish.gobuddy.domain.ScanConsensus
import com.baconnish.gobuddy.domain.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanConsensusTest {

    @Test
    fun `matching frames settle`() {
        val a = ScanResult(cp = 2387, hpMax = 158)
        val b = ScanResult(cp = 2387, hpMax = 158, stardust = 5000)
        assertTrue(ScanConsensus.settled(a, b))
    }

    @Test
    fun `cp still counting up does not settle`() {
        val a = ScanResult(cp = 1130, hpMax = 158)
        val b = ScanResult(cp = 2387, hpMax = 158)
        assertFalse(ScanConsensus.settled(a, b))
    }

    @Test
    fun `appraisal frames without cp settle on matching bars`() {
        val a = ScanResult(hpMax = 158, ivAtk = 12, ivDef = 10, ivSta = 11)
        val b = ScanResult(hpMax = 158, ivAtk = 12, ivDef = 10, ivSta = 11)
        assertTrue(ScanConsensus.settled(a, b))
    }

    @Test
    fun `bars still filling do not settle`() {
        val a = ScanResult(hpMax = 158, ivAtk = 7, ivDef = 5, ivSta = 9)
        val b = ScanResult(hpMax = 158, ivAtk = 12, ivDef = 10, ivSta = 11)
        assertFalse(ScanConsensus.settled(a, b))
    }

    @Test
    fun `empty frames never settle`() {
        assertFalse(ScanConsensus.settled(ScanResult(), ScanResult()))
    }

    @Test
    fun `pick takes the later of an agreeing pair`() {
        val frames = listOf(
            ScanResult(cp = 1130, hpMax = 158),
            ScanResult(cp = 2387, hpMax = 158),
            ScanResult(cp = 2387, hpMax = 158),
        )
        assertEquals(2, ScanConsensus.pick(frames))
    }

    @Test
    fun `pick falls back to highest cp when nothing agrees`() {
        val frames = listOf(
            ScanResult(cp = 512, hpMax = 158),
            ScanResult(cp = 2387, hpMax = 158),
            ScanResult(cp = 2381, hpMax = 158),
        )
        assertEquals(1, ScanConsensus.pick(frames))
    }

    @Test
    fun `pick prefers a later frame on cp ties`() {
        val frames = listOf(
            ScanResult(cp = 2387),
            ScanResult(cp = 2387, hpMax = 158),
        )
        assertEquals(1, ScanConsensus.pick(frames))
    }

    @Test
    fun `pick falls back to latest non-empty frame without cp`() {
        val frames = listOf(
            ScanResult(hpMax = 158, ivAtk = 7, ivDef = 5, ivSta = 9),
            ScanResult(hpMax = 158, ivAtk = 12, ivDef = 10, ivSta = 11),
            ScanResult(),
        )
        assertEquals(1, ScanConsensus.pick(frames))
    }

    @Test
    fun `pick handles all-empty and empty list`() {
        assertEquals(1, ScanConsensus.pick(listOf(ScanResult(), ScanResult())))
        assertEquals(-1, ScanConsensus.pick(emptyList()))
    }
}
