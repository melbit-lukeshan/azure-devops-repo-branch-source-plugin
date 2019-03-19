package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson.GsonProcessor
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Request
import java.nio.charset.Charset
import java.util.*

abstract class AzureBaseRequest<T, R>(val collectionUrl: String, private val pat: String) : Request<T, R>() {

    val authorizationHeader: String by lazy {
        "Basic " + Base64.getEncoder().encodeToString((":$pat").toByteArray(Charset.forName("UTF-8")))
    }

    override val jsonProcessor = GsonProcessor
    override val host = "{collectionUrl}"
    override val headers = "Authorization={authorizationHeader}"
    override val parameters: String? = "api-version=5.0"
    override val retryIfConnectionFail = true
    override val timeout = 30
}