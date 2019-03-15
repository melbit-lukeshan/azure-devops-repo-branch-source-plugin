package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName

data class IdentityRef(
        @SerializedName("_links") val links: ReferenceLinks,
        val descriptor: String,
        val displayName: String,
        val id: String,
        val inactive: Boolean,
        val uniqueName: String,
        val url: String
)