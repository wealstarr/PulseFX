package com.example.antigravityeq.data

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DistortionProcessorTest {

    @Test
    fun tanhDriveProducesNonlinearOutput() {
        val processor = DistortionProcessor(48000)
        val (left, right) = processor.processStereo(
            left = 0.5f,
            right = -0.5f,
            driveDb = 18f,
            mixPercent = 100f,
            toneHz = 20000f,
            outputDb = 0f,
            asymmetryPercent = 0f,
            mode = DistortionProcessor.Mode.TANH
        )

        assertTrue(abs(left) < 1f)
        assertTrue(abs(right) < 1f)
        assertTrue(left > 0f)
        assertTrue(right < 0f)
    }

    @Test
    fun dryMixPreservesInput() {
        val processor = DistortionProcessor(48000)
        val (left, right) = processor.processStereo(
            left = 0.37f,
            right = -0.21f,
            driveDb = 48f,
            mixPercent = 0f,
            toneHz = 200f,
            outputDb = 0f,
            asymmetryPercent = 100f,
            mode = DistortionProcessor.Mode.HARD_CLIP
        )

        assertTrue(abs(left - 0.37f) < 0.0001f)
        assertTrue(abs(right + 0.21f) < 0.0001f)
    }
}
