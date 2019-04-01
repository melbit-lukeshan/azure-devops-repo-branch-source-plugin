package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class PolicyScope(
        val refName: String,
        val matchKind: String,
        val repositoryId: String
)
