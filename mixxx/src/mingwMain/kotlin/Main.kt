@file:OptIn(ExperimentalTime::class)

import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.impl.extensions.asDouble
import io.github.smyrgeorge.sqlx4k.impl.extensions.asDoubleOrNull
import io.github.smyrgeorge.sqlx4k.impl.extensions.asFloat
import io.github.smyrgeorge.sqlx4k.impl.extensions.asFloatOrNull
import io.github.smyrgeorge.sqlx4k.impl.extensions.asInt
import io.github.smyrgeorge.sqlx4k.impl.extensions.asIntOrNull
import io.github.smyrgeorge.sqlx4k.impl.extensions.asLong
import io.github.smyrgeorge.sqlx4k.sqlite.SQLite
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.parse
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val TMP = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
val FS = FileSystem.SYSTEM

val sqliteDatetimeFormat = DateTimeComponents.Format {
    year();
    char('-');
    monthNumber();
    char('-');
    day()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

val outputTimestampFormat = LocalTime.Format {
    hour(Padding.ZERO)
    char(':')
    minute(Padding.ZERO)
    char(':')
    second(Padding.ZERO)
}

fun Duration.formatTimestamp(): String {
    val localTime = LocalTime.fromSecondOfDay(inWholeSeconds.toInt())
    return localTime.format(outputTimestampFormat)
}

@OptIn(ExperimentalForeignApi::class)
fun main(vararg args: String) {

    val appdata = getenv("LOCALAPPDATA")?.toKString() ?: error("cannot lookup %APPDATA%")
    val dbPath = appdata.toPath(true) / "Mixxx" / "mixxxdb.sqlite"

    // Additionally, you can set minConnections, acquireTimeout, idleTimeout, etc.
    val options = Driver.Pool.Options.builder()
        .maxConnections(10)
        .build()

    /**
     * The following urls are supported:
     * `sqlite::memory:`            | Open an in-memory database.
     * `sqlite:data.db`             | Open the file `data.db` in the current directory.
     * `sqlite://data.db`           | Open the file `data.db` in the current directory.
     * `sqlite:///data.db`          | Open the file `data.db` from the root (`/`) directory.
     * `sqlite://data.db?mode=ro`   | Open the file `data.db` for read-only access.
     */
    val db = SQLite(
        url = "sqlite://$dbPath", // If the `test.db` file is not found, a new db will be created.
        options = options
    )

//    println(Clock.System.now().format(sqliteDatetimeFormat))

    runBlocking {

//        db.execute("PRAGMA key = '402fd482c38817c35ffa8ffb8c7d93143b749e7d315df7a81732a1ff43608497'")
        val playlists = db.fetchAll(
            """
                SELECT 
                       p.Name AS playlistName,
                       pt.position   AS position,
                       unixepoch(pt.pl_datetime_added) AS timestamp,
                       l.duration    AS duration,
                       l.title       AS title,
                       l.artist      AS artist,
                       l.album       AS album,
                       l.year        AS year,
                       l.bpm         AS bpm,
                       l.key         AS key,
                       l.genre       AS genre
                FROM Playlists p
                         JOIN
                     PlaylistTracks pt ON p.id = pt.playlist_id
                         LEFT JOIN
                     library l ON pt.track_id = l.id
                WHERE p.locked = 0
                -- GROUP BY p.Name, pt.position, p.position
                ORDER BY p.Name, pt.position ASC, p.position ASC;
            """.trimIndent()
        ).map { it ->
            it.rows.groupBy {
                it.get("playlistName").asString()
            }.map { (playlistName, rows) ->
                Playlist(
                    name = playlistName,
                    songs = rows.map { songRow ->
                        Song(
                            position = songRow.get("position").asInt(),
                            timestamp = songRow.get("timestamp").asLong().let {
                                Instant.fromEpochMilliseconds(it)
                            },
                            duration = songRow.get("duration").asDoubleOrNull()
                                ?.takeUnless { it == 0.0 }
                                ?.seconds,
                            title = songRow.get("title").asString(),
                            artist = songRow.get("artist").asStringOrNull(),
                            album = songRow.get("album").asStringOrNull(),
                            year = songRow.get("year").asStringOrNull(),
                            bpm = songRow.get("bpm").asFloatOrNull()
                                ?.takeUnless { it == 0.0f },
                            key = songRow.get("key").asStringOrNull()
                                ?.takeUnless(String::isBlank),
                            genre = songRow.get("genre").asStringOrNull(),
                        )
                    }
                )
            }
        }.getOrThrow()

        playlists.forEach { playlist ->
            val playlistStart = playlist.songs.first().timestamp
            val txt = playlist.songs.joinToString(
                separator = "\n",
                prefix = "         artist | song \n"
            ) { song ->
                val start = song.timestamp - playlistStart
                val timestamp = start.formatTimestamp()
                "$timestamp ${song.artist} | ${song.title}"
            }

            val txtPath = "${playlist.name}.txt".toPath()
            println("writing to $txtPath")
            FS.write(txtPath) {
                writeUtf8(txt)
            }
        }
    }
}
