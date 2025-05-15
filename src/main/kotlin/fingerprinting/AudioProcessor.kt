package com.example.fingerprinting

import com.example.misc.complexArray
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.log10
import kotlin.math.pow

class AudioProcessor {
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHUNK_SIZE = 4096
        const val OVERLAP = CHUNK_SIZE / 2

        // Frequency bands for fingerprinting (Hz)
        val FREQ_BANDS = arrayOf(
            40 to 80,
            80 to 120,
            120 to 180,
            180 to 300
        )
    }

    fun processFile(filePath: String): List<List<Int>> {
        val audioData = readAudioFile(filePath)
        return processAudioData(audioData)
    }

    fun processAudioData(audioData: FloatArray): List<List<Int>> {
        val spectrogramPoints = mutableListOf<List<Int>>()

        // Process audio in chunks
        var position = 0
        while (position + CHUNK_SIZE <= audioData.size) {
            val chunk = audioData.copyOfRange(position, position + CHUNK_SIZE)

            // Apply window function (Hanning window)
            applyHanningWindow(chunk)

            // Compute FFT
            val fft = computeFFT(chunk)

            // Extract peak frequencies for each band
            val peaks = findPeakFrequencies(fft)
            spectrogramPoints.add(peaks)

            // Move to next position with overlap
            position += OVERLAP
        }

        return spectrogramPoints
    }

    private fun readAudioFile(filePath: String): FloatArray {
        val file = File(filePath)
        val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(file)

        // Convert to mono if needed and get samples as float array
        val format = audioInputStream.format
        val frameSize = format.frameSize
        val channels = format.channels

        // Read all bytes
        val bytes = audioInputStream.readAllBytes()
        val samples = FloatArray(bytes.size / frameSize)

        // Convert bytes to float samples (assuming 16-bit audio)
        for (i in samples.indices) {
            val sampleIndex = i * frameSize

            // Average all channels if multichannel
            var sum = 0f
            for (ch in 0 until channels) {
                val lo = bytes[sampleIndex + ch * 2]
                val hi = bytes[sampleIndex + ch * 2 + 1]
                val sample = ((hi.toInt() shl 8) or (lo.toInt() and 0xFF)) / 32768f
                sum += sample
            }
            samples[i] = sum / channels
        }

        audioInputStream.close()
        return samples
    }

    private fun applyHanningWindow(samples: FloatArray) {
        for (i in samples.indices) {
            val multiplier = 0.5 * (1 - kotlin.math.cos(2 * kotlin.math.PI * i / (samples.size - 1)))
            samples[i] = (samples[i] * multiplier).toFloat()
        }
    }

    private fun computeFFT(samples: FloatArray): Array<Complex> {
        val transformer = FastFourierTransformer(DftNormalization.STANDARD)

        // Pad array to power of 2 if needed
        val paddedSize = 2.0.pow(kotlin.math.ceil(log10(samples.size.toDouble()) / log10(2.0))).toInt()
        val paddedSamples = complexArray(paddedSize) { Complex(if (it < samples.size) samples[it].toDouble() else 0.0, 0.0) }

        return transformer.transform(paddedSamples, TransformType.FORWARD)
    }

    private fun findPeakFrequencies(fft: Array<Complex>): List<Int> {
        val magnitudes = fft.take(fft.size / 2).mapIndexed { index, complex ->
            Pair(index, complex.abs())
        }

        // Return peak frequency index for each band
        return FREQ_BANDS.map { (lowFreq, highFreq) ->
            // Convert Hz to FFT bin index
            val lowBin = (lowFreq * fft.size / SAMPLE_RATE).toInt()
            val highBin = (highFreq * fft.size / SAMPLE_RATE).toInt().coerceAtMost(magnitudes.size - 1)

            // Find peak in this frequency band
            val peakIndex = magnitudes.subList(lowBin, highBin + 1)
                .maxByOrNull { it.second }?.first ?: lowBin

            peakIndex
        }
    }
}