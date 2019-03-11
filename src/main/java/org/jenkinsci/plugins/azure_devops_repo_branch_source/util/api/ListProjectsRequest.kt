package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson.GsonProcessor
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Request

class ListProjectsRequest(private val pat: String, val organization: String)
    : Request<Projects, Any>() {
    val authorizationHeader: String by lazy {
        //"Basic " + Base64.getEncoder().withoutPadding().encodeToString((":$pat").toByteArray(Charset.forName("UTF-8")))
        "Basic Onp1czNjcnR3bHRvNmFzbnNoc3VoaHJhcWhxbWtpZ3V6bjVvY3hpcnAzM3B1cnVrank2ZXE="
    }
    override val jsonProcessor = GsonProcessor
    override val method = Method.GET
    override val host = "https://dev.azure.com"
    override val endpoint = "/{organization}"
    override val path = "/_apis/projects"
    override val parameters = "api-version=5.0"
    override val headers = "Authorization=Basic Onp1czNjcnR3bHRvNmFzbnNoc3VoaHJhcWhxbWtpZ3V6bjVvY3hpcnAzM3B1cnVrank2ZXE="
}