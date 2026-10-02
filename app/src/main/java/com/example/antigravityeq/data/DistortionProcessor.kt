package com.example.antigravityeq.data

import kotlin.math.*

/**
 * Dedicated nonlinear distortion processor.
 *
 * Clean-room implementation of standard waveshaping techniques.
 * It is intentionally independent from the existing tube/AnalogX stages.
 */
class DistortionProcessor(private val sampleRate: Int = 48000) {

    enum class Mode(val label: String) {
        SOFT_CLIP("Soft clip"),
        TANH("Tanh"),
        HARD_CLIP("Hard clip"),
        FOLDBACK("Foldback"),
        ASYMMETRIC("Asymmetric"),
        SINE_FOLD("Sine fold")
    }

    private var toneStateL = 0f
    private var toneStateR = 0f

    fun reset() {
        toneStateL = 0f
        toneStateR = 0f
    }

    private fun shape(x: Float, mode: Mode, asymmetry: Float): Float {
        val a = asymmetry.coerceIn(-1f, 1f)
        val biased = x + a * 0.18f

        return when (mode) {
            Mode.SOFT_CLIP -> {
                val k = 1.8f
                (biased * (1f + k)).let { v ->
                    v / (1f + abs(v))
                }
            }
            Mode.TANH -> tanh(biased)
            Mode.HARD_CLIP -> biased.coerceIn(-1f, 1f)
            Mode.FOLDBACK -> {
                var v = biased
                if (abs(v) <= 1f) v
                else {
                    repeat(8) {
                        if (v > 1f) v = 2f - v
                        if (v < -1f) v = -2f - v
                        if (abs(v) <= 1f) return@repeat
                    }
                    v.coerceIn(-1f, 1f)
                }
            }
            Mode.ASYMMETRIC -> {
                val pos = max(0f, biased)
                val neg = max(0f, -biased)
                val posCurve = tanh(pos * 1.35f)
                val negCurve = tanh(neg * (0.85f + 0.55f * (a + 1f)))
                posCurve - negCurve
            }
            Mode.SINE_FOLD -> sin(biased * Math.PI.toFloat() * 0.5f).coerceIn(-1f, 1f)
        }
    }

    private fun tone(sample: Float, cutoffHz: Float, state: Float): Pair<Float, Float> {
        val cutoff = cutoffHz.coerceIn(200f, (sampleRate * 0.45f).coerceAtLeast(200f))
        val alpha = (1f - exp(-2f * Math.PI.toFloat() * cutoff / sampleRate.toFloat())).coerceIn(0.001f, 1f)
        val next = state + alpha * (sample - state)
        return next to next
    }

    fun processStereo(
        left: Float,
        right: Float,
        driveDb: Float,
        mixPercent: Float,
        toneHz: Float,
        outputDb: Float,
        asymmetryPercent: Float,
        mode: Mode
    ): Pair<Float, Float> {
        val drive = 10f.pow(driveDb.coerceIn(0f, 48f) / 20f)
        val wet = (mixPercent.coerceIn(0f, 100f) / 100f)
        val output = 10f.pow(outputDb.coerceIn(-12f, 6f) / 20f)
        val asym = asymmetryPercent.coerceIn(-100f, 100f) / 100f

        val wetL = shape(left * drive, mode, asym)
        val wetR = shape(right * drive, mode, asym)

        val toneL = tone(wetL, toneHz, toneStateL)
        val toneR = tone(wetR, toneHz, toneStateR)
        toneStateL = toneL.second
        toneStateR = toneR.second

        val outL = (left * (1f - wet) + toneL.first * wet) * output
        val outR = (right * (1f - wet) + toneR.first * wet) * output
        return outL.coerceIn(-1.5f, 1.5f) to outR.coerceIn(-1.5f, 1.5f)
    }
}
