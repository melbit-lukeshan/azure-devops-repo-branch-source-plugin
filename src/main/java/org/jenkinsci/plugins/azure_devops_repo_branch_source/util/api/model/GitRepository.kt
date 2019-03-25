package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import com.google.gson.annotations.SerializedName

data class GitRepository(
        @SerializedName("_links") val links: ReferenceLinks,
        val defaultBranch: String,
        val id: String,
        val isFork: Boolean,
        val name: String,
        val parentRepository: GitRepositoryRef?,
        val project: TeamProjectReference,
        val remoteUrl: String,
        val size: Int,
        val sshUrl: String?,
        val url: String,
        val validRemoteUrls: List<String>
)
