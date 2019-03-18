package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

class ListCommitsRequest(collectionUrl: String, pat: String, val projectName: String, val repository: String)
    : AzureBaseRequest<Commits, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repository}/commits"
}