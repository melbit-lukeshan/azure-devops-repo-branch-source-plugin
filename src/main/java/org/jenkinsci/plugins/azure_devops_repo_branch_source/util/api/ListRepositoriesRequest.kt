package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson.GsonProcessor
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Request
import java.nio.charset.Charset
import java.util.*

class ListRepositoriesRequest(val pat: String, val organization: String, val project: String, _category: String? = null, _id: String? = null)
    : Request<Repositories, Any>(_category, _id) {
    val authorizationHeader: String by lazy {
        "Basic " + Base64.getEncoder().withoutPadding().encodeToString(pat.toByteArray(Charset.forName("UTF-8")))
    }
    override val jsonProcessor = GsonProcessor
    override val method = Method.GET
    override val host = "https://dev.azure.com"
    override val endpoint = "/{organization}/{project}"
    override val path = "/_apis/git/repositories"
    override val headers = "Authorization={authorizationHeader}"
    override val parameters = "api-version=5.0"
}