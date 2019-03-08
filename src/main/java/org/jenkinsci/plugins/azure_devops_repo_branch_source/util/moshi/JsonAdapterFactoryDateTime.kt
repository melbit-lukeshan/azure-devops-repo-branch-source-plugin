package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.DateTimeParserFormatter
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import java.lang.reflect.Type
import java.util.*

class JsonAdapterFactoryDateTime : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? =
            (annotations.find {
                it is JsonDateTime
            } as? JsonDateTime)?.let { jsonDateTime ->
                if (EpochType.values().map { it.name }.contains(jsonDateTime.value)) {
                    when (type) {
                        Date::class.java -> JsonAdapterDateEpoch(EpochType.valueOf(jsonDateTime.value))
                        OffsetDateTime::class.java -> JsonAdapterOffsetDateTimeEpoch(EpochType.valueOf(jsonDateTime.value))
                        else -> null
                    }
                } else {
                    when (type) {
                        Date::class.java -> JsonAdapterDate(DateTimeParserFormatter(jsonDateTime.value))
                        OffsetDateTime::class.java -> JsonAdapterOffsetDateTime(DateTimeParserFormatter(jsonDateTime.value))
                        LocalDateTime::class.java -> JsonAdapterLocalDateTime(DateTimeParserFormatter(jsonDateTime.value))
                        LocalDate::class.java -> JsonAdapterLocalDate(DateTimeParserFormatter(jsonDateTime.value))
                        LocalTime::class.java -> JsonAdapterLocalTime(DateTimeParserFormatter(jsonDateTime.value))
                        else -> null
                    }
                }
            }
}