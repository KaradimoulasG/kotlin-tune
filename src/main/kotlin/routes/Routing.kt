package com.example.routes

import com.example.db.FingerprintRepository
import com.example.db.SongRepository
import com.example.fingerprinting.Fingerprinter
import com.example.fingerprinting.Matcher
import com.example.misc.IdentifyResponse
import com.example.misc.readAudioData
import com.example.models.Fingerprint
import com.example.models.Song
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioSystem

fun Application.configureRouting() {


    val songRepository = SongRepository()
    val fingerprintRepository = FingerprintRepository()
    val matcher = Matcher(fingerprintRepository, songRepository)


    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }


        // Health check
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // Get all songs
        get("/songs") {
            val songs = songRepository.readAll()
            call.respond(songs)
        }

        // Add a new song
        post("/songs") {
            val multipart = call.receiveMultipart()
            var title = ""
            var artist = ""
            var tempFile: File? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "title" -> title = part.value
                            "artist" -> artist = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        // Save the uploaded file temporarily
                        val fileBytes = part.streamProvider().readBytes()
                        tempFile = withContext(Dispatchers.IO) {
                            val file = File.createTempFile(
                                "upload",
                                ".${part.originalFileName?.substringAfterLast('.') ?: "mp3"}"
                            )
                            file.writeBytes(fileBytes)
                            file
                        }
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (tempFile == null) {
                call.respond(HttpStatusCode.BadRequest, "No audio file provided")
                return@post
            }

            try {
                // Create song record
                val song = songRepository.create(Song(0, title, artist, tempFile!!.absolutePath))

                // Generate fingerprints
                val fingerprinter = Fingerprinter()
                val fingerprints = fingerprinter.fingerprint(tempFile!!.absolutePath)

                // Save fingerprints
                val fingerprintEntities = fingerprints.map { (hash, offset) ->
                    Fingerprint(0, song.id, hash, offset)
                }
                fingerprintRepository.createBatch(fingerprintEntities)

                call.respond(HttpStatusCode.Created, song)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error processing audio: ${e.message}")
            }
        }

        // Identify a song
        post("/identify") {
            val multipart = call.receiveMultipart()
            var tempFile: File? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        // Save the uploaded file temporarily
                        val fileBytes = part.streamProvider().readBytes()
                        tempFile = withContext(Dispatchers.IO) {
                            val file = File.createTempFile(
                                "identify",
                                ".${part.originalFileName?.substringAfterLast('.') ?: "mp3"}"
                            )
                            file.writeBytes(fileBytes)
                            file
                        }
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (tempFile == null) {
                call.respond(HttpStatusCode.BadRequest, "No audio file provided")
                return@post
            }

            try {
                // Read audio data
                val audioInputStream = AudioSystem.getAudioInputStream(tempFile)
                val audioData = readAudioData(audioInputStream)

                // Match against database
                val result = matcher.matchAudio(audioData)

                // Response
                if (result.song != null && result.confidence > 0.1) {
                    call.respond(
                        IdentifyResponse(
                            matched = true,
                            song = result.song,
                            confidence = result.confidence
                        )
                    )
                } else {
                    call.respond(
                        IdentifyResponse(
                            matched = false,
                            song = null,
                            confidence = result.confidence
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error identifying audio: ${e.message}")
            } finally {
                // Clean up temporary file
                tempFile?.delete()
            }
        }
    }
}
