package com.example.db

import com.example.models.Song
import com.example.models.Songs
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import javax.xml.crypto.Data

class SongRepository {
    suspend fun create(song: Song): Song {
        var id = 0
        DatabaseFactory.dbQuery {
            id = Songs.insert {
                it[title] = song.title
                it[artist] = song.artist
                it[filePath] = song.filePath
            }[Songs.id]
        }
        return song.copy(id = id)
    }

    suspend fun read(id: Int) =
        DatabaseFactory.dbQuery {
            Songs.select { Songs.id.eq(id) }
                .map { toSong(it) }
                .singleOrNull()
        }

    suspend fun readAll(): List<Song> {
        return DatabaseFactory.dbQuery {
            Songs.selectAll().map { toSong(it) }
        }
    }

    private fun toSong(row: ResultRow): Song =
        Song(
            id = row[Songs.id],
            title = row[Songs.title],
            artist = row[Songs.artist],
            filePath = row[Songs.filePath]
        )
}