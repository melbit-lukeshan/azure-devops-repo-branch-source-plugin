package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

class ListRepositoriesRequest(collectionUrl: String, pat: String, val projectName: String)
    : AzureBaseRequest<Repositories, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories"
}