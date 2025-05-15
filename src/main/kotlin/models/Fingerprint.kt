package com.example.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class Fingerprint(
    val id: Int,
    val songId: Int,
    val hash: String,
    val offset: Int
)

object Fingerprints : Table() {
    val id = integer("id").autoIncrement()
    val songId = integer("song_id").references(Songs.id)
    val hash = varchar("hash", 255)
    val offset = integer("offset")

    override val primaryKey = PrimaryKey(id)
}
