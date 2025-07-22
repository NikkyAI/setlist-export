import kotlin.time.Duration
import kotlin.time.Instant

data class Song(
    val position: Int,
    val timestamp: Instant,
    val duration: Duration?,
    val title: String,
    val artist: String?,
    val album: String?,
    val year: String?,
    val genre: String?,
    val bpm: Float?,
    val key: String?,
)