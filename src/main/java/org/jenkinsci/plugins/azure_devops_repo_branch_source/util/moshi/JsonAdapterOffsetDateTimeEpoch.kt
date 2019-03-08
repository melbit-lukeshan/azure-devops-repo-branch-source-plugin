package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.DateTimeParserFormatter
import org.threeten.bp.OffsetDateTime

class JsonAdapterOffsetDateTimeEpoch(private val epochType: EpochType) : JsonAdapter<OffsetDateTime>() {

    override fun toJson(writer: JsonWriter, value: OffsetDateTime?) {
        writer.value(valueToJson(value))
    }

    override fun fromJson(reader: JsonReader): OffsetDateTime? {
        return valueFromJson(reader.nextLong())
    }

    private val dateTimeParserFormatter by lazy {
        DateTimeParserFormatter(null)
    }

    @ToJson
    fun valueToJson(instance: OffsetDateTime?): Long? = dateTimeParserFormatter.toEpochMilli(instance)?.div(epochType.factor)

    @FromJson
    fun valueFromJson(epochMilli: Long?): OffsetDateTime? = dateTimeParserFormatter.toOffsetDateTime(epochMilli?.times(epochType.factor))
}