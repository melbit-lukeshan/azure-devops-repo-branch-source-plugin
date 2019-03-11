package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson.GsonProcessor
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.JsonProcessor
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Request


abstract class AzureBaseRequest<T, R> : Request<T, R>() {

    private companion object {
        val sharedJsonProcessor: JsonProcessor = GsonProcessor
    }

    override val jsonProcessor = sharedJsonProcessor
    override val host = "https://dev.azure.com"
    override val headers = "Authorization=Basic BASE64PATSTRING"
    override val parameters = "api-version=5.0"
    override val retryIfConnectionFail = true
    override val timeout = 30
}