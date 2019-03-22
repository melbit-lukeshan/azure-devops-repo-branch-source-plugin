package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

class GetItemRequest(collectionUrl: String, pat: String, val projectName: String, val repositoryName: String, val itemPath: String, val version: String, val versionType: GitVersionType)
    : AzureBaseRequest<GitItem, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/items"
    override val parameters = "${super.parameters}&path={itemPath}&version={version}&versionType={versionType}"
}