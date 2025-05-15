package com.example.fingerprinting

import com.example.db.FingerprintRepository
import com.example.db.SongRepository
import com.example.models.Song

class Matcher(
    private val fingerprintRepository: FingerprintRepository,
    private val songRepository: SongRepository
) {
    suspend fun matchAudio(audioData: FloatArray): MatchResult {
        val fingerprinter = Fingerprinter()
        val sampleFingerprints = fingerprinter.fingerprintAudioData(audioData)

        // Extract hashes from sample fingerprints
        val hashes = sampleFingerprints.map { it.first }

        // Find matching fingerprints in database
        val matchingFingerprints = fingerprintRepository.findMatchesByHashes(hashes)

        if (matchingFingerprints.isEmpty()) {
            return MatchResult(null, 0.0)
        }

        // For each potential match, calculate time offset histogram
        val bestMatch = findBestMatch(sampleFingerprints, matchingFingerprints)

        // Get song details
        val song = songRepository.read(bestMatch.first)

        return MatchResult(song, bestMatch.second)
    }

    private fun findBestMatch(
        sampleFingerprints: List<Pair<String, Int>>,
        matchingFingerprints: Map<Int, List<Pair<String, Int>>>
    ): Pair<Int, Double> {
        // Build a hash map for quick lookup of sample offsets
        val sampleHashToOffset = sampleFingerprints.associate { it }

        val songScores = mutableMapOf<Int, MutableMap<Int, Int>>()

        // Calculate time offset histograms for each song
        for ((songId, fingerprints) in matchingFingerprints) {
            val offsetHistogram = mutableMapOf<Int, Int>()

            for ((hash, dbOffset) in fingerprints) {
                val sampleOffset = sampleHashToOffset[hash] ?: continue

                // Calculate the time difference - if this is consistent across many fingerprints,
                // it's likely the same song at a different starting point
                val timeDiff = dbOffset - sampleOffset
                offsetHistogram[timeDiff] = (offsetHistogram[timeDiff] ?: 0) + 1
            }

            songScores[songId] = offsetHistogram
        }

        // Find the song with the highest number of aligned fingerprints
        var bestSongId = -1
        var bestScore = 0.0

        for ((songId, histogram) in songScores) {
            // Use the most common time offset as the score
            val bestCount = histogram.values.maxOrNull() ?: 0
            val confidence = bestCount.toDouble() / sampleFingerprints.size

            if (confidence > bestScore) {
                bestScore = confidence
                bestSongId = songId
            }
        }

        return Pair(bestSongId, bestScore)
    }

    data class MatchResult(
        val song: Song?,
        val confidence: Double
    )
}
