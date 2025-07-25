@file:OptIn(ExperimentalTime::class)

import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.impl.extensions.asInt
import io.github.smyrgeorge.sqlx4k.impl.extensions.asIntOrNull
import io.github.smyrgeorge.sqlx4k.impl.extensions.asLong
import io.github.smyrgeorge.sqlx4k.sqlite.SQLite
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.winhttp.WinHttp
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.io.okio.asKotlinxIoRawSink
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val httpClient = HttpClient(WinHttp) {

}

val TMP = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
val FS = FileSystem.SYSTEM

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

    val sqlCipherPath = TMP / "rekordbox-history-converter" / "sqlcipher.exe"

    val appdata = getenv("APPDATA")?.toKString() ?: error("cannot lookup %APPDATA%")
    val encryptedPath = appdata.toPath(true) / "Pioneer" / "rekordbox" / "master.db"

    println(sqlCipherPath)

    // downloading sqlcipher
    runBlocking {
        if (FS.exists(sqlCipherPath)) {
            println("sqlcipher already downloaded")
            return@runBlocking
        }
        println("downloading sqlcipher")

        httpClient.prepareGet(urlString = "https://github.com/Katecca/sqlcipher-static-binary/raw/refs/heads/master/windows/x86_64/sqlcipher.exe")
            .execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                var count = 0L
                FS.createDirectories(sqlCipherPath.parent!!, mustCreate = false)
                FS.write(sqlCipherPath, mustCreate = false) {
                    val rawSink = asKotlinxIoRawSink()
                    while (!channel.exhausted()) {
                        val chunk = channel.readRemaining()
                        count += chunk.remaining

                        chunk.transferTo(rawSink)
                        println("Received $count bytes from ${httpResponse.contentLength()}")
                    }
                }
            }
        println("downloaded sqlcipher")
    }

    val dbPath = TMP / "rekordbox-history-converter" / "plaintext.db"
    FS.delete(dbPath, mustExist = false)

    // decoding master.db
    runBlocking {

        val sql = """
            PRAGMA key='402fd482c38817c35ffa8ffb8c7d93143b749e7d315df7a81732a1ff43608497';
            ATTACH DATABASE '$dbPath' AS plaintext KEY '';
            SELECT sqlcipher_export('plaintext');
            DETACH DATABASE plaintext;
        """.trimIndent()

        val response = executeCommand(
            "$sqlCipherPath $encryptedPath \"${sql.replace("\n", " ")}\"",
            redirectStderr = false
        )

        println(response)
    }

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
        val tracklists = db.fetchAll(
            """
                SELECT h.Name        AS HistoryName,
                       sh.TrackNo    AS TrackNo,
                       unixepoch(sh.updated_at) AS start,
                       c.Title       AS Title,
                       c.ReleaseYear AS year,
                       c.Length      AS Length,
                       c.BPM         AS BPM,
                       a.Name        AS Artist,
                       al.Name       AS Album,
                       g.Name        AS Genre,
                       l.Name        As Label,
                       k.ScaleName   AS ScaleName
                FROM djmdHistory h
                         JOIN
                     djmdSongHistory sh ON h.id = sh.HistoryID
                         LEFT JOIN
                     djmdContent c ON sh.ContentID = c.ID
                         LEFT JOIN
                     djmdArtist a ON c.ArtistID = a.ID
                         LEFT JOIN
                     djmdAlbum al ON c.AlbumID = al.ID
                         LEFT JOIN
                     djmdGenre g ON c.GenreID = g.ID
                         LEFT JOIN
                     djmdLabel l ON c.LabelID = l.ID
                         LEFT JOIN
                     djmdKey k ON c.KeyID = k.ID
                -- GROUP BY sh.HistoryID, sh.TrackNo
                ORDER BY sh.HistoryID, sh.TrackNo ASC;
            """.trimIndent()
        )
            .map {
                it.rows.groupBy {
                    it.get("HistoryName").asString()
                }.map { (historyName, rows) ->
                    val referenceTimestamp = rows.first().get("start").asLong().let {
                    Instant.fromEpochSeconds(it)
                }
                    Tracklist(
                        title = historyName,
                        tracks = rows.map { row ->
                            val timestamp = row.get("start").asLong().let {
                                Instant.fromEpochSeconds(it)
                            }
                            Song(
                                position = row.get("TrackNo").asInt(),
                                time = (timestamp - referenceTimestamp),
                                timestamp = timestamp,
                                duration = row.get("Length").asInt().seconds,
                                title = row.get("Title").asString(),
                                artist = row.get("Artist").asStringOrNull(),
                                label = row.get("Label").asStringOrNull(),
                                album = row.get("Album").asStringOrNull(),
                                genre = row.get("Genre").asStringOrNull(),
                                year = row.get("year").asIntOrNull(),
                                bpm = row.get("BPM").asInt() / 100.0f,
                                scale = row.get("ScaleName").asStringOrNull(),
                            )
                        }
                    )
                }
            }.getOrThrow()

        tracklists.forEach { tracklist ->
            Template.write(
                tracklist,
                Song.serializer(),
            )
            genreBreakdown(tracklist) { genre }
        }

        println("")
        println("PRESS ANY BUTTON TO CLOSE")
        readlnOrNull()
    }
}
