package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.Refs

class ListRefsRequest(collectionUrl: String, pat: String, val projectName: String, val repository: String, val filter: String)
    : AzureBaseRequest<Refs, Any>(collectionUrl, pat) {
    override val method = Method.GET
    override val path = "/{projectName}/_apis/git/repositories/{repository}/refs"
    /**
     * Refs have names like
     * refs/heads/master
     * refs/heads/branch3
     * refs/pull/8/merge
     * refs/tags/tag111
     *
     * To retrieve refs with certain type and/or name, use {filter}
     *
     * For example,
     * set filter to "heads/" will only return all BRANCH refs
     * set filter to "tags/tag111" will only return TAG refs whose names starting with "refs/tags/tag111"
     * set filter to EMPTY string will return all refs
     */
    override val parameters = "${super.parameters}&filter={filter}"
}