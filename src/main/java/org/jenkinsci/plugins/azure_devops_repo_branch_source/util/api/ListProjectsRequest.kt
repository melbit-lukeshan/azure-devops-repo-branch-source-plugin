package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson.GsonProcessor
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Request
import java.nio.charset.Charset
import java.util.*

class ListProjectsRequest(val collectionUrl: String, private val pat: String)
    : Request<Projects, Any>() {
    val authorizationHeader: String by lazy {
        "Basic " + Base64.getEncoder().encodeToString((":$pat").toByteArray(Charset.forName("UTF-8")))
    }
    override val jsonProcessor = GsonProcessor
    override val method = Method.GET
    override val host = "{collectionUrl}"
    override val path = "/_apis/projects"
    override val parameters = "api-version=5.0"
    override val headers = "Authorization={authorizationHeader}"
}
