package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.DateTimeParserFormatter
import java.util.*

class JsonAdapterDateEpoch(private val epochType: EpochType) : JsonAdapter<Date>() {

    override fun toJson(writer: JsonWriter, value: Date?) {
        writer.value(valueToJson(value))
    }

    override fun fromJson(reader: JsonReader): Date? {
        return valueFromJson(reader.nextLong())
    }

    private val dateTimeParserFormatter by lazy {
        DateTimeParserFormatter(null)
    }

    @ToJson
    fun valueToJson(instance: Date?): Long? = dateTimeParserFormatter.toEpochMilli(instance)?.div(epochType.factor)

    @FromJson
    fun valueFromJson(epochMilli: Long?): Date? = dateTimeParserFormatter.toDate(epochMilli?.times(epochType.factor))
}