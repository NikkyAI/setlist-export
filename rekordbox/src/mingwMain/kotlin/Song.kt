import kotlin.time.Duration
import kotlin.time.Instant

data class Song(
    val trackNo: Int,
    val start: Instant,
    val length: Duration,
    val songName: String,
    val artist: String?,
    val label: String?,
    val album: String?,
    val genre: String?,
    val releaseYear: Int?,
    val bpm: Float,
    val scale: String?,
)