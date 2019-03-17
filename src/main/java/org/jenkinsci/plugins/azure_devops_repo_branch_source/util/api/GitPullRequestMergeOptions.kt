package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

data class GitPullRequestMergeOptions(
        val detectRenameFalsePositives: Boolean,
        val disableRenames: Boolean
)
