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
import okio.Path
import okio.FileSystem
import okio.buffer
import okio.use
import okio.Path.Companion.toPath
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TrackList(
    val title: String,
    val tracks: List<TrackData>
)

data class TrackData(
    val trackNum: Int,
    val title: String,
    val artist: String,
    val genre: String?,
    val startTime: Instant,
    val duration: Duration,
    val deck: String?,
) {
    val endTime = startTime + duration
}

val durationFormat = LocalTime.Format {
    hour(padding = Padding.NONE)
    char('h')
    minute(padding = Padding.ZERO)
    char(':')
    second(padding = Padding.ZERO)
}

fun parseHtmlFile(filePath: Path): TrackList? {
    return try {
        val data = FileSystem.SYSTEM.source(filePath).buffer().use { source ->
            source.readUtf8()
        }

        val document = Ksoup.parse(data)
        val h1 = document.selectFirst("h1")
        if(h1 == null) {
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

            if(index < 0) {
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

            if(index < 0) {
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


        if(missingOptionalFields.isNotEmpty()) {
            println("Missing optional fields:")
            println(missingOptionalFields.joinToString { "'$it'" })
        }
        if(missingFields.isNotEmpty()) {
            println("Missing required fields:")
            println(missingFields.joinToString { "'$it'" })
        }
        if(missingFields.isNotEmpty() || missingOptionalFields.isNotEmpty()) {
            println("Available Fields:")
            println(headerCells.joinToString { "'$it'" })

        }

        if(missingFields.isNotEmpty()) {
            return null
        }

        for (i in 1 until rows.size) { // Skip the first row (header row)
            val cells = rows[i].select("td")
//            println("parsing row: $cells")
            if (cells.size >= 10) {
                tracks.add(
                    TrackData(
                        trackNum = cells.trackNumField()?.toInt() ?: i,
                        title = cells.titleField(),
                        artist = cells.artistField(),
                        genre = cells.genreField(),
                        startTime = parseStartTime(cells.startTimeField()),
                        duration = run {
                            val duration = cells.durationField()

//                            println("parsing duration: $duration")

                            val localTime = LocalTime.parse("0h" + duration, durationFormat)
                            localTime.toSecondOfDay().seconds
                        },
                        deck = cells.deckField()
                    )
//                        .also {
//                            println(it)
//                        }
                )
            }
        }

        TrackList(
            title = title,
            tracks = tracks.sortedBy {it.startTime}
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

fun parseStartTime(startTimeString: String): Instant {
    return LocalDateTime.parse(
        startTimeString,
        dateFormat
    ).toInstant(TimeZone.currentSystemDefault())
}

fun createTracklist(trackList: TrackList) {
    val tracks = trackList.tracks
    val firstStartTime = tracks[0].startTime
    var lastEndTime = tracks[0].startTime

    val tracklistContent = tracks.joinToString("\n") { track ->
        val elapsedTime = track.startTime - firstStartTime

        val endTimestamp = track.endTime - firstStartTime
        val overlap = lastEndTime - track.startTime
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
            trackNum:   ${track.trackNum}
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
    val filePath = args.getOrNull(0)
//     ?: "C:\\Users\\nikky\\Downloads\\HISTORY.html"
//     ?: "Timeship - 250309 - Open Decks.html"
//        ?: "C:\\Users\\nikky\\Downloads\\Timeship - Open Decks - Ignore Cued Track.html"
        ?: run {
            println("Enter the path to the HTML file: ")
            print("> ")
            readlnOrNull()?.trim() ?: return
        }
    println()
    println("parsing $filePath")

    val parsedHtml = parseHtmlFile(filePath.toPath())

    if (parsedHtml != null) {
        createTracklist(parsedHtml)
    }

    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()
}

