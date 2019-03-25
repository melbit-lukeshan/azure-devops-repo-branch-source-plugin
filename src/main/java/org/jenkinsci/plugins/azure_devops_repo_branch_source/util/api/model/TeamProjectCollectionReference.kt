package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class TeamProjectCollectionReference(
        val id: String,
        val name: String,
        val url: String
)