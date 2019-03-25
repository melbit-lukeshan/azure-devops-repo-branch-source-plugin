package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.Commits

class ListCommitsRequest(collectionUrl: String, pat: String, val projectName: String, val repository: String)
    : AzureBaseRequest<Commits, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repository}/commits"
}