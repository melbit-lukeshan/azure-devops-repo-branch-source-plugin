package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class Repositories(
        val count: Int,
        val value: List<GitRepository>
)