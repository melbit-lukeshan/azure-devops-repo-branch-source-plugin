package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import com.cloudbees.plugins.credentials.common.StandardCredentials

data class GitRepositoryWithAzureContext(
        val gitRepository: GitRepository,
        val collectionUrl: String,
        val credentials: StandardCredentials,
        val projectName: String,
        val repositoryName: String
)
