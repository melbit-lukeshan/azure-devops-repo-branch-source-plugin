package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

data class GitStatusForCreation(
        val state: GitStatusState,
        val description: String,
        val targetUrl: String,
        val context: GitStatusContext
)
