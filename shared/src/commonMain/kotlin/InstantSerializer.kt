
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.serializers.FormattedInstantSerializer

object InstantSerializer : FormattedInstantSerializer(
    "ISO", DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
)