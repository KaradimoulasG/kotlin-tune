package com.example.fingerprinting

class Fingerprinter {
    companion object {
        // Parameters for fingerprinting
        const val TARGET_ZONE_SIZE = 5  // How many points ahead to look
        const val FAN_VALUE = 15        // How many pairs to generate for each anchor point
    }

    private val audioProcessor = AudioProcessor()

    fun fingerprint(filePath: String): List<Pair<String, Int>> {
        // Process audio to get spectrogram points
        val spectrogramPoints = audioProcessor.processFile(filePath)

        // Generate fingerprint hashes with their time offsets
        return generateHashes(spectrogramPoints)
    }

    fun fingerprintAudioData(audioData: FloatArray): List<Pair<String, Int>> {
        // Process audio to get spectrogram points
        val spectrogramPoints = audioProcessor.processAudioData(audioData)

        // Generate fingerprint hashes with their time offsets
        return generateHashes(spectrogramPoints)
    }

    private fun generateHashes(spectrogramPoints: List<List<Int>>): List<Pair<String, Int>> {
        val fingerprints = mutableListOf<Pair<String, Int>>()

        for (i in spectrogramPoints.indices) {
            // Use each point as an anchor
            val anchorPoint = spectrogramPoints[i]
            val anchorTime = i

            // Look ahead in the target zone
            for (j in i + 1 until minOf(i + TARGET_ZONE_SIZE, spectrogramPoints.size)) {
                val targetPoint = spectrogramPoints[j]
                val targetTime = j
                val timeDelta = targetTime - anchorTime

                // Create a hash from the anchor and target frequencies plus time delta
                val hash = hashFingerprint(anchorPoint, targetPoint, timeDelta)

                // Store hash with its offset time
                fingerprints.add(Pair(hash, anchorTime))
            }
        }

        return fingerprints
    }

    private fun hashFingerprint(anchorPoint: List<Int>, targetPoint: List<Int>, timeDelta: Int): String {
        // Combine frequency points and time delta into a single hash
        val combined = anchorPoint.joinToString(",") + ":" +
                targetPoint.joinToString(",") + ":" +
                timeDelta.toString()

        // Use a simple hashing mechanism
        return combined.hashCode().toString(16)
    }
}