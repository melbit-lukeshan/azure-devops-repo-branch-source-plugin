package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

data class GitPullRequestCompletionOptions(
        val bypassPolicy: Boolean,
        val bypassReason: String,
        val deleteSourceBranch: Boolean,
        val mergeCommitMessage: String,
        val squashMerge: Boolean,
        val transitionWorkItems: Boolean,
        val triggeredByAutoComplete: Boolean
)
