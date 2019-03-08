package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.LogUtil
import java.io.IOException
import java.lang.reflect.Type

class DefaultOnDataMismatchAdapter<T> private constructor(private val delegate: JsonAdapter<T>, private val defaultValue: T?) :
        JsonAdapter<T>() {

    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): T? =
            try {
                delegate.fromJsonValue(reader.readJsonValue())
            } catch (e: Exception) {
                LogUtil.logThrowable(e)
                defaultValue
            }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: T?) {
        delegate.toJson(writer, value)
    }

    companion object {
        @JvmStatic
        fun <T> newFactory(type: Class<T>, defaultValue: T?): JsonAdapter.Factory {
            return object : JsonAdapter.Factory {
                override fun create(requestedType: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
                    if (type != requestedType) {
                        return null
                    }
                    val delegate = moshi.nextAdapter<T>(this, type, annotations)
                    return DefaultOnDataMismatchAdapter(delegate, defaultValue)
                }
            }
        }

        @JvmStatic
        fun newEnumFallbackNullFactory(): JsonAdapter.Factory {
            return object : JsonAdapter.Factory {
                override fun create(requestedType: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
                    val rawType = Types.getRawType(requestedType)
                    if (rawType.isEnum) {
                        val delegate = moshi.nextAdapter<Enum<*>>(this, requestedType, annotations)
                        return DefaultOnDataMismatchAdapter(delegate, null)
                    }
                    return null
                }
            }
        }
    }
}