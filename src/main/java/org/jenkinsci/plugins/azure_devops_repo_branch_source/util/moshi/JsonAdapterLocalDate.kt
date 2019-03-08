package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.DateTimeParserFormatter
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.suppressThrowable
import org.threeten.bp.LocalDate

class JsonAdapterLocalDate(private val dateTimeParserFormatter: DateTimeParserFormatter) : JsonAdapter<LocalDate>() {

    override fun toJson(writer: JsonWriter, value: LocalDate?) {
        writer.value(valueToJson(value))
    }

    override fun fromJson(reader: JsonReader): LocalDate? =
            valueFromJson(reader.nextString())

    @ToJson
    fun valueToJson(instance: LocalDate?): String? =
            dateTimeParserFormatter.toString(instance)

    @FromJson
    fun valueFromJson(string: String?): LocalDate? =
            suppressThrowable { dateTimeParserFormatter.toLocalDate(string) }
}