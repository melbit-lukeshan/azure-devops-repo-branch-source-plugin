package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson

import com.google.gson.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Constants
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.LogUtil
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

/**
 * Pass the desired format to parse-from and format-to
 * Pass [null] to parse-from/format-to epoch milliseconds
 */
class GsonTypeAdapterDate(deserializerFormat: String? = Constants.DATE_TIME_FORMAT_ISO8601_ANDROID,
                          serializerFormat: String? = Constants.DATE_TIME_FORMAT_ISO8601_ANDROID)
    : GsonTypeAdapter, JsonDeserializer<Date>, JsonSerializer<Date> {

    private val dateFormatForDeserializer: DateFormat? by lazy {
        deserializerFormat?.let {
            SimpleDateFormat(it, Locale.US)
        }
    }

    private val dateFormatForSerializer: DateFormat? by lazy {
        serializerFormat?.let {
            SimpleDateFormat(it, Locale.US)
        }
    }

    private val additionalDateFormatForDeserializerSet by lazy {
        mutableListOf<DateFormat>()
    }

    fun addAdditionalDateFormatForDeserializer(additionalDateFormatForDeserializer: String): GsonTypeAdapterDate {
        additionalDateFormatForDeserializerSet.add(SimpleDateFormat(additionalDateFormatForDeserializer, Locale.US))
        return this
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date? {
        return try {
            dateFormatForDeserializer?.parse(json.asString) ?: Date(json.asLong)
        } catch (e: Exception) {
            additionalDateFormatForDeserializerSet.forEach {
                try {
                    it.parse(json.asString)
                } catch (ex: Exception) {
                }
            }
            LogUtil.logThrowable(e)
            null
        }
    }

    override fun serialize(src: Date, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return dateFormatForSerializer?.let {
            JsonPrimitive(it.format(src))
        } ?: JsonPrimitive(src.time)
    }

    override fun getTargetClass(): KClass<*> {
        return Date::class
    }
}