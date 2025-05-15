package com.example.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val filePath: String
)

object Songs : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val artist = varchar("artist", 255)
    val filePath = varchar("file_path", 255)

    override val primaryKey = PrimaryKey(id)
}