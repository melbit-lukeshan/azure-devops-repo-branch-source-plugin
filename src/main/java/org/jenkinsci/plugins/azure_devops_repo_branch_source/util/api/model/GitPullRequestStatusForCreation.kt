package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class GitPullRequestStatusForCreation(
        val context: GitStatusContext,
        val description: String,
        val state: GitStatusState,
        val targetUrl: String
)
