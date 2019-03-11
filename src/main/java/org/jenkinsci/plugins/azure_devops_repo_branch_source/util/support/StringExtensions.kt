package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import java.net.URLEncoder
import kotlin.reflect.full.memberProperties

private const val URL_ENCODING_CHARSET = "UTF-8"

fun String?.urlEncoded(): String? = this?.let { URLEncoder.encode(this, URL_ENCODING_CHARSET) }

fun String.getPropertyValue(instance: Any?): Any? {
    return instance?.let { notNullInstance ->
        val names = split(delimiters = *arrayOf("""."""), limit = 2)
        names.firstOrNull()?.let { propertyName ->
            suppressThrowable {
                notNullInstance.javaClass.kotlin.memberProperties.firstOrNull { it.name == propertyName }?.get(notNullInstance)
            }
        }?.let { notNullPropertyValue ->
            names.singleOrNull()?.let {
                notNullPropertyValue
            } ?: names[1].getPropertyValue(notNullPropertyValue)
        }
    }
}