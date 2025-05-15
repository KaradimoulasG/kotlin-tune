package com.example.misc

import com.example.models.Song
import kotlinx.serialization.Serializable
import org.apache.commons.math3.complex.Complex
import javax.sound.sampled.AudioInputStream


@Serializable
data class IdentifyResponse(
    val matched: Boolean,
    val song: Song?,
    val confidence: Double
)

fun readAudioData(audioInputStream: AudioInputStream): FloatArray {
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

// ComplexArray extension - helper for FFT
fun complexArray(size: Int, init: (Int) -> Complex): Array<Complex> {
    return Array(size) { init(it) }
}