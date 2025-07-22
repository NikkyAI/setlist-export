import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.math.max

//@Serializable
//abstract class CommonSong(
//    @Serializable(with=DurationSerializer::class)
//    val time: Duration,
//    @Serializable(with=InstantSerializer::class)
//    val timestamp: Instant,
//    val title: String,
//    val artist: String,
//
//)

object Template {
    val default = """
        {time} {artist} - {title}
    """.trimIndent().trim()
    fun load(): (JsonObject) -> String {
        val templatePath = "template.txt".toPath()
        val exists = FileSystem.SYSTEM.exists(templatePath)
        val templateString = if(exists) {
            FileSystem.SYSTEM.read(templatePath) {
                readUtf8()
            }.trim()
        } else {
            FileSystem.SYSTEM.write(templatePath) {
                writeUtf8(
                    default
                )
            }
            default
        }

        return { song: JsonObject ->
            song.entries.fold(templateString) { t, (key, value) ->
                t.replace("{$key}", value.jsonPrimitive.contentOrNull.orEmpty())
            }
        }
    }

    fun <E> write(
        basename: String,
        songs: List<E>,
        serializer: KSerializer<E>
    ) {
        val formatter = load()

        val jsonString = json.encodeToString(ListSerializer(serializer), songs)
        println(jsonString)

        val encodedSongs = json.decodeFromString(
            ListSerializer(JsonObject.serializer()),
            jsonString
        )
//        val encodedSongs = .json.encodeToJsonElement(ListSerializer(serializer), songs)
//            .jsonArray

        val txt = encodedSongs
            .joinToString("\n") {
                formatter(it)
            }

        val txtPath = "${basename}.txt".toPath()
        println("writing to $txtPath")
        FileSystem.SYSTEM.write(txtPath) {
            writeUtf8(txt)
        }

        val debugPath = ".out".toPath()
        FileSystem.SYSTEM.createDirectories(debugPath)
        val jsonPath = debugPath / ("${basename}.json").toPath()
        println("writing to $jsonPath")
        FileSystem.SYSTEM.write(jsonPath) {
            writeUtf8(jsonString)
        }


        val keys = encodedSongs.first().keys
        val values = encodedSongs.map {
            it.entries.associate {
                it.key to it.value.jsonPrimitive.contentOrNull.orEmpty()
            }
        }
        val widths = keys.associateWith { key ->
            max(
                key.length,
            values.maxOf { it[key]?.length ?: 0 }
            )
        }

        val md =
            keys.joinToString(" | ", "| ", " | \n") { it.padEnd(widths[it] ?: 0) } +
            keys.joinToString("-|-", "|-", "-| \n") { "-".repeat(widths[it] ?: 0) } +
            values.joinToString("\n") { obj ->
                obj.entries.joinToString(" | ","| ", " |") { (key, value) ->
                    value.padEnd(widths[key] ?: 0)
                }
            }

        val mdPath = "${basename}.md".toPath()
        println("writing to $mdPath")
        FileSystem.SYSTEM.write(mdPath) {
            writeUtf8(md)
        }

    }

    fun <E> write(
        playlist: Playlist<E>,
        serializer: KSerializer<E>
    ) {
        write(
            basename = playlist.title,
            songs = playlist.songs,
            serializer = serializer
        )
    }
}

fun <E> genreBreakdown(
    tracks: List<E>,
    getGenre: E.() -> String?
) {
    if(tracks.all { it.getGenre() == null }) return

    val genreCount = tracks.groupingBy { it.getGenre() }.eachCount()
    println("")
    println("GENRES: ")
    genreCount.entries.sortedByDescending { it.value }
        .forEach {
            println("${it.value} x ${it.key}")
        }
}