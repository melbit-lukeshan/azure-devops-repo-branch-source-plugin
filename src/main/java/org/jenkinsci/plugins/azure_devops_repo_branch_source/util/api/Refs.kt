package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

data class Refs(
        val count: Int,
        val value: List<GitRef>
)
