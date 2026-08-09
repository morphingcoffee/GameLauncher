package com.morphingcoffee.gamelauncher.core.designsystem.components

import org.jetbrains.skia.RuntimeEffect
import kotlin.test.Test
import kotlin.test.assertNotNull

class BackgroundSkslCompileTest {
    @Test
    fun spectralTopology_compiles() {
        assertNotNull(RuntimeEffect.makeForShader(BackgroundSksl.SPECTRAL_TOPOLOGY))
    }

    @Test
    fun backplaneLive_compiles() {
        assertNotNull(RuntimeEffect.makeForShader(BackgroundSksl.BACKPLANE_LIVE))
    }

    @Test
    fun isoLattice_compiles() {
        assertNotNull(RuntimeEffect.makeForShader(BackgroundSksl.ISO_LATTICE))
    }

    @Test
    fun draftBlueprint_compiles() {
        assertNotNull(RuntimeEffect.makeForShader(BackgroundSksl.DRAFT_BLUEPRINT))
    }
}
