import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Elements
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import okio.Path
import okio.FileSystem
import okio.buffer
import okio.use
import okio.Path.Companion.toPath
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class TrackData(
    val position: Int,
    @Serializable(with = DurationSerializer::class)
    val time: Duration,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant,
    @Serializable(with = DurationSerializer::class)
    val duration: Duration,
    val title: String,
    val artist: String,
    val genre: String?,
    val deck: String?,
    val key: String?,
) {
    @Serializable(with = InstantSerializer::class)
    val endTime = timestamp + duration
}

val durationFormat = LocalTime.Format {
//    optional {
    hour(padding = Padding.NONE)
    char('h')
//    }
    minute(padding = Padding.ZERO)
    char(':')
    second(padding = Padding.ZERO)
}

fun parseHtmlFile(filePath: Path): Tracklist<TrackData>? {
    return try {
        val data = FileSystem.SYSTEM.source(filePath).buffer().use { source ->
            source.readUtf8()
        }

        val document = Ksoup.parse(data)
        val h1 = document.selectFirst("h1")
        if (h1 == null) {
            println("Title (h1) not found")
            return null
        }
        val title = h1.text().trim().substringAfter("Track List: ")
        val table = document.selectFirst("table.border")
        if (table == null) {
            println("Table not found")
            return null
        }

        val tracks = mutableListOf<TrackData>()
        val rows = table.select("tr")

        val headerCells = rows[0].select("th").map { it.text() }

        val missingFields = mutableSetOf<String>()
        val missingOptionalFields = mutableSetOf<String>()
        fun findField(fieldName: String): Elements.() -> String {
            val index = headerCells.indexOf(fieldName) // .takeUnless { it < 0 }

            if (index < 0) {
                missingFields += fieldName
//                    println("missing field $fieldName")
//                        error("missing field '$fieldName' \navailable fields: $headerCells")

            }

            return {
                this[index].text().trim()
            }
        }

        fun findFieldOptional(fieldName: String): Elements.() -> String? {
            val index = headerCells.indexOf(fieldName) // .takeUnless { it < 0 }

            if (index < 0) {
                missingOptionalFields += fieldName
                return { null }
//                    println("missing field $fieldName")
//                        error("missing field '$fieldName' \navailable fields: $headerCells")

            }

            return {
                this[index].text().trim()
            }
        }

        val trackNumField = findFieldOptional("Num.")
        val titleField = findField("Title")
        val artistField = findField("Artist")
        val genreField = findFieldOptional("Genre")
        val startTimeField = findField("Start Time")
        val durationField = findField("Duration")
        val deckField = findFieldOptional("Deck")
        val keyField = findFieldOptional("Key")


        if (missingOptionalFields.isNotEmpty()) {
            println("Missing optional fields:")
            println(missingOptionalFields.joinToString { "'$it'" })
        }
        if (missingFields.isNotEmpty()) {
            println("Missing required fields:")
            println(missingFields.joinToString { "'$it'" })
        }
        if (missingFields.isNotEmpty() || missingOptionalFields.isNotEmpty()) {
            println("Available Fields:")
            println(headerCells.joinToString { "'$it'" })

        }

        if (missingFields.isNotEmpty()) {
            return null
        }

        val firstRow = rows[1].select("td")
        val referenceTimestamp = parseInstant(firstRow.startTimeField())
        for (i in 1 until rows.size) { // Skip the first row (header row)
            val cells = rows[i].select("td")
//            println("parsing row: $cells")
            if (cells.size >= 10) {
                val timestamp = parseInstant(cells.startTimeField())
                tracks.add(
                    TrackData(
                        position = cells.trackNumField()?.toInt() ?: i,
                        time = timestamp - referenceTimestamp,
                        title = cells.titleField(),
                        artist = cells.artistField(),
                        genre = cells.genreField(),
                        timestamp = timestamp,
                        duration = run {
                            val duration = cells.durationField()

                            println("parsing duration: $duration")

                            val localTime = LocalTime.parse("0h" + duration, durationFormat)
//                            val localTime = LocalTime.parse(duration, durationFormat)
                            localTime.toSecondOfDay().seconds
                        },
                        deck = cells.deckField(),
                        key = cells.keyField()
                    )
//                        .also {
//                            println(it)
//                        }
                )
            }
        }

        Tracklist(
            title = title,
            tracks = tracks.sortedBy { it.timestamp }
        )
        //.sortedBy { it.trackNum }

    } catch (error: Exception) {
        println("Error reading file: \n$error")
//        error.printStackTrace()
        null
    }
}

