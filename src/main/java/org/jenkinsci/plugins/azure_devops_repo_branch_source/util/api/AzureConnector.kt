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
import hudson.Util
import hudson.model.Item
import hudson.model.Queue
import hudson.model.queue.Tasks
import hudson.security.ACL
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.model.Jenkins
import jenkins.scm.api.SCMSourceOwner
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.OkHttp2Helper
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Result
import java.io.IOException
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

    private fun lookupScanCredentials(context: Item?,
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
}