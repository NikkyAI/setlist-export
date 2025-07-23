@file:OptIn(ExperimentalTime::class)

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.XML
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import platform.posix.getenv
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalForeignApi::class, ExperimentalXmlUtilApi::class)
fun main(vararg args: String) {

    val localAppdata = getenv("LOCALAPPDATA")?.toKString() ?: error("cannot lookup %LOCALAPPDATA%")
    val databaseXmlPath = localAppdata.toPath() / "VirtualDJ" / "database.xml"

    val xml = XML {
        recommended {
            ignoreUnknownChildren()
            pedantic = false
        }
//        unknownChildHandler = UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
//
//            DEFAULT_UNKNOWN_CHILD_HANDLER.handleUnknownChildRecovering(input, inputKind, descriptor, name, candidates)
//        }

    }
    val xml2 = XML {
        recommended {
            pedantic = false
        }
//        unknownChildHandler = UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
//
//            DEFAULT_UNKNOWN_CHILD_HANDLER.handleUnknownChildRecovering(input, inputKind, descriptor, name, candidates)
//        }

    }

//    FileSystem.SYSTEM.source(databaseXmlPath).buffer().use {
//        it.readUtf8()
//    }
//        .let {
//            println("decoding XML")
//            xml.decodeFromString(VirtualDJDatabase.serializer(), it)
//        }
//        .let {
//            println(it)
//        }
    val virtualDjDb = FileSystem.SYSTEM.source(databaseXmlPath).buffer().use {
        it.readUtf8()
    }
        .let {
            println("decoding XML")
            xml2.decodeFromString(VirtualDJDatabase.serializer(), it)
        }
        .also {
            println(it)
        }

    val tracklists = virtualDjDb.songs.filter {
        it.comment?.startsWith("Recorded using VirtualDJ on ") ?: false
    }.map { recording ->
        val date = recording.comment!!.substringAfter("Recorded using VirtualDJ on ")
            .let {
                LocalDate.parse(it, LocalDate.Formats.ISO)
            }
        val tracks = recording.pois
            .filter { it.type == "cue" }
            .sortedBy { it.num ?: Int.MAX_VALUE }
            .map { cuePoint ->
                val name = cuePoint.name ?: error("missing name on $cuePoint")
//                val song = virtualDjDb.songs.firstOrNull { s ->
//                    val authors = s.tags.author?.replace("\\s+", " ")?.split(", ") ?: return@firstOrNull false
//                    val title = s.tags.title?.replace("\\s+", " ") ?: return@firstOrNull false
//                    val remix = s.tags.remix?.replace("\\s+", " ") ?: return@firstOrNull false
//                    val feat = authors.drop(1).takeUnless { it.isEmpty() }
//                    "${authors.first()} - $title ($remix)" == name
//                } ?: virtualDjDb.songs.firstOrNull { s ->
//                    val authors = s.tags.author?.replace("\\s+", " ")?.split(", ") ?: return@firstOrNull false
//                    val title = s.tags.title?.replace("\\s+", " ") ?: return@firstOrNull false
//                    val feat = authors.drop(1).takeUnless { it.isEmpty() }
//                    if(feat)
//                    "${authors.first()} - $title" == name
//                } ?: virtualDjDb.songs.firstOrNull { s->
//                    s.filePath.substringAfterLast("\\").substringBeforeLast(".")
//                        .replace(" ", "")
//                        .replace("&amp;", "") == name.replace(" ", "")
//                }
//                if(song == null) {
//                    println("MISSING TRACK for: $cuePoint")
//                }
//                Triple(cuePoint.pos.seconds, song, cuePoint.name)
                Track(
                    position =cuePoint.num?:0,
                    time = cuePoint.pos.seconds,
                    title = name
                )
            }

//        val firstSeen = recording.infos.firstSeen.let { Instant.fromEpochSeconds(it) }
//        val lastModified = recording.infos.lastModified.let { Instant.fromEpochSeconds(it) }

        val recordingDuration = recording.infos.songLength?.toDouble()?.seconds
//        println("recording on $date")
//        println("duration $recordingDuration")

//        tracks.forEach { (start, name) ->
//            println("${start.formatTimestamp()} $name")
//        }
        Tracklist(
            title = date.toString(),
            tracks = tracks,
        )
    }.filter {
        it.tracks.isNotEmpty()
    }

//    println(Clock.System.now().format(sqliteDatetimeFormat))


    runBlocking {
        tracklists.forEach { playlist ->
            Template.write(
                playlist,
                Track.serializer(),
                
            )
        }

        println("")
        println("PRESS ANY BUTTON TO CLOSE")
        readlnOrNull()
    }
}
