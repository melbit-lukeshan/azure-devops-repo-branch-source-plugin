package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

class CreateCommitStatusRequest(collectionUrl: String, pat: String, val projectName: String, val repositoryName: String, val commitId: String, val status: GitStatusForCreation)
    : AzureBaseRequest<GitStatus, Any>(collectionUrl, pat) {
    override val method = Method.POST
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/commits/{commitId}/statuses"
    override val body = "{status}"
}