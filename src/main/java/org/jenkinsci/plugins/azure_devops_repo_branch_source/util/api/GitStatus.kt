package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName
import java.util.*

data class GitStatus(
        @SerializedName("_links") val links: ReferenceLinks,
        val context: GitStatusContext,
        val createdBy: IdentityRef,
        val creationDate: String,
        val description: String,
        val id: Int,
        val state: GitStatusState,
        val targetUrl: String,
        val updatedDate: Date
)
