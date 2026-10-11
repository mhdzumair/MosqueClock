package com.mosque.prayerclock.ui.components.screenfaces

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenFaceRegistryTest {
    @Test
    fun `registered face ids and aliases are unique`() {
        val allIds = ScreenFaceRegistry.faces.flatMap { listOf(it.id) + it.legacyIds }

        assertEquals(allIds.size, allIds.distinct().size)
    }

    @Test
    fun `legacy modern selections migrate to horizon faces`() {
        assertSame(HorizonScreenFace, ScreenFaceRegistry.get("modern_tv"))
        assertSame(HorizonMobileScreenFace, ScreenFaceRegistry.get("modern_mobile"))
    }

    @Test
    fun `unknown selection safely falls back to classic`() {
        assertSame(ClassicScreenFace, ScreenFaceRegistry.get("does_not_exist"))
        assertTrue(ScreenFaceRegistry.faces.contains(ClassicScreenFace))
    }

    @Test
    fun `v2 design expansion faces are registered`() {
        assertTrue(
            ScreenFaceRegistry.faces.map { it.id }.containsAll(
                listOf("ufuq", "safaa", "noor", "sahar", "kaash", "mihrab"),
            ),
        )
    }

    @Test
    fun `each tv face recommends its canonical palette`() {
        val recommendations = ScreenFaceRegistry.faces.associate { it.id to it.recommendedColorThemeId }

        assertEquals("horizon_dusk", recommendations["ufuq"])
        assertEquals("notice_paper", recommendations["safaa"])
        assertEquals("amber_matrix", recommendations["noor"])
        assertEquals("daylight_ivory", recommendations["sahar"])
        assertEquals("ink_saffron", recommendations["kaash"])
        assertEquals("sand_terracotta", recommendations["mihrab"])
    }
}
