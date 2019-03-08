package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.JsonProcessor
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

class MoshiProcessor(vararg customAdapters: Any) : JsonProcessor {

    private val moshi: Moshi by lazy {
        Moshi.Builder().apply {
            customAdapters.forEach { add(it) }  // Custom adapters always go first
            add(JsonAdapterFactoryDateTime())
            add(JsonAdapterBigDecimal())
            add(DefaultOnDataMismatchAdapter.newEnumFallbackNullFactory())
            add(KotlinJsonAdapterFactory()) // Kotlin adapter factory should always be added at last
        }.build()
    }

    override fun <T : Any> instanceFromJson(json: String?, clazz: KClass<T>): T? =
            if (clazz == Any::class || clazz == String::class) {
                clazz.cast(json)
            } else {
                json?.let {
                    moshi.adapter(clazz.java).fromJson(it)
                }
            }

    override fun <T : Any> instanceToJson(instance: T?): String? =
            instance?.let {
                moshi.adapter(it.javaClass).toJson(instance)
            }
}