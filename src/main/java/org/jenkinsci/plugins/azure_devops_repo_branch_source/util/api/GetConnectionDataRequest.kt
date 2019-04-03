package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.ConnectionData

class GetConnectionDataRequest(pat: String)
    : AzureBaseRequest<ConnectionData, Any>("https://app.vssps.visualstudio.com", pat) {
    override val method = Method.GET
    override val path = "/_apis/ConnectionData"
    override val parameters: String = "api-version=5.0-preview.1"
}
