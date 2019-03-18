package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName

data class GitItem(
        @SerializedName("_links") val links: ReferenceLinks?,
        val commitId: String,
        val content: String,
        val contentMetadata: FileContentMetadata?,
        val gitObjectType: GitObjectType,
        val isFolder: Boolean,
        val isSymLink: Boolean,
        val latestProcessedChange: GitCommitRef?,
        val objectId: String,
        val originalObjectId: String,
        val path: String,
        val url: String
)
