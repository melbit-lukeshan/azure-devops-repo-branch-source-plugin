package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import java.io.InputStream

class GetItemStreamRequest(collectionUrl: String, pat: String, val projectName: String, val repositoryName: String, val itemPath: String)
    : AzureBaseRequest<InputStream, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/items"
    override val parameters: String? = "${super.parameters}&path={itemPath}&download=true&resolveLfs=true&\$format=octetStream"
}