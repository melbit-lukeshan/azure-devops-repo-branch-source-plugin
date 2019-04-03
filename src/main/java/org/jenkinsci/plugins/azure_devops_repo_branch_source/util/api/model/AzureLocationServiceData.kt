package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class AzureLocationServiceData(
        val serviceOwner: String,
        val defaultAccessMappingMoniker: String,
        val lastChangeId: Int,
        val lastChangeId64: Int
)
