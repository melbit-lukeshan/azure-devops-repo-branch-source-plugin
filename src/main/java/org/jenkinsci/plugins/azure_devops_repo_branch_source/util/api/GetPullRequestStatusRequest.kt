package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitPullRequestStatus

class GetPullRequestStatusRequest(
        collectionUrl: String,
        pat: String,
        val projectName: String,
        val repositoryName: String,
        val pullRequestId: Int,
        val statusId: Int
) : AzureBaseRequest<GitPullRequestStatus, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/pullRequests/{pullRequestId}/statuses/{statusId}"
}
