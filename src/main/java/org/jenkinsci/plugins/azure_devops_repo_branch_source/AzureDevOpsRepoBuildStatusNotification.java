/*
 * The MIT License
 *
 * Copyright 2016-2017 CloudBees, Inc., Steven Foster
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.model.listeners.SCMListener;
import hudson.model.queue.QueueListener;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.*;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.AzureConnector;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitRepositoryWithAzureContext;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitStatus;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitStatusContext;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitStatusForCreation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages Azure DevOps Statuses.
 * <p>
 * Job (associated to a PR) scheduled: PENDING
 * Build doing a checkout: PENDING
 * Build done: SUCCESS, FAILURE or ERROR
 */
public class AzureDevOpsRepoBuildStatusNotification {

    private static final Logger LOGGER = Logger.getLogger(AzureDevOpsRepoBuildStatusNotification.class.getName());

    private AzureDevOpsRepoBuildStatusNotification() {
    }

    private static void createBuildCommitStatus(Run<?, ?> build, TaskListener listener) {
        SCMSource src = SCMSource.SourceByItem.findSource(build.getParent());
        SCMRevision revision = src != null ? SCMRevisionAction.getRevision(src, build) : null;
        if (revision != null) { // only notify if we have a revision to notify
            try {
                //GitHub gitHub = lookUpGitHub(build.getParent());
                GitRepositoryWithAzureContext repo = lookUpRepo(build.getParent());
                if (repo != null) {
                    Result result = build.getResult();
                    String revisionToNotify = resolveHeadCommit(revision);
                    SCMHead head = revision.getHead();
                    List<AbstractAzureDevOpsNotificationStrategy> strategies = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none())
                            .withTraits(((AzureDevOpsRepoSCMSource) src).getTraits()).notificationStrategies();
                    for (AbstractAzureDevOpsNotificationStrategy strategy : strategies) {
                        // TODO allow strategies to combine/cooperate on a notification
                        AzureDevOpsRepoNotificationContext notificationContext = AzureDevOpsRepoNotificationContext.build(null, build, src, head);
                        List<AzureDevOpsRepoNotificationRequest> details = strategy.notifications(notificationContext, listener);
                        for (AzureDevOpsRepoNotificationRequest request : details) {
                            boolean ignoreError = request.isIgnoreError();
                            //repo.createCommitStatus(revisionToNotify, request.getState(), request.getUrl(), request.getMessage(), request.getContext());
                            final String instanceUrl = StringUtils.stripEnd(Jenkins.get().getRootUrl(), "/");
                            String projectDisplayName = build.getParent().getParent().getFullName() + "/" + build.getParent().getDisplayName();
                            //TODO debug info. Remove later. - Luke
                            System.out.println("AzureDevOpsRepoBuildStatusNotification createBuildCommitStatus");
                            if (head instanceof PullRequestSCMHead) {
                                projectDisplayName = build.getParent().getParent().getFullName() + "/pr-merge";
                            } else if (head instanceof BranchSCMHead) {
                                BranchSCMHead branchSCMHead = (BranchSCMHead) head;
                                if (branchSCMHead.realBranchType == BranchSCMHead.RealBranchType.pr) {
                                    projectDisplayName = build.getParent().getParent().getFullName() + "/pr-merge";
                                }
                            }
                            //TODO end
                            GitStatusContext context = new GitStatusContext(instanceUrl, projectDisplayName);
                            GitStatusForCreation status = new GitStatusForCreation(request.getState(), request.getMessage(), request.getUrl(), context);
                            GitStatus ret = AzureConnector.INSTANCE.createCommitStatus(repo, revisionToNotify, status);
                            if (ret == null) {
                                if (!ignoreError) {
                                    listener.getLogger().format("%nCould not update commit status, please check if your scan " +
                                            "credentials belong to a member of the organization or a collaborator of the " +
                                            "repository and repo:status scope is selected%n%n");
                                }
                            }
                        }
                    }
                    if (result != null) {
                        listener.getLogger().format("%n" + Messages.AzureDevOpsRepoBuildStatusNotification_CommitStatusSet() + "%n%n");
                    }
                }
            } catch (IOException ioe) {
                listener.getLogger().format("%n"
                        + "Could not update commit status. Message: %s%n"
                        + "%n", ioe.getMessage());
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Could not update commit status of run " + build.getFullDisplayName(), ioe);
                }
            }
        }
    }

    /**
     * Returns the GitHub GitRepository associated to a Job.
     *
     * @param job A {@link Job}
     * @return A {@link GitRepositoryWithAzureContext} or null, either if a scan credentials was not provided, or a AzureDevOpsRepoSCMSource was not defined.
     * @throws IOException
     */
    @CheckForNull
    private static GitRepositoryWithAzureContext lookUpRepo(@NonNull Job<?, ?> job) throws IOException {
        SCMSource src = SCMSource.SourceByItem.findSource(job);
        if (src instanceof AzureDevOpsRepoSCMSource) {
            AzureDevOpsRepoSCMSource source = (AzureDevOpsRepoSCMSource) src;
            if (source.getCredentialsId() != null) {
                //return github.getRepository(source.getProjectName() + "/" + source.getRepository());
                return AzureConnector.INSTANCE.getRepository(job, source.getCollectionUrl(), source.getCredentialsId(), source.getProjectName(), source.getRepository());
            }
        }
        return null;
    }

    private static String resolveHeadCommit(SCMRevision revision) throws IllegalArgumentException {
        if (revision instanceof SCMRevisionImpl) {
            return ((SCMRevisionImpl) revision).getHash();
        } else if (revision instanceof PullRequestSCMRevision) {
//            return ((PullRequestSCMRevision) revision).getPullHash();
            //TODO: We use the pr's last merge commit to notify Azure DevOps
            return ((PullRequestSCMRevision) revision).getMergeHash();
        } else {
            throw new IllegalArgumentException("did not recognize " + revision);
        }
    }

    /**
     * With this listener one notifies to Azure DevOps when a Job has been scheduled.
     * Sends: GHCommitState.PENDING
     */
    @Extension
    public static class JobScheduledListener extends QueueListener {

        /**
         * Manages the Azure DevOps Commit Pending GitStatus.
         */
        @Override
        public void onEnterWaiting(Queue.WaitingItem wi) {
            if (!(wi.task instanceof Job)) {
                return;
            }
            final long taskId = wi.getId();
            final Job<?, ?> job = (Job) wi.task;
            final SCMSource source = SCMSource.SourceByItem.findSource(job);
            if (!(source instanceof AzureDevOpsRepoSCMSource)) {
                return;
            }
            final SCMHead head = SCMHead.HeadByItem.findHead(job);
            if (!(head instanceof PullRequestSCMHead)) {
                return;
            }
            final AzureDevOpsRepoSCMSourceContext sourceContext = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none())
                    .withTraits(((AzureDevOpsRepoSCMSource) source).getTraits());
            if (sourceContext.notificationsDisabled()) {
                return;
            }
            // prevent delays in the queue when updating github
            Computer.threadPoolForRemoting.submit(new Runnable() {
                @Override
                public void run() {
                    //GitHub gitHub = null;
                    try {
                        //gitHub = lookUpGitHub(job);
                        String hash = resolveHeadCommit(source.fetch(head, null));
                        GitRepositoryWithAzureContext repo = lookUpRepo(job);
                        if (repo != null) {
                            // The submitter might push another commit before this build even starts.
                            if (Jenkins.get().getQueue().getItem(taskId) instanceof Queue.LeftItem) {
                                // we took too long and the item has left the queue, no longer valid to apply pending

                                // status. JobCheckOutListener is now responsible for setting the pending status.
                                return;
                            }
                            List<AbstractAzureDevOpsNotificationStrategy> strategies = sourceContext.notificationStrategies();
                            for (AbstractAzureDevOpsNotificationStrategy strategy : strategies) {
                                // TODO allow strategies to combine/cooperate on a notification
                                AzureDevOpsRepoNotificationContext notificationContext = AzureDevOpsRepoNotificationContext.build(job, null,
                                        source, head);
                                List<AzureDevOpsRepoNotificationRequest> details = strategy.notifications(notificationContext, null);
                                for (AzureDevOpsRepoNotificationRequest request : details) {
                                    boolean ignoreErrors = request.isIgnoreError();
                                    //repo.createCommitStatus(hash, request.getState(), request.getUrl(), request.getMessage(), request.getContext());
                                    final String instanceUrl = StringUtils.stripEnd(Jenkins.get().getRootUrl(), "/");
                                    String projectDisplayName = job.getParent().getFullName() + "/" + job.getDisplayName();
                                    //TODO debug info. Remove later. - Luke
                                    System.out.println("AzureDevOpsRepoBuildStatusNotification JobScheduledListener");
                                    projectDisplayName = job.getParent().getFullName() + "/pr-merge";
                                    //TODO end
                                    GitStatusContext context = new GitStatusContext(instanceUrl, projectDisplayName);
                                    GitStatusForCreation status = new GitStatusForCreation(request.getState(), request.getMessage(), request.getUrl(), context);
                                    GitStatus ret = AzureConnector.INSTANCE.createCommitStatus(repo, hash, status);
                                    if (ret == null) {
                                        if (!ignoreErrors) {
                                            LOGGER.log(Level.WARNING,
                                                    "Could not update commit status to PENDING. Valid scan credentials? Valid scopes?");
                                        }
                                    }
                                }
                            }
                        }
                    } catch (FileNotFoundException e) {
                        LOGGER.log(Level.WARNING,
                                "Could not update commit status to PENDING. Valid scan credentials? Valid scopes?",
                                LOGGER.isLoggable(Level.FINE) ? e : null);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING,
                                "Could not update commit status to PENDING. Message: " + e.getMessage(),
                                LOGGER.isLoggable(Level.FINE) ? e : null);
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.WARNING,
                                "Could not update commit status to PENDING. Rate limit exhausted",
                                LOGGER.isLoggable(Level.FINE) ? e : null);
                        LOGGER.log(Level.FINE, null, e);
                    }
                }
            });
        }

    }

    /**
     * With this listener one notifies to GitHub when the SCM checkout process has started.
     * Possible option: GHCommitState.PENDING
     */
    @Extension
    public static class JobCheckOutListener extends SCMListener {

        @Override
        public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener, File changelogFile,
                               SCMRevisionState pollingBaseline) throws Exception {
            createBuildCommitStatus(build, listener);
        }

    }

    /**
     * With this listener one notifies to GitHub the build result.
     * Possible options: GHCommitState.SUCCESS, GHCommitState.ERROR or GHCommitState.FAILURE
     */
    @Extension
    public static class JobCompletedListener extends RunListener<Run<?, ?>> {

        @Override
        public void onCompleted(Run<?, ?> build, TaskListener listener) {
            createBuildCommitStatus(build, listener);
        }

    }

}
