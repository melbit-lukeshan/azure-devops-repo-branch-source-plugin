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
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.OkHttp3Helper
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Result
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

object AzureConnector {
    private val LOGGER = Logger.getLogger(AzureConnector::class.java.name)
    fun checkScanCredentials(context: SCMSourceOwner?, apiUri: String, scanCredentialsId: String): FormValidation {
        return checkScanCredentials(context as Item?, apiUri, scanCredentialsId)
    }

    /**
     * Checks the credential ID for use as scan credentials in the supplied context against the supplied API endpoint.
     *
     * @param context           the context.
     * @param apiUri            the api endpoint.
     * @param scanCredentialsId the credentials ID.
     * @return the [FormValidation] results.
     */
    fun checkScanCredentials(context: Item?, apiUri: String, scanCredentialsId: String): FormValidation {
        if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) || context != null && !context.hasPermission(Item.EXTENDED_READ)) {
            return FormValidation.ok()
        }
        if (!scanCredentialsId.isEmpty()) {
            val options = AzureConnector.listScanCredentials(context, apiUri)
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
            val credentials = AzureConnector.lookupScanCredentials(context, apiUri, scanCredentialsId)
            return if (credentials == null) {
                FormValidation.error("Credentials not found")
            } else {
                try {
                    val dummyResult = listProjects("", credentials)
                    val goodValue = dummyResult.getGoodValueOrNull()
                    if (goodValue != null) {
                        FormValidation.ok("%d projects found", goodValue.count)
                    } else {
                        FormValidation.error("Invalid credentials")
                    }
                    //val connector = AzureConnector.connect(apiUri, credentials)
//                    try {
//                        try {
//                            FormValidation.ok("User %s", connector.myself.login)
//                        } catch (e: IOException) {
//                            FormValidation.error("Invalid credentials")
//                        }
//                    } finally {
//                        AzureConnector.release(connector)
//                    }
                } catch (e: IOException) {
                    // ignore, never thrown
                    LOGGER.log(Level.WARNING, "Exception validating credentials {0} on {1}", arrayOf<Any>(CredentialsNameProvider.name(credentials), apiUri))
                    FormValidation.error("Exception validating credentials")
                }

            }
        } else {
            return FormValidation.warning("Credentials are recommended")
        }
    }

    fun listScanCredentials(context: Item?, apiUri: String): ListBoxModel {
        return StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(
                        if (context is Queue.Task)
                            Tasks.getDefaultAuthenticationOf((context as Queue.Task?)!!)
                        else
                            ACL.SYSTEM,
                        context,
                        StandardUsernameCredentials::class.java,
                        azureDomainRequirements(apiUri),
                        azureScanCredentialsMatcher()
                )
    }

    private fun azureScanCredentialsMatcher(): CredentialsMatcher {
        // TODO OAuth credentials
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials::class.java))
    }

    private fun azureDomainRequirements(apiUri: String?): List<DomainRequirement> {
        //return URIRequirementBuilder.fromUri(StringUtils.defaultIfEmpty(apiUri, "https://github.com")).build()
        return URIRequirementBuilder.create().build()
    }

    fun lookupScanCredentials(context: Item?,
                              apiUri: String?,
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
                            azureDomainRequirements(apiUri)
                    ),
                    CredentialsMatchers.allOf(CredentialsMatchers.withId(scanCredentialsId!!), azureScanCredentialsMatcher())
            )
        }
    }

    fun listProjects(apiUri: String?, credentials: StandardCredentials?): Result<Projects, Any> {
        var apiUrl = Util.fixEmptyAndTrim(apiUri)
        val pat = (credentials as StandardUsernamePasswordCredentials).password.plainText
        val listProjectsRequest = ListProjectsRequest(pat, "lukeshan")
        OkHttp3Helper.setDebugMode(true)
        return OkHttp3Helper.executeRequest(listProjectsRequest)
    }
}