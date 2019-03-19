package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.cloudbees.plugins.credentials.CredentialsMatcher
import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsNameProvider
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardCredentials
import com.cloudbees.plugins.credentials.common.StandardListBoxModel
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.DomainRequirement
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.AbortException
import hudson.Util
import hudson.model.Item
import hudson.model.Queue
import hudson.model.TaskListener
import hudson.model.queue.Tasks
import hudson.security.ACL
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.model.Jenkins
import jenkins.scm.api.SCMSourceOwner
import org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoConsoleNote
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.OkHttp2Helper
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Result
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

object AzureConnector {

    private val LOGGER = Logger.getLogger(AzureConnector::class.java.name)

    fun checkScanCredentials(context: SCMSourceOwner?, collectionUrl: String, scanCredentialsId: String): FormValidation {
        return checkScanCredentials(context as Item?, collectionUrl, scanCredentialsId)
    }

    /**
     * Checks the credential ID for use as scan credentials in the supplied context against the supplied API endpoint.
     *
     * @param context           the context.
     * @param collectionUrl     the Azure DevOps collection url.
     * @param scanCredentialsId the credentials ID.
     * @return the [FormValidation] results.
     */
    fun checkScanCredentials(context: Item?, collectionUrl: String?, scanCredentialsId: String): FormValidation {
        if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) || context != null && !context.hasPermission(Item.EXTENDED_READ)) {
            return FormValidation.ok()
        }
        if (collectionUrl == null || collectionUrl.isEmpty()) {
            return FormValidation.error("Collection URL is empty")
        } else {
            if (!scanCredentialsId.isEmpty()) {
                val options = AzureConnector.listScanCredentials(context, collectionUrl)
                var found = false
                for (b in options) {
                    if (scanCredentialsId == b.value) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    return FormValidation.error("Credentials not found")
                }
                if (context != null && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok("Credentials found")
                }
                val credentials = AzureConnector.lookupScanCredentials(context, collectionUrl, scanCredentialsId)
                return if (credentials == null) {
                    FormValidation.error("Credentials not found")
                } else {
                    try {
                        val dummyResult = listProjects(collectionUrl, credentials)
                        val goodValue = dummyResult.getGoodValueOrNull()
                        if (goodValue != null) {
                            FormValidation.ok("%d projects found", goodValue.count)
                        } else {
                            val httpErrorStatus = dummyResult.getHttpErrorStatusOrNull()
                            if (httpErrorStatus != null) {
                                when (httpErrorStatus.code) {
                                    Result.HttpStatus.NOT_FOUND -> FormValidation.error("Invalid collection url")
                                    Result.HttpStatus.UNAUTHORIZED, Result.HttpStatus.NON_AUTHORITATIVE_INFORMATION -> FormValidation.error("Invalid credentials")
                                    else -> FormValidation.error("Invalid collection url or credentials")
                                }
                            } else {
                                FormValidation.error("Invalid collection url or credentials")
                            }
                        }
                    } catch (e: IOException) {
                        // ignore, never thrown
                        LOGGER.log(Level.WARNING, "Exception validating credentials {0} on {1}", arrayOf<Any>(CredentialsNameProvider.name(credentials), collectionUrl))
                        FormValidation.error("Exception validating credentials")
                    }

                }
            } else {
                return FormValidation.warning("Credentials are recommended")
            }
        }
    }

    private fun azureScanCredentialsMatcher(): CredentialsMatcher {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials::class.java))
    }

    private fun azureDomainRequirements(apiUri: String?): List<DomainRequirement> {
        //return URIRequirementBuilder.fromUri(StringUtils.defaultIfEmpty(apiUri, "https://github.com")).build()
        return URIRequirementBuilder.create().build()
    }

    private fun isCredentialValid(collectionUrl: String?, credentials: StandardCredentials): Boolean {
        return if (collectionUrl != null) {
            listProjects(collectionUrl, credentials).getGoodValueOrNull() != null
        } else {
            false
        }
    }

    private fun listProjects(collectionUrl: String, credentials: StandardCredentials): Result<Projects, Any> {
        val fixedCollectionUrl = Util.fixEmptyAndTrim(collectionUrl)!!
        val pat = (credentials as StandardUsernamePasswordCredentials).password.plainText
        val listProjectsRequest = ListProjectsRequest(fixedCollectionUrl, pat)
        OkHttp2Helper.setDebugMode(true)
        return OkHttp2Helper.executeRequest(listProjectsRequest)
    }

    private fun listRepositories(collectionUrl: String, credentials: StandardCredentials, projectName: String): Result<Repositories, Any> {
        val fixedCollectionUrl = Util.fixEmptyAndTrim(collectionUrl)!!
        val pat = (credentials as StandardUsernamePasswordCredentials).password.plainText
        val listRepositoriesRequest = ListRepositoriesRequest(fixedCollectionUrl, pat, projectName)
        OkHttp2Helper.setDebugMode(true)
        return OkHttp2Helper.executeRequest(listRepositoriesRequest)
    }

    private fun listRefsR(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, filter: String): Result<Refs, Any> {
        val fixedCollectionUrl = Util.fixEmptyAndTrim(collectionUrl)!!
        val pat = (credentials as StandardUsernamePasswordCredentials).password.plainText
        val listRefsRequest = ListRefsRequest(fixedCollectionUrl, pat, projectName, repositoryName, filter)
        OkHttp2Helper.setDebugMode(true)
        return OkHttp2Helper.executeRequest(listRefsRequest)
    }

    private fun listCommitsR(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String): Result<Commits, Any> {
        val fixedCollectionUrl = Util.fixEmptyAndTrim(collectionUrl)!!
        val pat = (credentials as StandardUsernamePasswordCredentials).password.plainText
        val listCommitsRequest = ListCommitsRequest(fixedCollectionUrl, pat, projectName, repositoryName)
        OkHttp2Helper.setDebugMode(true)
        return OkHttp2Helper.executeRequest(listCommitsRequest)
    }

    private fun getCommitR(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, commitId: String): Result<GitCommit, Any> {
        val fixedCollectionUrl = Util.fixEmptyAndTrim(collectionUrl)!!
        val pat = (credentials as StandardUsernamePasswordCredentials).password.plainText
        val getCommitRequest = GetCommitRequest(fixedCollectionUrl, pat, projectName, repositoryName, commitId)
        OkHttp2Helper.setDebugMode(true)
        return OkHttp2Helper.executeRequest(getCommitRequest)
    }

    private fun listItemsR(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, scopePath: String, recursionType: VersionControlRecursionType): Result<Items, Any> {
        val fixedCollectionUrl = Util.fixEmptyAndTrim(collectionUrl)!!
        val pat = (credentials as StandardUsernamePasswordCredentials).password.plainText
        val listItemsRequest = ListItemsRequest(fixedCollectionUrl, pat, projectName, repositoryName, scopePath, recursionType)
        OkHttp2Helper.setDebugMode(true)
        return OkHttp2Helper.executeRequest(listItemsRequest)
    }

    private fun getItemR(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, itemPath: String): Result<GitItem, Any> {
        val fixedCollectionUrl = Util.fixEmptyAndTrim(collectionUrl)!!
        val pat = (credentials as StandardUsernamePasswordCredentials).password.plainText
        val getItemRequest = GetItemRequest(fixedCollectionUrl, pat, projectName, repositoryName, itemPath)
        OkHttp2Helper.setDebugMode(true)
        return OkHttp2Helper.executeRequest(getItemRequest)
    }

    fun lookupScanCredentials(context: Item?,
                              collectionUrl: String?,
                              scanCredentialsId: String?): StandardCredentials? {
        return if (Util.fixEmpty(scanCredentialsId) == null) {
            null
        } else {
            CredentialsMatchers.firstOrNull<StandardUsernameCredentials>(
                    CredentialsProvider.lookupCredentials<StandardUsernameCredentials>(
                            StandardUsernameCredentials::class.java,
                            context,
                            if (context is Queue.Task)
                                Tasks.getDefaultAuthenticationOf((context as Queue.Task?)!!)
                            else
                                ACL.SYSTEM,
                            azureDomainRequirements(collectionUrl)
                    ),
                    CredentialsMatchers.allOf(CredentialsMatchers.withId(scanCredentialsId!!), azureScanCredentialsMatcher())
            )
        }
    }

    fun listScanCredentials(context: Item?, collectionUrl: String): ListBoxModel {
        return StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(
                        if (context is Queue.Task)
                            Tasks.getDefaultAuthenticationOf((context as Queue.Task?)!!)
                        else
                            ACL.SYSTEM,
                        context,
                        StandardUsernameCredentials::class.java,
                        azureDomainRequirements(collectionUrl),
                        azureScanCredentialsMatcher()
                )
    }

    fun getProjectNames(context: Item?, collectionUrl: String, credentialsId: String): List<String> {
        val credentials = AzureConnector.lookupScanCredentials(context, collectionUrl, credentialsId)!!
        val result = listProjects(collectionUrl, credentials)
        val projectNameList: ArrayList<String> = arrayListOf()
        result.getGoodValueOrNull()?.let {
            for (project in it.value) {
                projectNameList.add(project.name)
            }
        }
        return projectNameList
    }

    fun getRepositoryNames(context: Item?, collectionUrl: String, credentialsId: String, projectName: String): List<String> {
        val credentials = AzureConnector.lookupScanCredentials(context, collectionUrl, credentialsId)!!
        val result = listRepositories(collectionUrl, credentials, projectName)
        val repositoryNameList: ArrayList<String> = arrayListOf()
        result.getGoodValueOrNull()?.let {
            for (repository in it.value) {
                repositoryNameList.add(repository.name)
            }
        }
        return repositoryNameList
    }

    fun getRepository(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String): GitRepositoryWithAzureContext? {
        val result = listRepositories(collectionUrl, credentials, projectName)
        result.getGoodValueOrNull()?.let {
            for (repository in it.value) {
                if (repository.name == repositoryName) {
                    return GitRepositoryWithAzureContext(repository, collectionUrl, credentials, projectName, repositoryName)
                }
            }
        }
        return null
    }

    fun getRefs(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, filter: String): List<GitRef>? {
        return getRefs(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                filter)
    }

    fun getRef(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, filter: String): GitRef? {
        getRefs(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                filter)?.forEach {
            if (it.name == "refs/$filter") {
                return it
            }
        }
        return null
    }

    fun getBranches(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext): List<GitRef>? {
        return getRefs(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                "heads/")
    }

    private fun getRefs(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, filter: String): List<GitRef>? {
        return listRefsR(collectionUrl, credentials, projectName, repositoryName, filter).getGoodValueOrNull()?.value
    }

    fun getCommits(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext): List<GitCommitRef>? {
        return getCommits(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName)
    }

    fun getCommit(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, commitSha1: String): GitCommit? {
        return getCommit(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                commitSha1)
    }

    private fun getCommits(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String): List<GitCommitRef>? {
        return listCommitsR(collectionUrl, credentials, projectName, repositoryName).getGoodValueOrNull()?.value
    }

    private fun getCommit(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, commitId: String): GitCommit? {
        return getCommitR(collectionUrl, credentials, projectName, repositoryName, commitId).getGoodValueOrNull()
    }

    //TODO GitHub support getting content by path and ref. What do we do?
    fun getItems(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, scopePath: String, recursionType: VersionControlRecursionType): List<GitItem> {
        return getItems(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                scopePath,
                recursionType)
    }

    //TODO GitHub support getting content by path and ref. What do we do?
    fun getItem(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, itemPath: String): GitItem? {
        return getItem(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                itemPath)
    }

    private fun getItems(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, scopePath: String, recursionType: VersionControlRecursionType): List<GitItem> {
        return listItemsR(collectionUrl, credentials, projectName, repositoryName, scopePath, recursionType).getGoodValueOrNull()?.value ?: arrayListOf()
    }

    private fun getItem(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, itemPath: String): GitItem? {
        return getItemR(collectionUrl, credentials, projectName, repositoryName, itemPath).getGoodValueOrNull()
    }

    @Throws(IOException::class)
    fun checkConnectionValidity(collectionUrl: String?, listener: TaskListener, credentials: StandardCredentials?) {
        assert(collectionUrl != null)
        assert(credentials != null)
        if (credentials != null && !isCredentialValid(collectionUrl, credentials)) {
            val message = String.format("Invalid scan credentials %s to connect to %s, skipping", CredentialsNameProvider.name(credentials), collectionUrl)
            throw AbortException(message)
        }
        listener.logger.println(AzureDevOpsRepoConsoleNote.create(
                System.currentTimeMillis(),
                String.format("Connecting to %s using %s", collectionUrl, CredentialsNameProvider.name(credentials!!))
        ))
    }

    @Throws(IOException::class)
    fun checkConnectionValidity(collectionUrl: String?, credentials: StandardCredentials?) {
        assert(collectionUrl != null)
        assert(credentials != null)
        if (credentials != null && !isCredentialValid(collectionUrl, credentials)) {
            val message = String.format("Error using credentials %s to connect to %s", CredentialsNameProvider.name(credentials), collectionUrl)
            throw AbortException(message)
        }
    }
}