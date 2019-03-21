package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api


import com.cloudbees.jenkins.GitHubRepositoryName
import com.google.common.base.Function
import hudson.Extension
import hudson.ExtensionPoint
import hudson.model.*
import hudson.util.SequentialExecutionQueue
import jenkins.model.Jenkins
import jenkins.scm.api.SCMEvent
import org.apache.commons.lang3.Validate.notNull
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.FluentIterableWrapper.Companion.from
import org.jenkinsci.plugins.github.GitHubPlugin
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber.isInterestedIn
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber.processEvent
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent
import org.jenkinsci.plugins.github.internal.GHPluginConfigException
import org.jenkinsci.plugins.github.util.JobInfoHelpers.isAlive
import org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable
import org.jenkinsci.plugins.github.webhook.GHEventHeader
import org.jenkinsci.plugins.github.webhook.GHEventPayload
import org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload
import org.jenkinsci.plugins.github.webhook.WebhookManager.forHookUrl
import org.kohsuke.accmod.Restricted
import org.kohsuke.accmod.restrictions.NoExternalUse
import org.kohsuke.github.GHEvent
import org.kohsuke.stapler.Stapler
import org.slf4j.LoggerFactory
import java.net.URL


/**
 * Receives Azure DevOps hook.
 *
 * @author Luke Shan
 */
@Extension
class AzureDevOpsWebHook : UnprotectedRootAction {

    @Transient
    private val queue = SequentialExecutionQueue(Computer.threadPoolForRemoting)

    override fun getIconFileName(): String? {
        return null
    }

    override fun getDisplayName(): String? {
        return null
    }

    override fun getUrlName(): String? {
        return URLNAME
    }

    /**
     * If any wants to auto-register hook, then should call this method
     * Example code:
     * `GitHubWebHook.get().registerHookFor(job);`
     *
     * @param job not null project to register hook for
     */
    @Deprecated("use {@link #registerHookFor(Item)}")
    fun registerHookFor(job: Job<*, *>) {
        reRegisterHookForJob<Item>().apply(job)
    }

    /**
     * If any wants to auto-register hook, then should call this method
     * Example code:
     * `GitHubWebHook.get().registerHookFor(item);`
     *
     * @param item not null item to register hook for
     * @since 1.25.0
     */
    fun registerHookFor(item: Item) {
        reRegisterHookForJob<Item>().apply(item)
    }

    /**
     * Calls [.registerHookFor] for every project which have subscriber
     *
     * @return list of jobs which jenkins tried to register hook
     */
    fun reRegisterAllHooks(): List<Item> {
        return from<Item>(jenkinsInstance.getAllItems(Item::class.java))
                .filter(isBuildable<Item>())
                .filter(isAlive<Item>())
                .transform(reRegisterHookForJob<Item>())
                .toList()
    }

    /**
     * Receives the webhook call
     *
     * @param event   GH event type. Never null
     * @param payload Payload from hook. Never blank
     */
    @RequirePostWithGHHookPayload
    fun doIndex(@GHEventHeader event: GHEvent, @GHEventPayload payload: String) {
        val subscriberEvent = GHSubscriberEvent(SCMEvent.originOf(Stapler.getCurrentRequest()), event, payload)
        from<GHEventsSubscriber>(GHEventsSubscriber.all())
                .filter(isInterestedIn(event))
                .transform(processEvent(subscriberEvent)).toList()
    }

    private fun <T : Item> reRegisterHookForJob(): Function<T, T> {
        return object : Function<T, T> {
            override fun apply(job: T?): T {
                LOGGER.debug("Calling registerHooks() for {}", notNull<T>(job, "Item can't be null").getFullName())

                // We should handle wrong url of self defined hook url here in any case with try-catch :(
                val hookUrl: URL
                try {
                    hookUrl = GitHubPlugin.configuration().getHookUrl()
                } catch (e: GHPluginConfigException) {
                    LOGGER.error("Skip registration of GHHook ({})", e.message)
                    return job!!
                }

                val hookRegistrator = forHookUrl(hookUrl).registerFor(job)
                queue.execute(hookRegistrator)
                return job!!
            }
        }
    }

    /**
     * Other plugins may be interested in listening for these updates.
     *
     * @since 1.8
     */
    @Restricted(NoExternalUse::class)
    @Deprecated("working theory is that this API is not required any more with the {@link SCMEvent} based API,\n" +
            "      if wrong, please raise a JIRA")
    abstract class Listener : ExtensionPoint {

        /**
         * Called when there is a change notification on a specific repository.
         *
         * @param pusherName        the pusher name.
         * @param changedRepository the changed repository.
         *
         * @since 1.8
         */
        abstract fun onPushRepositoryChanged(pusherName: String, changedRepository: GitHubRepositoryName)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AzureDevOpsWebHook::class.java)
        val URLNAME = "github-webhook"

        // headers used for testing the endpoint configuration
        val URL_VALIDATION_HEADER = "X-Jenkins-Validation"
        val X_INSTANCE_IDENTITY = "X-Instance-Identity"

        fun get(): AzureDevOpsWebHook? {
            return Jenkins.get().getExtensionList(RootAction::class.java).get(AzureDevOpsWebHook::class.java)
        }

        val jenkinsInstance: Jenkins
            @Throws(IllegalStateException::class)
            get() {
                return Jenkins.get()
            }
    }

}