val dateFormat = LocalDateTime.Format {
    year()
    char('/')
    monthNumber(padding = Padding.NONE)
    char('/')
    day(padding = Padding.NONE)
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

fun parseInstant(startTimeString: String): Instant {
    return LocalDateTime.parse(
        startTimeString,
        dateFormat
    ).toInstant(TimeZone.currentSystemDefault())
}

fun createTracklist(trackList: Tracklist<TrackData>) {
    val tracks = trackList.tracks
    val firstStartTime = tracks[0].timestamp
    var lastEndTime = tracks[0].timestamp

    val tracklistContent = tracks.joinToString("\n") { track ->
        val elapsedTime = track.timestamp - firstStartTime

        val endTimestamp = track.endTime - firstStartTime
        val overlap = lastEndTime - track.timestamp
//        val formattedTime = "${elapsedTime.formatTimestamp()} - ${endTimestamp.formatTimestamp()} ${track.deck} $overlap"

        lastEndTime = track.endTime

        var artist = track.artist
        var title = track.title

        if (artist.isEmpty() && title.contains(" - ")) {
            val parts = title.split(" - ")
            artist = parts[0]
            title = parts[1]
        } else if (artist.isEmpty()) {
            artist = "N/A"
        }

        val debugInfo = """
            trackNum:   ${track.position}
            artist:  $artist
            title:   $title
            genre:   ${track.genre}
            start:   ${elapsedTime.formatTimestamp()}
            end:     ${endTimestamp.formatTimestamp()}
            overlap: $overlap
            ---
        """
//        deck:    ${track.deck}
            .trimIndent()
            .trim()
        println(debugInfo)

        "${elapsedTime.formatTimestamp()} $artist - $title"
    }

    val genreCount = tracks.groupingBy { it.genre }.eachCount()
    println("")
    println("GENRES: ")
    genreCount.entries.sortedByDescending { it.value }
        .forEach {
            println("${it.value} x ${it.key}")
        }

    val fileName = "tracklist_${trackList.title}.txt"

    val path = fileName.toPath()
    FileSystem.SYSTEM.sink(path).use { fileSink ->
        fileSink.buffer().use { bufferedSink ->
            bufferedSink.writeUtf8(tracklistContent)
        }
    }
    println()
    println("Tracklist exported to $fileName")
}

fun formatTimestamp(milliseconds: Double): String {
    fun pad(num: Int): String = num.toString().padStart(2, '0')

    val hours = pad((milliseconds / (1000 * 60 * 60)).toInt())
    val minutes = pad(((milliseconds % (1000 * 60 * 60)) / (1000 * 60)).toInt())
    val seconds = pad(((milliseconds % (1000 * 60)) / 1000).toInt())

    return "$hours:$minutes:$seconds"
}

val timestampFormat = LocalTime.Format {
    hour(Padding.ZERO)
    char(':')
    minute(Padding.ZERO)
    char(':')
    second(Padding.ZERO)
}

fun Duration.formatTimestamp(): String {
    val localTime = LocalTime.fromSecondOfDay(inWholeSeconds.toInt())
    return localTime.format(timestampFormat)
//    fun pad(num: Int): String = num.toString().padStart(2, '0')
//    fun pad(num: Long): String = num.toString().padStart(2, '0')
//    return duration.toComponents { hours, minutes, seconds, _ ->
//        val hours = pad(hours)
//        val minutes = pad(minutes)
//        val seconds = pad(seconds)
//        "$hours:$minutes:$seconds"
//    }
}

fun cleanFilePath(filePath: String): String {
    var cleanedPath = filePath

    if (cleanedPath.startsWith("?")) {
        cleanedPath = cleanedPath.substring(1)
    }

    val htmlIndex = cleanedPath.indexOf(".html")
    if (htmlIndex != -1) {
        cleanedPath = cleanedPath.substring(0, htmlIndex + 5)
    }

    return cleanedPath
}

fun main(vararg args: String) {
    val documents = executeCommand("powershell.exe -Command [Environment]::GetFolderPath('MyDocuments')")
    println(documents)

    val filePath = args.getOrNull(0)
        ?: "HISTORY.html".takeIf {
            FileSystem.SYSTEM.exists(it.toPath())
        }
        ?: run {
            println("Enter the path to the HTML file: ")
            print("> ")
            readlnOrNull()?.trim() ?: return
        }
    println()
    println("parsing $filePath")

    val tracklist = parseHtmlFile(filePath.toPath())

    if (tracklist != null) {
//        createTracklist(parsedHtml)
        Template.write(tracklist, TrackData.serializer())
        genreBreakdown(tracklist) { genre }
    }

    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()
}

