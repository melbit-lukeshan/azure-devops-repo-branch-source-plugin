package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.PullRequestStatus
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.PullRequests

class ListPullRequestsRequest(
        collectionUrl: String,
        pat: String,
        val projectName: String,
        val repositoryName: String,
        val pullRequestStatus: PullRequestStatus?,
        val sourceBranchRefName: String?
) : AzureBaseRequest<PullRequests, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/pullRequests"
    /**
     * sourceRefName is the full ref, for example "refs/heads/b3"
     */
    override val parameters = "${super.parameters}&status={pullRequestStatus}&sourceRefName={sourceBranchRefName}"
}
