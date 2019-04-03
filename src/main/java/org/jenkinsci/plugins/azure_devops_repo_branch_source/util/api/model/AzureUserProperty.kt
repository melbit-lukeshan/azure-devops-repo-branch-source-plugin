package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import com.google.gson.annotations.SerializedName

data class AzureUserProperty(
        @SerializedName("\$type") val type: String,
        @SerializedName("\$value") val value: String
)
