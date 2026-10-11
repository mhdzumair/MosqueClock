package com.mosque.prayerclock.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorThemeRegistryTest {
    @Test
    fun `theme ids are unique`() {
        val themes = AppColorThemes.getAllThemes()

        assertEquals(themes.size, themes.map { it.id }.distinct().size)
    }

    @Test
    fun `v2 portable palettes are registered`() {
        val ids = AppColorThemes.getAllThemes().map { it.id }

        assertTrue(
            ids.containsAll(
                listOf(
                    "daylight_ivory", "morning_mist", "parchment_sepia", "sand_terracotta",
                    "slate_steel", "forest_moss", "rose_copper", "ink_saffron",
                    "jewel_amethyst", "mono_contrast",
                    "horizon_dusk", "notice_paper", "amber_matrix",
                ),
            ),
        )
    }
}
