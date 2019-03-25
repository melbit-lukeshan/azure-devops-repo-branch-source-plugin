package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.Projects

class ListProjectsRequest(collectionUrl: String, pat: String)
    : AzureBaseRequest<Projects, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/_apis/projects"
}
