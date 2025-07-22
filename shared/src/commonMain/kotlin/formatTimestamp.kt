import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlin.time.Duration

fun Duration.formatTimestamp(): String {
    val localTime = LocalTime.fromSecondOfDay(inWholeSeconds.toInt())
    return localTime.format(durationTimestampFormat)
}