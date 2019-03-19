package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson

import com.google.gson.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Constants
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.LogUtil
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass

/**
 * Pass the desired format to parse-from and format-to
 * Pass null to parse-from/format-to epoch milliseconds
 */
class GsonTypeAdapterLocalDate(deserializerFormat: String? = Constants.LOCAL_DATE_FORMAT,
                               serializerFormat: String? = Constants.LOCAL_DATE_FORMAT)
    : GsonTypeAdapter, JsonDeserializer<LocalDate>, JsonSerializer<LocalDate> {

    private val formatterForDeserializer: DateTimeFormatter? by lazy {
        deserializerFormat?.let {
            DateTimeFormatter.ofPattern(it, Locale.US)
        }
    }
    private val formatterForSerializer: DateTimeFormatter? by lazy {
        serializerFormat?.let {
            DateTimeFormatter.ofPattern(it, Locale.US)
        }
    }
    private val additionalDateFormattersForDeserializerSet by lazy {
        mutableListOf<DateTimeFormatter>()
    }

    fun addAdditionalDateFormatForDeserializer(additionalDateFormatForDeserialize: String): GsonTypeAdapterLocalDate {
        additionalDateFormattersForDeserializerSet.add(DateTimeFormatter.ofPattern(additionalDateFormatForDeserialize, Locale.US))
        return this
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate? =
            try {
                formatterForDeserializer?.parse(json.asString, LocalDate::from)
                        ?: LocalDate.ofEpochDay(json.asLong)
            } catch (e: Exception) {
                additionalDateFormattersForDeserializerSet.forEach {
                    try {
                        it.parse(json.asString, LocalDate::from)
                    } catch (ex: Exception) {
                    }
                }
                LogUtil.logThrowable(e)
                null
            }

    override fun serialize(src: LocalDate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
            formatterForSerializer?.let {
                JsonPrimitive(it.format(src))
            } ?: JsonPrimitive(src.toEpochDay())

    override fun getTargetClass(): KClass<*> {
        return LocalDate::class
    }
}