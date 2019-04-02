package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class GitPush(
        var gitPushRef: GitPushRef,
        var commits: List<GitCommitRef>?,
        var refUpdates: List<GitRefUpdate>?,
        var repository: GitRepository?
)