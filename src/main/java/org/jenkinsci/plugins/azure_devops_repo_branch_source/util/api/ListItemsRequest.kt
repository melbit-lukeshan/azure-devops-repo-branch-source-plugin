package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

class ListItemsRequest(collectionUrl: String, pat: String, val projectName: String, val repositoryName: String, val scopePath: String)
    : AzureBaseRequest<Items, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/items"
    override val parameters = "${super.parameters}&scopePath={scopePath}"
}