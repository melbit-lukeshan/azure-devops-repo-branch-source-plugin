package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitStatus
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitStatusForCreation

class CreateCommitStatusRequest(collectionUrl: String, pat: String, val projectName: String, val repositoryName: String, val commitId: String, val status: GitStatusForCreation)
    : AzureBaseRequest<GitStatus, Any>(collectionUrl, pat) {
    override val method = Method.POST
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/commits/{commitId}/statuses"
    override val body = "{status}"
}