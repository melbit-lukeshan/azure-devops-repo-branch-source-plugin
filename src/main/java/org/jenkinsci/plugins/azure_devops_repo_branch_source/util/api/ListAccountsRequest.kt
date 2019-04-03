package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.Accounts

class ListAccountsRequest(pat: String, val ownerId: String)
    : AzureBaseRequest<Accounts, Any>("https://app.vssps.visualstudio.com", pat) {
    override val method = Method.GET
    override val path = "/_apis/accounts"
    override val parameters = "${super.parameters}&ownerId={ownerId}"
}
