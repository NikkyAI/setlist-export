import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Options
import okio.Path
import okio.buffer
import okio.use
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun parseCue(path: Path): CueFile {
    val content = FileSystem.SYSTEM.source(path).buffer().use { buf ->
        var performer: String? = null
        var title: String? = null
        var file: String? = null
        var fileFormat: String? = null
        val tracks = mutableListOf<CueTrack>()

        while (!buf.exhausted()) {
            when (
                val s = buf.select(
                    Options.of(
                        "PERFORMER".encodeUtf8(),
                        "TITLE".encodeUtf8(),
                        "FILE".encodeUtf8(),
                    ),
                )
            ) {
                -1 -> {
                    val line = buf.readUtf8Line()
                    println("found no option in line: '$line'")

                }

                0 -> {
                    println("> PERFORMER")
                    val line = buf.readUtf8LineStrict()
                    performer = line.substringAfter("\"")
                        .substringBeforeLast("\"")

                }

                1 -> {
                    println("> TITLE")
                    val line = buf.readUtf8LineStrict()
                    title = line.substringAfter("\"")
                        .substringBeforeLast("\"")
                }

                2 -> {
                    println("> FILE")
                    val line = buf.readUtf8LineStrict()
                    file = line.substringAfter("\"")
                        .substringBeforeLast("\"")
                    fileFormat = line.substringAfterLast("\"").trim()

                    while (true) {
                        buf.peek().readUtf8Line().also {
                            println("> FILE parsing '$it'")
                        }
                        when (
                            val trackSelect = buf.select(
                                Options.of(
                                    "  TRACK".encodeUtf8(),
                                ),
                            )
                        ) {
                            -1 -> {
                                break
                            }

                            0 -> {
                                println("> FILE > TRACK")
                                val line = buf.readUtf8LineStrict()
                                val trackNum: Int = line.trim().substringBefore(" ").toInt()
                                val trackType = line.trim().substringAfterLast(" ")
                                var trackTitle: String? = null
                                var trackPerformer: String? = null
                                var timestamp: Duration? = null

//                                while (indent("  ", "TRACK")) {
                                while (true) {
                                    buf.peek().readUtf8Line().also {
                                        println("> FILE > TRACK parsing '$it'")
                                    }
                                    when (
                                        val trackPropertiesSelect = buf.select(
                                            Options.of(
                                                "    TITLE".encodeUtf8(),
                                                "    PERFORMER".encodeUtf8(),
                                                "    INDEX 01 ".encodeUtf8(),
                                            ),
                                        )
                                    ) {
                                        -1 -> {
                                            break
                                        }

                                        0 -> {
                                            println("> FILE > TRACK > TITLE")
                                            val line = buf.readUtf8LineStrict()
                                            trackTitle = line.substringAfter("\"")
                                                .substringBeforeLast("\"")
                                            println("title: $title")
                                        }

                                        1 -> {
                                            println("> FILE > TRACK > PERFORMER")
                                            val line = buf.readUtf8LineStrict()
                                            trackPerformer = line.substringAfter("\"")
                                                .substringBeforeLast("\"")
                                            println("performer: $trackPerformer")
                                        }

                                        2 -> {
                                            println("> FILE > TRACK > INDEX 01")
                                            val line = buf.readUtf8LineStrict()

                                            val timestampString = line.trim()
                                            val components = timestampString.split(":").map { it.toInt() }

                                            timestamp =
                                                components[0].minutes + components[1].seconds + components[2].milliseconds
                                            println("timestamp: ${timestamp.formatTimestamp()}")
                                        }

                                    }
                                }
                                // after TRACK
                                println("track processed")
                                tracks.add(
                                    CueTrack(
                                        trackNum = trackNum,
                                        type = trackType,
                                        title = trackTitle ?: error("missing tag TITLE"),
                                        performer = trackPerformer ?: error("missing tag PERFORMER"),
                                        timestamp = timestamp ?: error("missing tag INDEX"),
                                    )
                                )
                            }
                        }
                    }
                    println("all tracks processed?")
                    // after FILE

                }

                else -> {
                    error("UNEXPECTED $s")
                }
            }
        }
        CueFile(
            performer ?: error("missing tag PERFORMER"),
            title ?: error("missing tag TITLE"),
            file ?: error("missing tag FILE"),
            fileFormat ?: error("missing tag FILE"),
            tracks = tracks
        )
    }

    return content
}

data class CueFile(
    val performer: String,
    val title: String,
    val file: String,
    val fileFormat: String,
    val tracks: List<CueTrack>,
)

data class CueTrack(
    val trackNum: Int,
    val type: String = "AUDIO",
    val title: String,
    val performer: String,
    val timestamp: Duration,
)
