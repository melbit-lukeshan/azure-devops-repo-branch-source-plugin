package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName

/**
 * Refs have names like
 *
 * refs/heads/master
 * refs/heads/branch3
 * refs/pull/8/merge
 * refs/tags/tag111
 **/
data class GitRef(
        @SerializedName("_links") val links: ReferenceLinks?,
        val creator: IdentityRef,
        val isLocked: Boolean,
        val isLockedBy: IdentityRef,
        val name: String,
        val objectId: String,
        val peeledObjectId: String?,
        val statuses: List<GitStatus>?,
        val url: String
) {
    fun isBranch(): Boolean {
        return name.startsWith("refs/heads/")
    }

    fun isTag(): Boolean {
        return name.startsWith("refs/tags/")
    }

    fun isPullRequest(): Boolean {
        return name.startsWith("refs/pull/")
    }
}
