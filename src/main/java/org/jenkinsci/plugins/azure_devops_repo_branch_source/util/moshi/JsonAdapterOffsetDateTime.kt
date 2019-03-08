package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.DateTimeParserFormatter
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.suppressThrowable
import org.threeten.bp.OffsetDateTime

class JsonAdapterOffsetDateTime(private val dateTimeParserFormatter: DateTimeParserFormatter) : JsonAdapter<OffsetDateTime>() {
    override fun toJson(writer: JsonWriter, value: OffsetDateTime?) {
        writer.value(valueToJson(value))
    }

    override fun fromJson(reader: JsonReader): OffsetDateTime? {
        return valueFromJson(reader.nextString())
    }

    @ToJson
    fun valueToJson(instance: OffsetDateTime?): String? = dateTimeParserFormatter.toString(instance)

    @FromJson
    fun valueFromJson(string: String?): OffsetDateTime? = suppressThrowable { dateTimeParserFormatter.toOffsetDateTime(string) }
}