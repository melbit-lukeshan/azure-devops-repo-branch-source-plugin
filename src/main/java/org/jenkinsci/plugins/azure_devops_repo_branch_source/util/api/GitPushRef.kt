package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName
import java.util.*

data class GitPushRef(
        @SerializedName("_links") val links: ReferenceLinks,
        val date: Date,
        val pushId: Int,
        val pushedBy: IdentityRef,
        val url: String
)
