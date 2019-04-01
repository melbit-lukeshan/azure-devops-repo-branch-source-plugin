package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.PolicyConfigurations

class ListPolicyConfigurationsRequest(collectionUrl: String, pat: String, val projectName: String)
    : AzureBaseRequest<PolicyConfigurations, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/policy/configurations"
}