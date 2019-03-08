package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.moshi

import com.squareup.moshi.JsonQualifier
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.Constants

const val EPOCH_TYPE_MILLI_SECONDS = "MILLI_SECONDS"
const val EPOCH_TYPE_SECONDS = "SECONDS"

enum class EpochType(val factor: Long) {
    MILLI_SECONDS(1L), SECONDS(1000L)
}

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class JsonDateTime(val value: String = Constants.DATE_TIME_FORMAT_ISO8601_ANDROID)