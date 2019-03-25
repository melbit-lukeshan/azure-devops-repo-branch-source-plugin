package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class GitRepositoryRef(
        val collection: TeamProjectCollectionReference,
        val id: String,
        val isFork: Boolean,
        val name: String,
        val project: TeamProjectReference,
        val remoteUrl: String,
        val sshUrl: String,
        val url: String
)