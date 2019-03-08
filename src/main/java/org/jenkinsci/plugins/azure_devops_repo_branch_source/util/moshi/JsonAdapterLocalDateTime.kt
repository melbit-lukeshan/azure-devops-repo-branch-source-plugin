package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.DateTimeParserFormatter
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.suppressThrowable
import org.threeten.bp.LocalDateTime

class JsonAdapterLocalDateTime(private val dateTimeParserFormatter: DateTimeParserFormatter) : JsonAdapter<LocalDateTime>() {

    override fun toJson(writer: JsonWriter, value: LocalDateTime?) {
        writer.value(valueToJson(value))
    }

    override fun fromJson(reader: JsonReader): LocalDateTime? =
            valueFromJson(reader.nextString())

    @ToJson
    fun valueToJson(instance: LocalDateTime?): String? =
            dateTimeParserFormatter.toString(instance)

    @FromJson
    fun valueFromJson(string: String?): LocalDateTime? =
            suppressThrowable { dateTimeParserFormatter.toLocalDateTime(string) }
}