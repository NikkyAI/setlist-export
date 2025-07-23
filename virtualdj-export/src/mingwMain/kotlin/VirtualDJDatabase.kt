import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import okio.Path

@Serializable
data class VirtualDJDatabase(
    @SerialName("Version")
    val version: String? = null,

    @XmlElement
    @SerialName("Song")
    val songs: List<Song>,
) {
    @Serializable
    data class Song(
        @SerialName("FilePath")
        val filePath: String,
        @SerialName("FileSize")
        val fileSize: Long? = null,
        @SerialName("Flag")
        val flag: Int? = null,
        @XmlElement
        @SerialName("Tags")
        val tags: Tags,
        @XmlElement
        @SerialName("Infos")
        val infos: Infos,
        @XmlElement
        @SerialName("Scan")
        val scan: Scan? = null,
        @XmlElement
        @SerialName("Comment")
        val comment: String? = null,
        @XmlElement
        @SerialName("Poi")
        val pois: List<Poi>
    ) {
        @Serializable
        data class Tags(
            @SerialName("Flag")
            val flag: Int? = null,
            @SerialName("Bpm")
            val bpm: Float? = null,
            @SerialName("Remix")
            val remix: String? = null,
            @SerialName("Author")
            val author: String? = null,
            @SerialName("Title")
            val title: String? = null,
            @SerialName("Album")
            val album: String? = null,
            @SerialName("Genre")
            val genre: String? = null,
            @SerialName("Label")
            val label: String? = null,
            @SerialName("Composer")
            val composer: String? = null,
            @SerialName("TrackNumber")
            val trackNumber: String? = null,
            @SerialName("Year")
            val year: String? = null,
            @SerialName("Grouping")
            val grouping: String? = null,
            @SerialName("Key")
            val key: String? = null,
            @SerialName("Remixer")
            val remixer: String? = null,
        )

        @Serializable
        data class Infos(
            @SerialName("SongLength")
            val songLength: Float? = null,
            @SerialName("LastModified")
            val lastModified: Long,
            @SerialName("FirstSeen")
            val firstSeen: Long,
            @SerialName("FirstPlay")
            val firstPlay: Long? = null,
            @SerialName("LastPlay")
            val lastPlay: Long? = null,
            @SerialName("PlayCount")
            val playCount: Int = 0,
            @SerialName("Cover")
            val cover: String? = null,
            @SerialName("Bitrate")
            val bitrate: Int? = null,
            @SerialName("Gain")
            val gain: Float? = null,
        )

        @Serializable
        data class Scan(
            @SerialName("Version")
            val version: String? = null,
            @SerialName("Bpm")
            val bpm: Float? = null,
            @SerialName("AltBpm")
            val altBpm: Float? = null,
            @SerialName("Volume")
            val volume: Float? = null,
            @SerialName("Key")
            val key: String? = null,
            @SerialName("Flag")
            val flag: String? = null,
        )

        @Serializable
        data class Poi(
            @SerialName("Name")
            val name: String? = null,
            @SerialName("Type")
            val type: String? = null,
            @SerialName("Pos")
            val pos: Double = 0.0,
            @SerialName("Num")
            val num: Int? = null,
            @SerialName("Point")
            val point: String? = null,
            @SerialName("Color")
            val color: String? = null,
            // type: loop
            @SerialName("Size")
            val size: Float? = null,
            @SerialName("Seconds")
            val seconds: Float? = null,
            @SerialName("Slot")
            val slot: Int? = null,
            @SerialName("AutoTrigger")
            val autoTrigger: String? = null,
            //type: beatgrid
            @SerialName("Bpm")
            val bpm: Float? = null,
        )
    }
}