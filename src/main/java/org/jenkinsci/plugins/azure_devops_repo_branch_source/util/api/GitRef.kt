package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName

data class GitRef(
        @SerializedName("_links") val links: ReferenceLinks,
        val creator: IdentityRef,
        val isLocked: Boolean,
        val isLockedBy: IdentityRef,
        val name: String,
        val objectId: String,
        val peeledObjectId: String,
        val statuses: List<GitStatus>?,
        val url: String
)
