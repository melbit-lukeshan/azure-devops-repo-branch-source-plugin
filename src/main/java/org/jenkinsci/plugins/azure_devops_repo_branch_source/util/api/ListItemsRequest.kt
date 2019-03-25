package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitVersionType
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.Items
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.VersionControlRecursionType

class ListItemsRequest(collectionUrl: String, pat: String, val projectName: String, val repositoryName: String, val scopePath: String, val version: String, val versionType: GitVersionType, val recursionLevel: VersionControlRecursionType = VersionControlRecursionType.none)
    : AzureBaseRequest<Items, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repositoryName}/items"
    override val parameters = "${super.parameters}&scopePath={scopePath}&version={version}&versionType={versionType}&recursionLevel={recursionLevel}"
}