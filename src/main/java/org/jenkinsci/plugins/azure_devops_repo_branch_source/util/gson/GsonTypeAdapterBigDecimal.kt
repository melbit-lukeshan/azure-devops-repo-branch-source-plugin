package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson

import com.google.gson.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.suppressThrowable
import java.lang.reflect.Type
import java.math.BigDecimal
import kotlin.reflect.KClass

class GsonTypeAdapterBigDecimal : GsonTypeAdapter, JsonDeserializer<BigDecimal>, JsonSerializer<BigDecimal> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BigDecimal? =
            suppressThrowable(null) {
                BigDecimal(json.asString.replace("[a-zA-Z]+".toRegex(), ""))
            }

    override fun serialize(src: BigDecimal, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
            JsonPrimitive(src.toString())

    override fun getTargetClass(): KClass<*> {
        return BigDecimal::class
    }
}