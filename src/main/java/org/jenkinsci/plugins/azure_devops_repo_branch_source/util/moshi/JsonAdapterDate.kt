package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.DateTimeParserFormatter
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.suppressThrowable
import java.util.*

class JsonAdapterDate(private val dateTimeParserFormatter: DateTimeParserFormatter) : JsonAdapter<Date>() {
    override fun toJson(writer: JsonWriter, value: Date?) {
        writer.value(valueToJson(value))
    }

    override fun fromJson(reader: JsonReader): Date? {
        return valueFromJson(reader.nextString())
    }

    @ToJson
    fun valueToJson(instance: Date?): String? = dateTimeParserFormatter.toString(instance)

    @FromJson
    fun valueFromJson(string: String?): Date? = suppressThrowable { dateTimeParserFormatter.toDate(string) }
}