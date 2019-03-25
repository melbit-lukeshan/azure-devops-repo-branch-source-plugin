package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitCommit

class GetCommitRequest(collectionUrl: String, pat: String, val projectName: String, val repositoryName: String, val commitId: String)
    : AzureBaseRequest<GitCommit, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/commits/{commitId}"
}