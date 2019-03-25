package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

enum class PullRequestAsyncStatus {
    conflicts,
    failure,
    notSet,
    queued,
    rejectedByPolicy,
    succeeded
}
