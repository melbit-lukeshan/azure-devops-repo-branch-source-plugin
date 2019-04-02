package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class GitRefUpdate(
        val name: String,
        val newObjectId: String,
        val oldObjectId: String,
        val repositoryId: String
)