package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

data class GitRepository(
        val id: String,
        val name: String,
        val url: String,
        val remoteUrl: String,
        val defaultBranch: String,
        val project: TeamProjectReference
)
