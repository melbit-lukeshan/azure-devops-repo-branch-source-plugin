package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class ConnectionData(
        val authenticatedUser: AzureUser,
        val authorizedUser: AzureUser,
        val instanceId: String,
        val deploymentId: String,
        val deploymentType: String,
        val locationServiceData: AzureLocationServiceData
)
