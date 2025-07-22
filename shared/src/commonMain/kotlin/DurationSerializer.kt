import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.optional
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(BetaInteropApi::class)
val durationTimestampFormat = LocalTime.Format {
//    optional {
        hour(Padding.ZERO)
        char(':')
//    }
    minute(Padding.ZERO)
    char(':')
    second(Padding.ZERO)
}

object DurationSerializer : KSerializer<Duration> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Duration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) {
        val localTime = LocalTime.fromSecondOfDay(value.inWholeSeconds.toInt())
        encoder.encodeString(localTime.format(durationTimestampFormat))
    }

    override fun deserialize(decoder: Decoder): Duration {
        val localTime = LocalTime.parse(decoder.decodeString(), durationTimestampFormat)
        return localTime.toMillisecondOfDay().milliseconds
    }
}