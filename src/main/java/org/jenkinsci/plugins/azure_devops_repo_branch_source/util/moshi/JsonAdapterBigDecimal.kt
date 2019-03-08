package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.*
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.suppressThrowable
import java.math.BigDecimal

class JsonAdapterBigDecimal : JsonAdapter<BigDecimal>() {

    override fun toJson(writer: JsonWriter, value: BigDecimal?) {
        writer.value(valueToJson(value))
    }

    override fun fromJson(reader: JsonReader): BigDecimal? {
        return valueFromJson(reader.nextString())
    }

    @ToJson
    fun valueToJson(instance: BigDecimal?): String? = instance?.toString()

    @FromJson
    fun valueFromJson(string: String?): BigDecimal? = suppressThrowable { BigDecimal(string) }
}