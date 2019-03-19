package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import java.io.InputStream

class GetItemStreamRequest(collectionUrl: String, pat: String, val url: String)
    : AzureBaseRequest<InputStream, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val host = "{url}"
    override val parameters: String? = null
}