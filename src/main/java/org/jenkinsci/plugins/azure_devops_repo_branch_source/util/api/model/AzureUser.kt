package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class AzureUser(
        val id: String,
        val descriptor: String,
        val subjectDescriptor: String,
        val providerDisplayName: String,
        val isActive: Boolean,
        val properties: AzureUserProperties,
        val resourceVersion: Int,
        val metaTypeId: Int
)
