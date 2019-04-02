package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import com.google.gson.annotations.SerializedName
import java.time.OffsetDateTime

data class GitPush(
        @SerializedName("_links") val links: ReferenceLinks,
        val date: OffsetDateTime,
        val pushId: Int,
        val pushedBy: IdentityRef,
        var commits: List<GitCommitRef>?,
        var refUpdates: List<GitRefUpdate>?,
        var repository: GitRepository?
)