package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class GitPullRequestStatus(
        @SerializedName("_links") val links: ReferenceLinks,
        val context: GitStatusContext,
        val author: GitUserDate,
        val createBy: IdentityRef,
        val creationDate: Date,
        val description: String,
        val id: Int,
        val iterationId: Int,
        val properties: PropertiesCollection,
        val state: GitStatusState,
        val targetUrl: String,
        val updatedDate: Date
)
