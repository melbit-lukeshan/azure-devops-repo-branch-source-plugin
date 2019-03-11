package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson

import com.emmacanhelp.androidbase.json.gson.GsonTypeAdapterOffsetDateTime
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.JsonProcessor
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

object GsonProcessor : JsonProcessor {
    private val gson: Gson by lazy {
        GsonBuilder()
                .setLenient()
                .registerTypeAdapter(BigDecimal::class.java, GsonTypeAdapterBigDecimal())
                .registerTypeAdapter(Date::class.java, GsonTypeAdapterDate())
                .registerTypeAdapter(OffsetDateTime::class.java, GsonTypeAdapterOffsetDateTime())
                .registerTypeAdapter(LocalDate::class.java, GsonTypeAdapterLocalDate())
                .registerTypeAdapterFactory(SuperGsonEnumTypeAdapter.SuperGsonEnumTypeAdapterFactory)
                .create()
    }

    override fun <T : Any> instanceFromJson(json: String?, clazz: KClass<T>): T? =
            if (clazz == Any::class || clazz == String::class) {
                clazz.cast(json)
            } else {
                gson.fromJson(json, clazz.java)
            }

    override fun <T : Any> instanceToJson(instance: T?): String? =
            gson.toJson(instance)
}