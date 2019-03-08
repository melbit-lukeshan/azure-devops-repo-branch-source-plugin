package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.DateTimeParserFormatter
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.suppressThrowable
import org.threeten.bp.LocalTime

class JsonAdapterLocalTime(private val dateTimeParserFormatter: DateTimeParserFormatter) : JsonAdapter<LocalTime>() {

    override fun toJson(writer: JsonWriter, value: LocalTime?) {
        writer.value(valueToJson(value))
    }

    override fun fromJson(reader: JsonReader): LocalTime? =
            valueFromJson(reader.nextString())

    @ToJson
    fun valueToJson(instance: LocalTime?): String? =
            dateTimeParserFormatter.toString(instance)

    @FromJson
    fun valueFromJson(string: String?): LocalTime? =
            suppressThrowable { dateTimeParserFormatter.toLocalTime(string) }
}