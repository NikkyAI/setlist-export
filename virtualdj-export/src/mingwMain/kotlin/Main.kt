@file:OptIn(ExperimentalTime::class)

import com.saveourtool.okio.safeToRealPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.datetime.LocalDate
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.XML
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import platform.posix.getenv
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalForeignApi::class, ExperimentalXmlUtilApi::class)
fun main(vararg args: String) {
    println("args: ${args.toList()}")

    val localAppdata = getenv("LOCALAPPDATA")?.toKString() ?: error("cannot lookup %LOCALAPPDATA%")
    val databaseXmlPath = localAppdata.toPath() / "VirtualDJ" / "database.xml"

    val xmlLenient = XML {
        recommended {
            ignoreUnknownChildren()
            pedantic = false
        }
    }
    val xML = XML {
        recommended {
            pedantic = false
        }
    }
//    FileSystem.SYSTEM.source(databaseXmlPath).buffer().use {
//        it.readUtf8()
//    }
//        .let {
//            println("decoding XML")
//            xmlLenient.decodeFromString(VirtualDJDatabase.serializer(), it)
//        }
//        .let {
//            println(it)
//        }
    val database = if(FileSystem.SYSTEM.exists(databaseXmlPath)) {
        FileSystem.SYSTEM.source(databaseXmlPath).buffer().use {
            it.readUtf8()
        }
            .let {
                println("decoding $databaseXmlPath")
                xML.decodeFromString(VirtualDJDatabase.serializer(), it)
            }
    }else {
        println("database file $databaseXmlPath not found, some information may not be accurate")

        VirtualDJDatabase(songs = emptyList())
    }
//        .also {
//            println(it)
//        }

    val cueFiles = if (args.isEmpty()) {

        val folders = database.songs.mapNotNull {
            it.filePath.toPath().parent?.safeToRealPath()
        }.distinct()

        val realFolders = folders.filter {
            FileSystem.SYSTEM.exists(it)
        }

        println("folders: $folders")
        println("realFolders: $realFolders")

        realFolders
            .run {
                takeUnless { it.isEmpty() }
                    ?: listOf(
//                    ".".toPath().safeToRealPath(),
                        "..".toPath().safeToRealPath(),
                    )
            }
            .filter {
                FileSystem.SYSTEM.exists(it)
            }
            .flatMap { folder ->
                println("scanning $folder")
                try {
                    FileSystem.SYSTEM.listRecursively(folder, followSymlinks = false)
                        .filter { it.name.endsWith(".cue") }
                        .map { it.safeToRealPath() }
                        .toList()
                } catch (e: FileNotFoundException) {
                    println()
                    e.printStackTrace()
                    println()
                    emptyList()
                }
            }.distinct()
    } else {
        val paths = args.map { it.toPath() }.filter {
            FileSystem.SYSTEM.exists(it)
        }
        paths.filter { it.name.endsWith(".cue") } + paths
            .filterNot { it.name.endsWith(".cue") }
            .flatMap { folder ->
                println("scanning $folder")
                try {
                    FileSystem.SYSTEM.listRecursively(folder, followSymlinks = false)
                        .filter { it.name.endsWith(".cue") }
                        .map { it.safeToRealPath() }
                        .toList()
                } catch (e: FileNotFoundException) {
                    println()
                    e.printStackTrace()
                    println()
                    emptyList()
                }
            }.distinct()
    }

    if(cueFiles.isEmpty()) {
        println("no cue file locations oassed or found in $databaseXmlPath ")
        exitProcess(1)
    }

    println("cue files: $cueFiles")
    val cues = cueFiles.map {
        parseCue(it)
    }

    cues.forEach { cueFile ->
        println()
        println("cue file: $cueFile")
        val recording = database.songs.firstOrNull() { it.filePath.endsWith(cueFile.file) }

        println("recording: ${recording?.filePath}")
        if (recording == null) {
            println("WARN: MISSING RECORDING for ${cueFile.file}")
        }
        val date = recording
            ?.comment
            ?.substringAfter("Recorded using VirtualDJ on ")
            ?.let {
                LocalDate.parse(it, LocalDate.Formats.ISO)
            }
        val tracklist = Tracklist(
            title = date?.toString() ?: cueFile.file,
            tracks = cueFile.tracks.map { cueTrack ->
                Track(
                    position = cueTrack.trackNum,
                    time = cueTrack.timestamp,
                    title = cueTrack.title,
                    artist = cueTrack.performer
                )
            }
        )

        Template.write(
            tracklist,
            Track.serializer(),
        )

//        cueFile.tracks.map { cueTrack ->
//            val performer = cueTrack.performer
//                .substringBefore(" feat. ")
//            val feat = cueTrack.performer
//                .substringAfter(" feat. ", missingDelimiterValue = "")
//                .takeUnless { it.isEmpty() }
//            val title = cueTrack.title.substringBefore("(Remix)")
//            val remix = if(cueTrack.title.contains("(Remix)")) {
//                "(Remix)"
//            } else null
//
//            virtualDjDb.songs.firstOrNull {
//                it.tags.author == cueTrack.performer &&
//                        it.tags.title == cueTrack.title
//            }
//                ?: virtualDjDb.songs.firstOrNull {
//                    it.tags.author == cueTrack.performer &&
//                            it.tags.title + " (" + it.tags.remix + ")" == cueTrack.title
//                }
//                ?: virtualDjDb.songs.firstOrNull {
//                    it.tags.author == performer &&
//                            it.tags.title == "${cueTrack.title} (ft. $feat)"
//                }
////                ?: virtualDjDb.songs.firstOrNull {
////                    val filename = it.filePath.substringAfterLast("\\").substringBeforeLast(".")
////                    val expected = "${cueTrack.performer} - ${cueTrack.title}"
////                    println("$expected == $filename")
////                    filename == expected
////                }
//                ?: virtualDjDb.songs.firstOrNull {
//                    val filename = it.filePath
//                        .substringAfterLast("\\")
//                        .substringBeforeLast(".")
//                        .replace("""[\[\]()&]""".toRegex(), "")
//                    val expected = listOfNotNull(
//                        "$performer - ${cueTrack.title}",
//                        "(ft. $feat)".takeIf { feat != null},
//                        "$remix".takeIf { remix != null}
//                    ).joinToString(" ")
////                        .replace("&", "&amp;")
//                            .replace("""[\[\]()&]""".toRegex(), "")
//                    println("$expected == $filename")
//                    filename == expected
//                }
//                ?: run {
//                    println("performer: $performer")
//                    println("feat: $feat")
//                    println("title: $title")
//                    println("remix: $remix")
//                    error("could not find track for $cueTrack")
//                }
//        }

    }


    val tracklists = database.songs.filter {
        it.comment?.startsWith("Recorded using VirtualDJ on ") ?: false
    }.filter {
        it.filePath.toPath().name !in cues.map { it.file }
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
                SimpleTrack(
                    position = cuePoint.num ?: 0,
                    time = cuePoint.pos.seconds,
                    title = name
                )
            }

        val recordingDuration = recording.infos.songLength?.toDouble()?.seconds

//        val firstSeen = recording.infos.firstSeen.let { Instant.fromEpochSeconds(it) }
//        val lastModified = recording.infos.lastModified.let { Instant.fromEpochSeconds(it) }

//        println("recording on $date")
//        println("duration $recordingDuration")

//        tracks.forEach { (start, name) ->
//            println("${start.formatTimestamp()} $name")
//        }
        val tracklist = Tracklist(
            title = date.toString(),
            tracks = tracks,
        )
        if(tracklist.tracks.isNotEmpty()) {

            Template.write(
                tracklist,
                SimpleTrack.serializer(),
                defaultTemplate = "{time} {title}",
                templateKey = "template_simple"
            )
        } else {
            println("tracklist was empty")
        }
    }

//    println(Clock.System.now().format(sqliteDatetimeFormat))


//    runBlocking {
//        tracklistsNew.forEach { playlist ->
//            Template.write(
//                playlist,
//                Track.serializer(),
//            )
//        }
//    }


    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()

    exitProcess(0)
}
