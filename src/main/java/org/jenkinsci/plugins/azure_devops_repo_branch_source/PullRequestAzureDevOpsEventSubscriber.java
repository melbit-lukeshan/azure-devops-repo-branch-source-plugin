/*
 * The MIT License
 *
 * Copyright 2016 CloudBees, Inc.
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

import com.cloudbees.jenkins.GitHubRepositoryName;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.*;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMHeadPrefilter;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitPullRequest;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitRepository;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Sets.immutableEnumSet;
import static org.kohsuke.github.GHEvent.PULL_REQUEST;

/**
 * This subscriber manages {@link org.kohsuke.github.GHEvent} PULL_REQUEST.
 */
@Extension
public class PullRequestAzureDevOpsEventSubscriber extends GHEventsSubscriber {

    private static final Logger LOGGER = Logger.getLogger(PullRequestAzureDevOpsEventSubscriber.class.getName());
    private static final Pattern REPOSITORY_NAME_PATTERN = Pattern.compile("https?://([^/]+)/([^/]+)/([^/]+)");

    @Override
    protected boolean isApplicable(@Nullable Item project) {
        if (project != null) {
            if (project instanceof SCMSourceOwner) {
                SCMSourceOwner owner = (SCMSourceOwner) project;
                for (SCMSource source : owner.getSCMSources()) {
                    if (source instanceof AzureDevOpsRepoSCMSource) {
                        return true;
                    }
                }
            }
            if (project.getParent() instanceof SCMSourceOwner) {
                SCMSourceOwner owner = (SCMSourceOwner) project.getParent();
                for (SCMSource source : owner.getSCMSources()) {
                    if (source instanceof AzureDevOpsRepoSCMSource) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return set with only PULL_REQUEST event
     */
    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(PULL_REQUEST);
    }

    @Override
    protected void onEvent(GHSubscriberEvent event) {
        try {
            final GHEventPayload.PullRequest p = GitHub.offline()
                    .parseEventPayload(new StringReader(event.getPayload()), GHEventPayload.PullRequest.class);
            String action = p.getAction();
            String repoUrl = p.getRepository().getHtmlUrl().toExternalForm();
            LOGGER.log(Level.INFO, "Received {0} for {1} from {2}",
                    new Object[]{event.getGHEvent(), repoUrl, event.getOrigin()}
            );
            Matcher matcher = REPOSITORY_NAME_PATTERN.matcher(repoUrl);
            if (matcher.matches()) {
                final GitHubRepositoryName changedRepository = GitHubRepositoryName.create(repoUrl);
                if (changedRepository == null) {
                    LOGGER.log(Level.WARNING, "Malformed repository URL {0}", repoUrl);
                    return;
                }

                if ("opened".equals(action)) {
                    fireAfterDelay(new SCMHeadEventImpl(
                            SCMEvent.Type.CREATED,
                            event.getTimestamp(),
                            p,
                            changedRepository,
                            event.getOrigin()
                    ));
                } else if ("reopened".equals(action) || "synchronize".equals(action) || "edited".equals(action)) {
                    fireAfterDelay(new SCMHeadEventImpl(
                            SCMEvent.Type.UPDATED,
                            event.getTimestamp(),
                            p,
                            changedRepository,
                            event.getOrigin()
                    ));
                } else if ("closed".equals(action)) {
                    fireAfterDelay(new SCMHeadEventImpl(
                            SCMEvent.Type.REMOVED,
                            event.getTimestamp(),
                            p,
                            changedRepository,
                            event.getOrigin()
                    ));
                }
            }

        } catch (IOException e) {
            LogRecord lr = new LogRecord(Level.WARNING, "Could not parse {0} event from {1} with payload: {2}");
            lr.setParameters(new Object[]{event.getGHEvent(), event.getOrigin(), event.getPayload()});
            lr.setThrown(e);
            LOGGER.log(lr);
        }
    }

    private void fireAfterDelay(final SCMHeadEventImpl e) {
        SCMHeadEvent.fireLater(e, AzureDevOpsRepoSCMSource.getEventDelaySeconds(), TimeUnit.SECONDS);
    }

    private static class SCMHeadEventImpl extends SCMHeadEvent<GHEventPayload.PullRequest> {
        private final String repoHost;
        private final String repoOwner;
        private final String repository;

        public SCMHeadEventImpl(Type type, long timestamp, GHEventPayload.PullRequest pullRequest, GitHubRepositoryName repo,
                                String origin) {
            super(type, timestamp, pullRequest, origin);
            this.repoHost = repo.getHost();
            this.repoOwner = pullRequest.getRepository().getOwnerName();
            this.repository = pullRequest.getRepository().getName();
        }

        private boolean isApiMatch(String apiUri) {
            return repoHost.equalsIgnoreCase(RepositoryUriResolver.hostnameFromApiUri(apiUri));
        }

        @Override
        public boolean isMatch(@NonNull SCMNavigator navigator) {
            return navigator instanceof AzureDevOpsRepoSCMNavigator
                    && repoOwner.equalsIgnoreCase(((AzureDevOpsRepoSCMNavigator) navigator).getRepoOwner());
        }

        @Override
        public String descriptionFor(@NonNull SCMNavigator navigator) {
            String action = getPayload().getAction();
            if (action != null) {
                switch (action) {
                    case "opened":
                        return "Pull request #" + getPayload().getNumber() + " opened in repository " + repository;
                    case "reopened":
                        return "Pull request #" + getPayload().getNumber() + " reopened in repository " + repository;
                    case "synchronize":
                        return "Pull request #" + getPayload().getNumber() + " updated in repository " + repository;
                    case "closed":
                        return "Pull request #" + getPayload().getNumber() + " closed in repository " + repository;
                }
            }
            return "Pull request #" + getPayload().getNumber() + " event in repository " + repository;
        }

        @Override
        public String descriptionFor(SCMSource source) {
            String action = getPayload().getAction();
            if (action != null) {
                switch (action) {
                    case "opened":
                        return "Pull request #" + getPayload().getNumber() + " opened";
                    case "reopened":
                        return "Pull request #" + getPayload().getNumber() + " reopened";
                    case "synchronize":
                        return "Pull request #" + getPayload().getNumber() + " updated";
                    case "closed":
                        return "Pull request #" + getPayload().getNumber() + " closed";
                }
            }
            return "Pull request #" + getPayload().getNumber() + " event";
        }

        @Override
        public String description() {
            String action = getPayload().getAction();
            if (action != null) {
                switch (action) {
                    case "opened":
                        return "Pull request #" + getPayload().getNumber() + " opened in repository " + repoOwner + "/" + repository;
                    case "reopened":
                        return "Pull request #" + getPayload().getNumber() + " reopened in repository " + repoOwner
                                + "/" + repository;
                    case "synchronize":
                        return "Pull request #" + getPayload().getNumber() + " updated in repository " + repoOwner + "/"
                                + repository;
                    case "closed":
                        return "Pull request #" + getPayload().getNumber() + " closed in repository " + repoOwner + "/"
                                + repository;
                }
            }
            return "Pull request #" + getPayload().getNumber() + " event in repository " + repoOwner + "/" + repository;
        }

        @NonNull
        @Override
        public String getSourceName() {
            return repository;
        }

        @NonNull
        @Override
        public Map<SCMHead, SCMRevision> heads(@NonNull SCMSource source) {
            if (!(source instanceof AzureDevOpsRepoSCMSource
                    && isApiMatch(((AzureDevOpsRepoSCMSource) source).getCollectionUrl())
                    && repoOwner.equalsIgnoreCase(((AzureDevOpsRepoSCMSource) source).getProjectName())
                    && repository.equalsIgnoreCase(((AzureDevOpsRepoSCMSource) source).getRepository()))) {
                return Collections.emptyMap();
            }
            AzureDevOpsRepoSCMSource src = (AzureDevOpsRepoSCMSource) source;
            //GHEventPayload.PullRequest pullRequest = getPayload();
            //GHPullRequest ghPullRequest = pullRequest.getPullRequest();
            //GHRepository repo = pullRequest.getRepository();
            //TODO we need to populate the GitPullRequest - Luke
            GitPullRequest gitPullRequest = null;
            GitRepository repo = gitPullRequest.getRepository();
            String prRepoName = repo.getName();
            if (!prRepoName.matches(AzureDevOpsRepoSCMSource.VALID_GITHUB_REPO_NAME)) {
                // fake repository name
                return Collections.emptyMap();
            }
//            GHUser user;
//            try {
//                user = ghPullRequest.getHead().getUser();
//            } catch (IOException e) {
//                // fake owner name
//                return Collections.emptyMap();
//            }
            String prOwnerName = gitPullRequest.getCreatedBy().getUniqueName();//TODO unique name or id? - Luke
            if (!prOwnerName.matches(AzureDevOpsRepoSCMSource.VALID_GITHUB_USER_NAME)) {
                // fake owner name
                return Collections.emptyMap();
            }
            if (!gitPullRequest.getLastMergeTargetCommit().getCommitId().matches(AzureDevOpsRepoSCMSource.VALID_GIT_SHA1)) {
                // fake base sha1
                return Collections.emptyMap();
            }
            if (!gitPullRequest.getLastMergeSourceCommit().getCommitId().matches(AzureDevOpsRepoSCMSource.VALID_GIT_SHA1)) {
                // fake head sha1
                return Collections.emptyMap();
            }

            boolean fork = !src.getProjectName().equalsIgnoreCase(prOwnerName);

            Map<SCMHead, SCMRevision> result = new HashMap<>();
            AzureDevOpsRepoSCMSourceContext context = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none())
                    .withTraits(src.getTraits());
            if (!fork && context.wantBranches()) {
                final String branchName = gitPullRequest.getSourceRefName();
                SCMHead head = new BranchSCMHead(branchName);
                boolean excluded = false;
                for (SCMHeadPrefilter prefilter : context.prefilters()) {
                    if (prefilter.isExcluded(source, head)) {
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    SCMRevision hash =
                            new AbstractGitSCMSource.SCMRevisionImpl(head, gitPullRequest.getLastMergeSourceCommit().getCommitId());
                    result.put(head, hash);
                }
            }
            if (context.wantPRs()) {
                int number = gitPullRequest.getPullRequestId();
                Set<ChangeRequestCheckoutStrategy> strategies = fork ? context.forkPRStrategies() : context.originPRStrategies();
                for (ChangeRequestCheckoutStrategy strategy : strategies) {
                    final String branchName;
                    if (strategies.size() == 1) {
                        branchName = "PR-" + number;
                    } else {
                        branchName = "PR-" + number + "-" + strategy.name().toLowerCase(Locale.ENGLISH);
                    }
                    PullRequestSCMHead head;
                    PullRequestSCMRevision revision;
                    switch (strategy) {
                        case MERGE:
                            // it will take a call to Azure DevOps to get the merge commit, so let the event receiver poll
                            head = new PullRequestSCMHead(gitPullRequest, branchName, true);
                            revision = null;
                            break;
                        default:
                            // Give the event receiver the data we have so they can revalidate
                            head = new PullRequestSCMHead(gitPullRequest, branchName, false);
                            revision = new PullRequestSCMRevision(
                                    head,
                                    gitPullRequest.getLastMergeTargetCommit().getCommitId(),
                                    gitPullRequest.getLastMergeTargetCommit().getCommitId()
                            );
                            break;
                    }
                    boolean excluded = false;
                    for (SCMHeadPrefilter prefilter : context.prefilters()) {
                        if (prefilter.isExcluded(source, head)) {
                            excluded = true;
                            break;
                        }
                    }
                    if (!excluded) {
                        result.put(head, revision);
                    }
                }
            }
            return result;
        }

        @Override
        public boolean isMatch(@NonNull SCM scm) {
            return false;
        }
    }
}
