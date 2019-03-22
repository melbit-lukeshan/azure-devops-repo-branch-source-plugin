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
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger

object AzureConnector {

    private val LOGGER = Logger.getLogger(AzureConnector::class.java.name)

    init {
        OkHttp2Helper.setDebugMode(true)
    }

    fun lookupCredentials(context: Item?, collectionUrl: String?, credentialsId: String?): StandardCredentials? {
        return if (Util.fixEmpty(credentialsId) == null) {
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
                    CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialsId!!), azureScanCredentialsMatcher())
            )
        }
    }

    fun listCredentials(context: Item?, collectionUrl: String?): ListBoxModel {
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

    fun checkCredentials(context: SCMSourceOwner?, collectionUrl: String, scanCredentialsId: String): FormValidation {
        return checkCredentials(context as Item?, collectionUrl, scanCredentialsId)
    }

    /**
     * Checks the credential ID for use as scan credentials in the supplied context against the supplied API endpoint.
     *
     * @param context           the context.
     * @param collectionUrl     the Azure DevOps collection url.
     * @param scanCredentialsId the credentials ID.
     * @return the [FormValidation] results.
     */
    fun checkCredentials(context: Item?, collectionUrl: String?, credentialsId: String): FormValidation {
        if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) || context != null && !context.hasPermission(Item.EXTENDED_READ)) {
            return FormValidation.ok()
        }
        val fixedCollectionUrl = fixCollectionUrl(collectionUrl)
        if (fixedCollectionUrl == null || fixedCollectionUrl.isEmpty()) {
            return FormValidation.error("Collection URL is empty")
        } else {
            if (!credentialsId.isEmpty()) {
                val options = listCredentials(context, collectionUrl)
                var found = false
                for (b in options) {
                    if (credentialsId == b.value) {
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
                val credentials = lookupCredentials(context, collectionUrl, credentialsId)
                return if (credentials == null) {
                    FormValidation.error("Credentials not found")
                } else {
                    try {
                        val dummyResult = listProjectsR(fixedCollectionUrl, getPat(credentials))
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
                        LOGGER.log(Level.WARNING, "Exception validating credentials {0} on {1}", arrayOf<Any>(CredentialsNameProvider.name(credentials), fixedCollectionUrl))
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
        return fixCollectionUrl(collectionUrl)?.let { fixedCollectionUrl ->
            listProjectsR(fixedCollectionUrl, getPat(credentials)).getGoodValueOrNull() != null
        } ?: false
    }

    private fun getPat(credentials: StandardCredentials): String {
        return (credentials as StandardUsernamePasswordCredentials).password.plainText
    }

    private fun fixCollectionUrl(collectionUrl: String?): String? {
        return Util.fixEmptyAndTrim(collectionUrl)
    }

    private fun listProjectsR(collectionUrl: String, pat: String): Result<Projects, Any> {
        return OkHttp2Helper.executeRequest(ListProjectsRequest(collectionUrl, pat))
    }

    private fun listRepositoriesR(collectionUrl: String, pat: String, projectName: String): Result<Repositories, Any> {
        return OkHttp2Helper.executeRequest(ListRepositoriesRequest(collectionUrl, pat, projectName))
    }

    private fun getRepositoryR(collectionUrl: String, pat: String, projectName: String, repositoryName: String): Result<GitRepository, Any> {
        return OkHttp2Helper.executeRequest(GetRepositoryRequest(collectionUrl, pat, projectName, repositoryName))
    }

    private fun listRefsR(collectionUrl: String, pat: String, projectName: String, repositoryName: String, filter: String): Result<Refs, Any> {
        return OkHttp2Helper.executeRequest(ListRefsRequest(collectionUrl, pat, projectName, repositoryName, filter))
    }

    private fun listCommitsR(collectionUrl: String, pat: String, projectName: String, repositoryName: String): Result<Commits, Any> {
        return OkHttp2Helper.executeRequest(ListCommitsRequest(collectionUrl, pat, projectName, repositoryName))
    }

    private fun getCommitR(collectionUrl: String, pat: String, projectName: String, repositoryName: String, commitId: String): Result<GitCommit, Any> {
        return OkHttp2Helper.executeRequest(GetCommitRequest(collectionUrl, pat, projectName, repositoryName, commitId))
    }

    private fun listItemsR(collectionUrl: String, pat: String, projectName: String, repositoryName: String, scopePath: String, version: String, versionType: GitVersionType, recursionType: VersionControlRecursionType): Result<Items, Any> {
        return OkHttp2Helper.executeRequest(ListItemsRequest(collectionUrl, pat, projectName, repositoryName, scopePath, version, versionType, recursionType))
    }

    private fun getItemR(collectionUrl: String, pat: String, projectName: String, repositoryName: String, itemPath: String, version: String, versionType: GitVersionType): Result<GitItem, Any> {
        return OkHttp2Helper.executeRequest(GetItemRequest(collectionUrl, pat, projectName, repositoryName, itemPath, version, versionType))
    }

    private fun getItemStreamR(collectionUrl: String, pat: String, projectName: String, repositoryName: String, itemPath: String, version: String, versionType: GitVersionType): Result<InputStream, Any> {
        return OkHttp2Helper.executeRequest(GetItemStreamRequest(collectionUrl, pat, projectName, repositoryName, itemPath, version, versionType))
    }

    private fun createCommitStatusR(collectionUrl: String, pat: String, projectName: String, repositoryName: String, commitId: String, status: GitStatusForCreation): Result<GitStatus, Any> {
        return OkHttp2Helper.executeRequest(CreateCommitStatusRequest(collectionUrl, pat, projectName, repositoryName, commitId, status))
    }

    fun getProjectNames(context: Item?, collectionUrl: String?, credentialsId: String?): List<String>? {
        return fixCollectionUrl(collectionUrl)?.let { fixedCollectionUrl ->
            lookupCredentials(context, collectionUrl, credentialsId)?.let { credentials ->
                listProjectsR(fixedCollectionUrl, getPat(credentials)).getGoodValueOrNull()?.let { projects ->
                    projects.value.map { teamProjectReference ->
                        teamProjectReference.name
                    }
                }
            }
        }
    }

    fun getRepositoryNames(context: Item?, collectionUrl: String?, credentialsId: String?, projectName: String): List<String>? {
        return fixCollectionUrl(collectionUrl)?.let { fixedCollectionUrl ->
            lookupCredentials(context, collectionUrl, credentialsId)?.let { credentials ->
                listRepositoriesR(fixedCollectionUrl, getPat(credentials), projectName).getGoodValueOrNull()?.let { repositories ->
                    repositories.value.map { gitRepository ->
                        gitRepository.name
                    }
                }
            }
        }
    }

    fun getRepository(context: Item?, collectionUrl: String?, credentialsId: String?, projectName: String, repositoryName: String): GitRepositoryWithAzureContext? {
        return fixCollectionUrl(collectionUrl)?.let { fixedCollectionUrl ->
            lookupCredentials(context, collectionUrl, credentialsId)?.let { credentials ->
                getRepositoryR(fixedCollectionUrl, getPat(credentials), projectName, repositoryName).getGoodValueOrNull()?.let { gitRepository ->
                    GitRepositoryWithAzureContext(gitRepository, fixedCollectionUrl, credentials, projectName, repositoryName)
                }
            }
        }
    }

    fun getRepository(collectionUrl: String?, credentials: StandardCredentials, projectName: String, repositoryName: String): GitRepositoryWithAzureContext? {
        return fixCollectionUrl(collectionUrl)?.let { fixedCollectionUrl ->
            getRepositoryR(fixedCollectionUrl, getPat(credentials), projectName, repositoryName).getGoodValueOrNull()?.let { gitRepository ->
                GitRepositoryWithAzureContext(gitRepository, fixedCollectionUrl, credentials, projectName, repositoryName)
            }
        }
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
        return getRefs(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                filter)?.find { it.name == "refs/$filter" }
    }

    fun getBranches(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext): List<GitRef>? {
        return getRefs(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                "heads/")
    }

    fun getTags(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext): List<GitRef>? {
        return getRefs(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                "tags/")
    }

    fun getPullRequests(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext): List<GitRef>? {
        return getRefs(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                "pull/")
    }

    private fun getRefs(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, filter: String): List<GitRef>? {
        return listRefsR(collectionUrl, getPat(credentials), projectName, repositoryName, filter).getGoodValueOrNull()?.value
    }

    fun getCommits(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext): List<GitCommitRef>? {
        return getCommits(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName)
    }

    fun getCommit(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, commitId: String): GitCommit? {
        return getCommit(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                commitId)
    }

    private fun getCommits(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String): List<GitCommitRef>? {
        return listCommitsR(collectionUrl, getPat(credentials), projectName, repositoryName).getGoodValueOrNull()?.value
    }

    private fun getCommit(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, commitId: String): GitCommit? {
        return getCommitR(collectionUrl, getPat(credentials), projectName, repositoryName, commitId).getGoodValueOrNull()
    }

    fun getItems(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, scopePath: String, version: String, versionType: GitVersionType, recursionType: VersionControlRecursionType): List<GitItem>? {
        return getItems(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                scopePath,
                version,
                versionType,
                recursionType)
    }

    fun getItem(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, itemPath: String, version: String, versionType: GitVersionType): GitItem? {
        return getItem(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                itemPath,
                version,
                versionType)
    }

    fun getItemStream(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, itemPath: String, version: String, versionType: GitVersionType): InputStream? {
        return getItemStream(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                itemPath,
                version,
                versionType)
    }

    private fun getItems(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, scopePath: String, version: String, versionType: GitVersionType, recursionType: VersionControlRecursionType): List<GitItem>? {
        return listItemsR(collectionUrl, getPat(credentials), projectName, repositoryName, scopePath, version, versionType, recursionType).getGoodValueOrNull()?.value
    }

    private fun getItem(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, itemPath: String, version: String, versionType: GitVersionType): GitItem? {
        return getItemR(collectionUrl, getPat(credentials), projectName, repositoryName, itemPath, version, versionType).getGoodValueOrNull()
    }

    private fun getItemStream(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, itemPath: String, version: String, versionType: GitVersionType): InputStream? {
        return getItemStreamR(collectionUrl, getPat(credentials), projectName, repositoryName, itemPath, version, versionType).getGoodValueOrNull()
    }

    fun createCommitStatus(gitRepositoryWithAzureContext: GitRepositoryWithAzureContext, commitId: String, status: GitStatusForCreation): GitStatus? {
        return createCommitStatus(
                gitRepositoryWithAzureContext.collectionUrl,
                gitRepositoryWithAzureContext.credentials,
                gitRepositoryWithAzureContext.projectName,
                gitRepositoryWithAzureContext.repositoryName,
                commitId,
                status)
    }

    private fun createCommitStatus(collectionUrl: String, credentials: StandardCredentials, projectName: String, repositoryName: String, commitId: String, status: GitStatusForCreation): GitStatus? {
        return createCommitStatusR(collectionUrl, getPat(credentials), projectName, repositoryName, commitId, status).getGoodValueOrNull()
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