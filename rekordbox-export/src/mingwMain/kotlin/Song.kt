import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class Song(
    val position: Int,
    @Serializable(with=DurationSerializer::class)
    val time: Duration,
    @Serializable(with=InstantSerializer::class)
    val timestamp: Instant,
    @Serializable(with=DurationSerializer::class)
    val duration: Duration?,
    val title: String,
    val artist: String?,
    val label: String?,
    val album: String?,
    val genre: String?,
    val year: Int?,
    val bpm: Float,
    val scale: String?,
)