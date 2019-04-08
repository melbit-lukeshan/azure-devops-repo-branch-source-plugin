/*
 * The MIT License
 *
 * Copyright 2015-2017 CloudBees, Inc.
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

import com.cloudbees.jenkins.GitHubWebHook;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.AbortException;
import hudson.Extension;
import hudson.RestrictedSince;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.plugins.git.MergeWithGitSCMExtension;
import jenkins.plugins.git.traits.GitBrowserSCMSourceTrait;
import jenkins.scm.api.*;
import jenkins.scm.api.metadata.ContributorMetadataAction;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMTrait;
import jenkins.scm.api.trait.SCMTraitDescriptor;
import jenkins.scm.impl.ChangeRequestSCMHeadCategory;
import jenkins.scm.impl.TagSCMHeadCategory;
import jenkins.scm.impl.UncategorizedSCMHeadCategory;
import jenkins.scm.impl.form.NamedArrayList;
import jenkins.scm.impl.trait.Discovery;
import jenkins.scm.impl.trait.Selection;
import jenkins.scm.impl.trait.WildcardSCMHeadFilterTrait;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.AzureConnector;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.*;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hudson.Functions.isWindows;
import static hudson.model.Items.XSTREAM2;

public class AzureDevOpsRepoSCMSource extends AbstractGitSCMSource {

    public static final String VALID_GITHUB_REPO_NAME = "^[0-9A-Za-z._-]+$";
    public static final String VALID_GITHUB_USER_NAME = "^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$";
    public static final String VALID_GIT_SHA1 = "^[a-fA-F0-9]{40}$";
    private static final Logger LOGGER = Logger.getLogger(AzureDevOpsRepoSCMSource.class.getName());
    private static final String R_PULL = Constants.R_REFS + "pull/";
    /**
     * Lock to guard access to the {@link #pullRequestSourceMap} field and prevent concurrent Azure DevOps Repo queries during
     * a 1.x to 2.2.0+ upgrade.
     *
     * @since 2.2.0
     */
    private static final Object pullRequestSourceMapLock = new Object();
    /**
     * How long to delay events received from Azure DevOps Repo in order to allow the API caches to sync.
     */
    private static /*mostly final*/ int eventDelaySeconds =
            Math.min(300, Math.max(0, Integer.getInteger(AzureDevOpsRepoSCMSource.class.getName() + ".eventDelaySeconds", 5)));
    /**
     * How big (in megabytes) an on-disk cache to keep of Azure DevOps Repo API responses. Cache is per repo, per credentials.
     */
    private static /*mostly final*/ int cacheSize =
            Math.min(1024, Math.max(0, Integer.getInteger(AzureDevOpsRepoSCMSource.class.getName() + ".cacheSize", isWindows() ? 0 : 20)));

    //////////////////////////////////////////////////////////////////////
    // Configuration fields
    //////////////////////////////////////////////////////////////////////

    /**
     * The Azure DevOps collection URL.
     */
    @NonNull
    private final String collectionUrl;

    /**
     * Credentials for Azure DevOps Repo API; currently only supports username/password (personal access token).
     *
     * @since 2.2.0
     */
    @CheckForNull
    private String credentialsId;

    /**
     * The project name
     */
    @NonNull
    private final String projectName;

    /**
     * The repository
     */
    @NonNull
    private final String repository;

    /**
     * The behaviours to apply to this source.
     *
     * @since 2.2.0
     */
    @NonNull
    private List<SCMSourceTrait> traits;

    //////////////////////////////////////////////////////////////////////
    // Run-time cached state
    //////////////////////////////////////////////////////////////////////

    /**
     * Cache of the official repository HTML URL.
     */
    @CheckForNull
    private transient URL repositoryUrl;
    /**
     * The collaborator names used to determine if pull requests are from trusted authors
     */
    @CheckForNull
    private transient Set<String> collaboratorNames;
    /**
     * Cache of details of the repository.
     */
    @CheckForNull
    private transient GHRepository ghRepository;

    /**
     * Cache of details of the Azure repository.
     */
    @CheckForNull
    private transient GitRepositoryWithAzureContext gitRepository;

    /**
     * The cache of {@link ObjectMetadataAction} instances for each open PR.
     */
    @NonNull
    private Map<Integer, ObjectMetadataAction> pullRequestMetadataCache;
    /**
     * The cache of {@link ContributorMetadataAction} instances for each open PR.
     */
    @NonNull
    private transient /*effectively final*/ Map<Integer, ContributorMetadataAction> pullRequestContributorCache;

    /**
     * Used during upgrade from 1.x to 2.2.0+ only.
     *
     * @see #retrievePullRequestSource(int)
     * @see PullRequestSCMHead.FixMetadata
     * @see PullRequestSCMHead.FixMetadataMigration
     * @since 2.2.0
     */
    @CheckForNull // normally null except during a migration from 1.x
    private transient /*effectively final*/ Map<Integer, PullRequestSource> pullRequestSourceMap;

    /**
     * Constructor, anonymous access, does not default any
     * {@link SCMSourceTrait} behaviours.
     *
     * @param repository the repository name.
     * @since 2.2.0
     */
    @DataBoundConstructor
    public AzureDevOpsRepoSCMSource(@NonNull String collectionUrl, @NonNull String repository, @NonNull String projectName) {
        this.collectionUrl = collectionUrl;
        this.repository = repository;
        this.projectName = projectName;
        pullRequestMetadataCache = new ConcurrentHashMap<>();
        pullRequestContributorCache = new ConcurrentHashMap<>();
        this.traits = new ArrayList<>();
    }

    /**
     * Legacy constructor.
     *
     * @param id                    the source id.
     * @param checkoutCredentialsId the checkout credentials id or {@link DescriptorImpl#SAME} or
     *                              {@link DescriptorImpl#ANONYMOUS}.
     * @param scanCredentialsId     the scan credentials id or {@code null}.
     * @param repository            the repository name.
     */
    @Deprecated
    public AzureDevOpsRepoSCMSource(@CheckForNull String id, @NonNull String checkoutCredentialsId,
                                    @CheckForNull String scanCredentialsId, @NonNull String collectionUrl,
                                    @NonNull String repository, @NonNull String projectName) {
        this(collectionUrl, repository, projectName);
        setId(id);
        setCredentialsId(scanCredentialsId);
        this.traits = new ArrayList<>();
        this.traits.add(new BranchDiscoveryTrait(true, true));
        this.traits.add(new ForkPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE), new ForkPullRequestDiscoveryTrait.TrustPermission()));
        if (!DescriptorImpl.SAME.equals(checkoutCredentialsId)) {
            traits.add(new SSHCheckoutTrait(checkoutCredentialsId));
        }
    }

    /**
     * Returns how long to delay events received from Azure DevOps Repo in order to allow the API caches to sync.
     *
     * @return how long to delay events received from Azure DevOps Repo in order to allow the API caches to sync.
     */
    public static int getEventDelaySeconds() {
        return eventDelaySeconds;
    }

    /**
     * Sets how long to delay events received from Azure DevOps Repo in order to allow the API caches to sync.
     *
     * @param eventDelaySeconds number of seconds to delay, will be restricted into a value within the range
     *                          {@code [0,300]} inclusive
     */
    @Restricted(NoExternalUse.class) // to allow configuration from system groovy console
    public static void setEventDelaySeconds(int eventDelaySeconds) {
        AzureDevOpsRepoSCMSource.eventDelaySeconds = Math.min(300, Math.max(0, eventDelaySeconds));
    }

    /**
     * Returns how many megabytes of on-disk cache to maintain per Azure DevOps Repo API URL per credentials.
     *
     * @return how many megabytes of on-disk cache to maintain per Azure DevOps Repo API URL per credentials.
     */
    public static int getCacheSize() {
        return cacheSize;
    }

    /**
     * Sets how long to delay events received from Azure DevOps Repo in order to allow the API caches to sync.
     *
     * @param cacheSize how many megabytes of on-disk cache to maintain per Azure DevOps Repo API URL per credentials,
     *                  will be restricted into a value within the range {@code [0,1024]} inclusive.
     */
    @Restricted(NoExternalUse.class) // to allow configuration from system groovy console
    public static void setCacheSize(int cacheSize) {
        AzureDevOpsRepoSCMSource.cacheSize = Math.min(1024, Math.max(0, cacheSize));
    }

    /**
     * @return the Azure DevOps collection url.
     */
    @Exported
    @NonNull
    public String getCollectionUrl() {
        return collectionUrl;
    }

    /**
     * Gets the repository name.
     *
     * @return the repository name.
     */
    @Exported
    @NonNull
    public String getRepository() {
        return repository;
    }

    /**
     * Gets the project name.
     *
     * @return the project name.
     */
    @Exported
    @NonNull
    public String getProjectName() {
        return projectName;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.2.0
     */
    @Override
    public List<SCMSourceTrait> getTraits() {
        return traits;
    }

    /**
     * Gets the credentials used to access the Azure DevOps Repo REST API (also used as the default credentials for checking out
     * sources.
     *
     * @return the credentials used to access the Azure DevOps Repo REST API or {@code null} to access anonymously
     */
    @Override
    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Sets the behaviours that are applied to this {@link AzureDevOpsRepoSCMSource}.
     *
     * @param traits the behaviours that are to be applied.
     */
    @DataBoundSetter
    public void setTraits(@CheckForNull List<SCMSourceTrait> traits) {
        this.traits = new ArrayList<>(Util.fixNull(traits));
    }

    /**
     * Sets the credentials used to access the Azure DevOps Repo REST API (also used as the default credentials for checking out
     * sources.
     *
     * @param credentialsId the credentials used to access the Azure DevOps Repo REST API or {@code null} to access anonymously
     * @since 2.2.0
     */
    @DataBoundSetter
    public void setCredentialsId(@CheckForNull String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemote() {
        return AzureDevOpsRepoSCMBuilder.uriResolver(getOwner(), collectionUrl, credentialsId)
                .getRepositoryUri(collectionUrl, projectName, repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPronoun() {
        return Messages.AzureDevOpsRepoSCMSource_Pronoun();
    }

    /**
     * Returns a {@link RepositoryUriResolver} according to credentials configuration.
     *
     * @return a {@link RepositoryUriResolver}
     * @deprecated use {@link AzureDevOpsRepoSCMBuilder#uriResolver()} or {@link AzureDevOpsRepoSCMBuilder#uriResolver(Item, String, String)}.
     */
    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    public RepositoryUriResolver getUriResolver() {
        return AzureDevOpsRepoSCMBuilder.uriResolver(
                getOwner(),
                collectionUrl,
                credentialsId
        );
    }

    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @Deprecated
    @CheckForNull
    public String getScanCredentialsId() {
        return credentialsId;
    }

    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @Deprecated
    public void setScanCredentialsId(@CheckForNull String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @Deprecated
    @CheckForNull
    public String getCheckoutCredentialsId() {
        for (SCMSourceTrait trait : traits) {
            if (trait instanceof SSHCheckoutTrait) {
                return StringUtils.defaultString(
                        ((SSHCheckoutTrait) trait).getCredentialsId(),
                        AzureDevOpsRepoSCMSource.DescriptorImpl.ANONYMOUS
                );
            }
        }
        return DescriptorImpl.SAME;
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setIncludes(@NonNull String includes) {
        for (int i = 0; i < traits.size(); i++) {
            SCMSourceTrait trait = traits.get(i);
            if (trait instanceof WildcardSCMHeadFilterTrait) {
                WildcardSCMHeadFilterTrait existing = (WildcardSCMHeadFilterTrait) trait;
                if ("*".equals(includes) && "".equals(existing.getExcludes())) {
                    traits.remove(i);
                } else {
                    traits.set(i, new WildcardSCMHeadFilterTrait(includes, existing.getExcludes()));
                }
                return;
            }
        }
        if (!"*".equals(includes)) {
            traits.add(new WildcardSCMHeadFilterTrait(includes, ""));
        }
    }

    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setExcludes(@NonNull String excludes) {
        for (int i = 0; i < traits.size(); i++) {
            SCMSourceTrait trait = traits.get(i);
            if (trait instanceof WildcardSCMHeadFilterTrait) {
                WildcardSCMHeadFilterTrait existing = (WildcardSCMHeadFilterTrait) trait;
                if ("*".equals(existing.getIncludes()) && "".equals(excludes)) {
                    traits.remove(i);
                } else {
                    traits.set(i, new WildcardSCMHeadFilterTrait(existing.getIncludes(), excludes));
                }
                return;
            }
        }
        if (!"".equals(excludes)) {
            traits.add(new WildcardSCMHeadFilterTrait("*", excludes));
        }
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    public boolean getBuildOriginBranch() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof BranchDiscoveryTrait) {
                return ((BranchDiscoveryTrait) trait).isBuildBranch();
            }
        }
        return false;
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setBuildOriginBranch(boolean buildOriginBranch) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof BranchDiscoveryTrait) {
                BranchDiscoveryTrait previous = (BranchDiscoveryTrait) trait;
                if (buildOriginBranch || previous.isBuildBranchesWithPR()) {
                    traits.set(i, new BranchDiscoveryTrait(buildOriginBranch, previous.isBuildBranchesWithPR()));
                } else {
                    traits.remove(i);
                }
                return;
            }
        }
        if (buildOriginBranch) {
            traits.add(new BranchDiscoveryTrait(buildOriginBranch, false));
        }
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    public boolean getBuildOriginBranchWithPR() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof BranchDiscoveryTrait) {
                return ((BranchDiscoveryTrait) trait).isBuildBranchesWithPR();
            }
        }
        return false;
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setBuildOriginBranchWithPR(boolean buildOriginBranchWithPR) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof BranchDiscoveryTrait) {
                BranchDiscoveryTrait previous = (BranchDiscoveryTrait) trait;
                if (buildOriginBranchWithPR || previous.isBuildBranch()) {
                    traits.set(i, new BranchDiscoveryTrait(previous.isBuildBranch(), buildOriginBranchWithPR));
                } else {
                    traits.remove(i);
                }
                return;
            }
        }
        if (buildOriginBranchWithPR) {
            traits.add(new BranchDiscoveryTrait(false, buildOriginBranchWithPR));
        }
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    public boolean getBuildOriginPRMerge() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof OriginPullRequestDiscoveryTrait) {
                return ((OriginPullRequestDiscoveryTrait) trait).getStrategies()
                        .contains(ChangeRequestCheckoutStrategy.MERGE);
            }
        }
        return false;
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setBuildOriginPRMerge(boolean buildOriginPRMerge) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof OriginPullRequestDiscoveryTrait) {
                Set<ChangeRequestCheckoutStrategy> s = ((OriginPullRequestDiscoveryTrait) trait).getStrategies();
                if (buildOriginPRMerge) {
                    s.add(ChangeRequestCheckoutStrategy.MERGE);
                } else {
                    s.remove(ChangeRequestCheckoutStrategy.MERGE);
                }
                traits.set(i, new OriginPullRequestDiscoveryTrait(s));
                return;
            }
        }
        if (buildOriginPRMerge) {
            traits.add(new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE)));
        }
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    public boolean getBuildOriginPRHead() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof OriginPullRequestDiscoveryTrait) {
                return ((OriginPullRequestDiscoveryTrait) trait).getStrategies()
                        .contains(ChangeRequestCheckoutStrategy.HEAD);
            }
        }
        return false;

    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setBuildOriginPRHead(boolean buildOriginPRHead) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof OriginPullRequestDiscoveryTrait) {
                Set<ChangeRequestCheckoutStrategy> s = ((OriginPullRequestDiscoveryTrait) trait).getStrategies();
                if (buildOriginPRHead) {
                    s.add(ChangeRequestCheckoutStrategy.HEAD);
                } else {
                    s.remove(ChangeRequestCheckoutStrategy.HEAD);
                }
                traits.set(i, new OriginPullRequestDiscoveryTrait(s));
                return;
            }
        }
        if (buildOriginPRHead) {
            traits.add(new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD)));
        }
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    public boolean getBuildForkPRMerge() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof ForkPullRequestDiscoveryTrait) {
                return ((ForkPullRequestDiscoveryTrait) trait).getStrategies()
                        .contains(ChangeRequestCheckoutStrategy.MERGE);
            }
        }
        return false;
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setBuildForkPRMerge(boolean buildForkPRMerge) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof ForkPullRequestDiscoveryTrait) {
                ForkPullRequestDiscoveryTrait forkTrait = (ForkPullRequestDiscoveryTrait) trait;
                Set<ChangeRequestCheckoutStrategy> s = forkTrait.getStrategies();
                if (buildForkPRMerge) {
                    s.add(ChangeRequestCheckoutStrategy.MERGE);
                } else {
                    s.remove(ChangeRequestCheckoutStrategy.MERGE);
                }
                traits.set(i, new ForkPullRequestDiscoveryTrait(s, forkTrait.getTrust()));
                return;
            }
        }
        if (buildForkPRMerge) {
            traits.add(new ForkPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE),
                    new ForkPullRequestDiscoveryTrait.TrustPermission()));
        }
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    public boolean getBuildForkPRHead() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof ForkPullRequestDiscoveryTrait) {
                return ((ForkPullRequestDiscoveryTrait) trait).getStrategies()
                        .contains(ChangeRequestCheckoutStrategy.HEAD);
            }
        }
        return false;
    }


    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setBuildForkPRHead(boolean buildForkPRHead) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof ForkPullRequestDiscoveryTrait) {
                ForkPullRequestDiscoveryTrait forkTrait = (ForkPullRequestDiscoveryTrait) trait;
                Set<ChangeRequestCheckoutStrategy> s = forkTrait.getStrategies();
                if (buildForkPRHead) {
                    s.add(ChangeRequestCheckoutStrategy.HEAD);
                } else {
                    s.remove(ChangeRequestCheckoutStrategy.HEAD);
                }
                traits.set(i, new ForkPullRequestDiscoveryTrait(s, forkTrait.getTrust()));
                return;
            }
        }
        if (buildForkPRHead) {
            traits.add(new ForkPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD),
                    new ForkPullRequestDiscoveryTrait.TrustPermission()));
        }
    }

    @Override
    protected final void retrieve(@CheckForNull SCMSourceCriteria criteria,
                                  @NonNull SCMHeadObserver observer,
                                  @CheckForNull SCMHeadEvent<?> event,
                                  @NonNull final TaskListener listener) throws IOException, InterruptedException {
        StandardCredentials credentials = Connector.lookupScanCredentials((Item) getOwner(), collectionUrl, credentialsId);
        checkApiUrlValidity(credentials);

        try {
            AzureConnector.INSTANCE.checkConnectionValidity(collectionUrl, listener, credentials);

            if (StringUtils.isBlank(repository)) {
                throw new AbortException("No repository selected, skipping");
            }

            this.gitRepository = AzureConnector.INSTANCE.getRepository(collectionUrl, credentials, projectName, repository);

            String fullName = projectName + "/" + repository;
            final GitRepositoryWithAzureContext gitRepository = this.gitRepository;

            listener.getLogger().format("Examining %s%n", HyperlinkNote.encodeTo(gitRepository.getGitRepository().getRemoteUrl(), fullName));

            repositoryUrl = new URL(gitRepository.getGitRepository().getRemoteUrl());

            try (final AzureDevOpsRepoSCMSourceRequest request = new AzureDevOpsRepoSCMSourceContext(criteria, observer)
                    .withTraits(traits)
                    .newRequest(this, listener)) {
                if (request.isFetchPRs()) {
                    request.setPullRequests(new LazyPullRequestsAzure(request, gitRepository));
                }
                if (request.isFetchBranches()) {
                    request.setBranches(new LazyBranchesAzure(request, gitRepository));
                }
                if (request.isFetchTags()) {
                    request.setTags(new LazyTagsAzure(request, gitRepository));
                }
                //TODO uncomment below. For now we ignore permission source- Luke.
                //request.setCollaboratorNames(new LazyContributorNames(request, listener, github, ghRepository, credentials));
                request.setPermissionsSource(new AzureDevOpsRepoPermissionsSource() {
                    @Override
                    public AzurePermissionType fetch(String username) throws IOException, InterruptedException {
                        //return ghRepository.getPermission(username);
                        return AzurePermissionType.ADMIN;
                    }
                });

                if (request.isFetchBranches() && !request.isComplete()) {
                    listener.getLogger().format("%n  Checking branches...%n");
                    int count = 0;
                    for (final GitRef branch : request.getBranches()) {
                        count++;
                        String branchName;
                        BranchSCMHead.RealBranchType realBranchType;
                        if (branch.isBranch()) {
                            branchName = branch.getBranchName();
                            realBranchType = BranchSCMHead.RealBranchType.branch;
                        } else if (branch.isPullRequest()) {
                            branchName = branch.getPullRequestName();//Note we may treat PR as branch, so we may actually get the PR name here
                            realBranchType = BranchSCMHead.RealBranchType.pr;
                        } else {
                            branchName = branch.getTagName();//Note we may treat tag as branch, so we may actually get the tag name here
                            realBranchType = BranchSCMHead.RealBranchType.tag;
                        }

                        listener.getLogger().format("%n    Checking branch %s%n", HyperlinkNote.encodeTo(repositoryUrl + "?version=GB" + branchName, branchName));
                        BranchSCMHead head = new BranchSCMHead(branchName, realBranchType);
                        if (request.process(head, new SCMRevisionImpl(head, branch.getObjectId()),
                                new SCMSourceRequest.ProbeLambda<BranchSCMHead, SCMRevisionImpl>() {
                                    @NonNull
                                    @Override
                                    public SCMSourceCriteria.Probe create(@NonNull BranchSCMHead head, @Nullable SCMRevisionImpl revisionInfo) {
                                        return new AzureDevOpsRepoSCMProbe(gitRepository, head, revisionInfo);
                                    }
                                }, new CriteriaWitness(listener))) {
                            listener.getLogger().format("%n  %d branches were processed (query completed)%n", count);
                            break;
                        }
                    }
                    listener.getLogger().format("%n  %d branches were processed%n", count);
                }
                if (request.isFetchPRs() && !request.isComplete()) {
                    listener.getLogger().format("%n  Checking pull-requests...%n");
                    int count = 0;
                    Map<Boolean, Set<ChangeRequestCheckoutStrategy>> strategies = request.getPRStrategies();
                    PRs:
                    for (final GitPullRequest pr : request.getPullRequests()) {
                        int number = pr.getPullRequestId();
                        boolean fork = (pr.getForkSource() != null);
                        listener.getLogger().format("%n    Checking pull request %s%n", HyperlinkNote.encodeTo(pr.getUrl(), "#" + number));
                        if (strategies.get(fork).isEmpty()) {
                            if (fork) {
                                listener.getLogger().format("    Submitted from fork, skipping%n%n");
                            } else {
                                listener.getLogger().format("    Submitted from origin repository, skipping%n%n");
                            }
                            continue;
                        }
                        for (final ChangeRequestCheckoutStrategy strategy : strategies.get(fork)) {
                            final String branchName;
                            if (strategies.get(fork).size() == 1) {
                                branchName = "PR-" + number;
                            } else {
                                branchName = "PR-" + number + "-" + strategy.name().toLowerCase(Locale.ENGLISH);
                            }
                            count++;
                            if (request.process(new PullRequestSCMHead(pr, branchName, strategy == ChangeRequestCheckoutStrategy.MERGE),
                                    null,
                                    new SCMSourceRequest.ProbeLambda<PullRequestSCMHead, Void>() {
                                        @NonNull
                                        @Override
                                        public SCMSourceCriteria.Probe create(@NonNull PullRequestSCMHead head, @Nullable Void revisionInfo)
                                                throws IOException, InterruptedException {
                                            boolean trusted = request.isTrusted(head);
                                            if (!trusted) {
                                                listener.getLogger().format("    (not from a trusted source)%n");
                                            }
                                            return new AzureDevOpsRepoSCMProbe(gitRepository, trusted ? head : head.getTarget(), null);
                                        }
                                    },
                                    new SCMSourceRequest.LazyRevisionLambda<PullRequestSCMHead, SCMRevision, Void>() {
                                        @NonNull
                                        @Override
                                        public SCMRevision create(@NonNull PullRequestSCMHead head,
                                                                  @Nullable Void ignored) {
                                            String baseSha = pr.getLastMergeTargetCommit().getCommitId();
                                            String pullSha = pr.getLastMergeSourceCommit().getCommitId();
                                            String mergeSha = pr.getLastMergeCommit().getCommitId();
                                            if (strategy == ChangeRequestCheckoutStrategy.MERGE) {
                                                GitRef baseRef = AzureConnector.INSTANCE.getRef(gitRepository, pr.getTargetRefName().replace("refs/", ""), false);
                                                if (baseRef != null) {
                                                    baseSha = baseRef.getObjectId();
                                                }
                                            }
                                            return new PullRequestSCMRevision(head, baseSha, pullSha, mergeSha);
                                        }
                                    },
                                    new MergabilityWitness(pr, strategy, listener),
                                    new CriteriaWitness(listener)
                            )) {
                                listener.getLogger().format("%n  %d pull requests were processed (query completed)%n", count);
                                break PRs;
                            }
                        }
                    }
                    listener.getLogger().format("%n  %d pull requests were processed%n", count);
                }
                if (request.isFetchTags() && !request.isComplete()) {
                    listener.getLogger().format("%n  Checking tags...%n");
                    int count = 0;
                    for (final GitRef tag : request.getTags()) {
                        if (!tag.isTag()) {
                            continue;
                        }
                        String tagName = tag.getTagName();
                        count++;
                        listener.getLogger().format("%n    Checking tag %s%n", HyperlinkNote.encodeTo(repositoryUrl + "?version=GT" + tagName, tagName));
                        long tagDate = 0L;
                        String sha = tag.getPeeledObjectId();
                        GitCommit commit = AzureConnector.INSTANCE.getCommit(gitRepository, sha);
                        if (commit != null) {
                            tagDate = commit.getPush().getDate().toInstant().toEpochMilli();
                        }
                        AzureDevOpsRepoTagSCMHead head = new AzureDevOpsRepoTagSCMHead(tagName, tagDate);
                        if (request.process(head, new GitTagSCMRevision(head, sha),
                                new SCMSourceRequest.ProbeLambda<AzureDevOpsRepoTagSCMHead, GitTagSCMRevision>() {
                                    @NonNull
                                    @Override
                                    public SCMSourceCriteria.Probe create(@NonNull AzureDevOpsRepoTagSCMHead head, @Nullable GitTagSCMRevision revisionInfo) {
                                        return new AzureDevOpsRepoSCMProbe(gitRepository, head, revisionInfo);
                                    }
                                }, new CriteriaWitness(listener))) {
                            listener.getLogger().format("%n  %d tags were processed (query completed)%n", count);
                            break;
                        }
                    }
                    listener.getLogger().format("%n  %d tags were processed%n", count);
                }
            }
            listener.getLogger().format("%nFinished examining %s%n%n", fullName);
        } catch (WrappedException e) {
            try {
                e.unwrap();
            } catch (RateLimitExceededException rle) {
                throw new AbortException(rle.getMessage());
            }
        }
    }

    @NonNull
    @Override
    protected Set<String> retrieveRevisions(@NonNull TaskListener listener) throws IOException, InterruptedException {
        StandardCredentials credentials = Connector.lookupScanCredentials((Item) getOwner(), collectionUrl, credentialsId);
        try {
            checkApiUrlValidity(credentials);
            Set<String> result = new TreeSet<>();
            try {
                AzureConnector.INSTANCE.checkConnectionValidity(collectionUrl, listener, credentials);
                if (StringUtils.isBlank(repository)) {
                    throw new AbortException("No repository selected, skipping");
                }
                String fullName = projectName + "/" + repository;
                gitRepository = AzureConnector.INSTANCE.getRepository(collectionUrl, credentials, projectName, repository);
                final GitRepositoryWithAzureContext gitRepository = this.gitRepository;
                listener.getLogger().format("Listing %s%n", HyperlinkNote.encodeTo(gitRepository.getGitRepository().getRemoteUrl(), fullName));
                repositoryUrl = new URL(gitRepository.getGitRepository().getRemoteUrl());
                AzureDevOpsRepoSCMSourceContext context = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none()).withTraits(traits);
                boolean wantBranches = context.wantBranches();
                boolean wantTags = context.wantTags();
                boolean wantPRs = context.wantPRs();
                boolean wantSinglePRs = context.forkPRStrategies().size() == 1 || context.originPRStrategies().size() == 1;
                boolean wantMultiPRs = context.forkPRStrategies().size() > 1 || context.originPRStrategies().size() > 1;
                Set<ChangeRequestCheckoutStrategy> strategies = new TreeSet<>();
                strategies.addAll(context.forkPRStrategies());
                strategies.addAll(context.originPRStrategies());
                List<GitRef> allRefs = AzureConnector.INSTANCE.listRefs(gitRepository, "", false);
                if (allRefs != null) {
                    for (GitRef ref : allRefs) {
                        if (ref.isBranch() && wantBranches) {
                            String branchName = ref.getBranchName();
                            listener.getLogger().format("%n  Found branch %s%n", HyperlinkNote.encodeTo(repositoryUrl + "?version=GB" + branchName, branchName));
                            result.add(branchName);
                            continue;
                        }
                        if (ref.isPullRequest() && wantPRs) {
                            int number = ref.getPullRequestNumber();
                            if (number != -1) {
                                listener.getLogger().format("%n  Found pull request %s%n", HyperlinkNote.encodeTo(repositoryUrl + "/pullrequest/" + number, "#" + number));
                                if (wantSinglePRs) {
                                    result.add("PR-" + number);
                                }
                                if (wantMultiPRs) {
                                    for (ChangeRequestCheckoutStrategy strategy : strategies) {
                                        result.add("PR-" + number + "-" + strategy.name().toLowerCase(Locale.ENGLISH));
                                    }
                                }
                            }
                            continue;
                        }
                        if (ref.isTag() && wantTags) {
                            String tagName = ref.getTagName();
                            listener.getLogger().format("%n  Found tag %s%n", HyperlinkNote.encodeTo(repositoryUrl + "?version=GT" + tagName, tagName));
                            result.add(tagName);
                            continue;
                        }
                    }
                }
                listener.getLogger().format("%nFinished listing %s%n%n", fullName);
            } catch (WrappedException e) {
                try {
                    e.unwrap();
                } catch (RateLimitExceededException rle) {
                    throw new AbortException(rle.getMessage());
                }
            }
            return result;
        } finally {
        }
    }

    @Override
    protected SCMRevision retrieve(@NonNull String headName, @NonNull TaskListener listener)
            throws IOException {
        StandardCredentials credentials = Connector.lookupScanCredentials((Item) getOwner(), collectionUrl, credentialsId);
        checkApiUrlValidity(credentials);
        if (StringUtils.isBlank(repository)) {
            throw new AbortException("No repository selected, skipping");
        }

        String fullName = projectName + "/" + repository;
        gitRepository = AzureConnector.INSTANCE.getRepository(collectionUrl, credentials, projectName, repository);
        final GitRepositoryWithAzureContext gitRepository = this.gitRepository;
        listener.getLogger().format("Examining %s%n", HyperlinkNote.encodeTo(gitRepository.getGitRepository().getRemoteUrl(), fullName));
        AzureDevOpsRepoSCMSourceContext context = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none()).withTraits(traits);
        Matcher prMatcher = Pattern.compile("^PR-(\\d+)(?:-(.*))?$").matcher(headName);
        if (prMatcher.matches()) {
            // it's a looking very much like a PR
            int number = Integer.parseInt(prMatcher.group(1));
            listener.getLogger().format("Attempting to resolve %s as pull request %d%n", headName, number);
            GitPullRequest pr = AzureConnector.INSTANCE.getPullRequest(gitRepository, number);
            if (pr != null) {
                boolean fork = gitRepository.getGitRepository().isFork();
                Set<ChangeRequestCheckoutStrategy> strategies;
                if (context.wantPRs()) {
                    strategies = fork
                            ? context.forkPRStrategies()
                            : context.originPRStrategies();
                } else {
                    // if not configured, we go with merge
                    strategies = EnumSet.of(ChangeRequestCheckoutStrategy.MERGE);
                }
                ChangeRequestCheckoutStrategy strategy;
                if (prMatcher.group(2) == null) {
                    if (strategies.size() == 1) {
                        strategy = strategies.iterator().next();
                    } else {
                        // invalid name
                        listener.getLogger().format(
                                "Resolved %s as pull request %d but indeterminate checkout strategy, "
                                        + "please try %s or %s%n",
                                headName,
                                number,
                                headName + "-" + ChangeRequestCheckoutStrategy.HEAD.name(),
                                headName + "-" + ChangeRequestCheckoutStrategy.MERGE.name()
                        );
                        return null;
                    }
                } else {
                    strategy = null;
                    for (ChangeRequestCheckoutStrategy s : strategies) {
                        if (s.name().toLowerCase(Locale.ENGLISH).equals(prMatcher.group(2))) {
                            strategy = s;
                            break;
                        }
                    }
                    if (strategy == null) {
                        // invalid name;
                        listener.getLogger().format(
                                "Resolved %s as pull request %d but unknown checkout strategy %s, "
                                        + "please try %s or %s%n",
                                headName,
                                number,
                                prMatcher.group(2),
                                headName + "-" + ChangeRequestCheckoutStrategy.HEAD.name(),
                                headName + "-" + ChangeRequestCheckoutStrategy.MERGE.name()
                        );
                        return null;
                    }
                }
                PullRequestSCMHead head = new PullRequestSCMHead(pr, headName, strategy == ChangeRequestCheckoutStrategy.MERGE);
                String baseSha = pr.getLastMergeTargetCommit().getCommitId();
                String pullSha = pr.getLastMergeSourceCommit().getCommitId();
                String mergeSha = pr.getLastMergeCommit().getCommitId();
                if (strategy == ChangeRequestCheckoutStrategy.MERGE) {
                    GitRef baseRef = AzureConnector.INSTANCE.getRef(gitRepository, pr.getTargetRefName().replace("refs/", ""), false);
                    if (baseRef != null) {
                        baseSha = baseRef.getObjectId();
                    }
                    listener.getLogger().format("Resolved %s as pull request %d at revision %s merged onto %s %n", headName, number, pullSha, baseSha);
                } else {
                    listener.getLogger().format("Resolved %s as pull request %d at revision %s%n", headName, number, pullSha);
                }
                return new PullRequestSCMRevision(head, baseSha, pullSha, mergeSha);
            } else {
                listener.getLogger().format("Could not resolve %s as pull request %d%n", headName, number);
            }
        }
        try {
            listener.getLogger().format("Attempting to resolve %s as a branch%n", headName);
            GitRef branch = AzureConnector.INSTANCE.getRef(gitRepository, "heads/" + headName, false);
            if (branch != null) {
                listener.getLogger().format("Resolved %s as branch %s at revision %s%n", headName, branch.getName(), branch.getObjectId());
                return new SCMRevisionImpl(new BranchSCMHead(headName, BranchSCMHead.RealBranchType.branch), branch.getObjectId());
            }
        } catch (Exception e) {
            // maybe it's a tag
        }
        try {
            listener.getLogger().format("Attempting to resolve %s as a tag%n", headName);
            GitRef tag = AzureConnector.INSTANCE.getRef(gitRepository, "tags/" + headName, true);
            if (tag != null) {
                long tagDate = 0L;
                String tagSha = tag.getPeeledObjectId();
                GitCommit commit = AzureConnector.INSTANCE.getCommit(gitRepository, tagSha);
                if (commit != null) {
                    tagDate = commit.getPush().getDate().toInstant().toEpochMilli();
                }
                listener.getLogger().format("Resolved %s as tag %s at revision %s%n", headName, headName, tagSha);
                return new GitTagSCMRevision(new AzureDevOpsRepoTagSCMHead(headName, tagDate), tagSha);
            }
        } catch (Exception e) {
            // ok it doesn't exist
        }
        listener.error("Could not resolve %s", headName);

        // TODO try and resolve as a revision, but right now we'd need to know what branch the revision belonged to
        // once GitSCMSource has support for arbitrary refs, we could just use that... but given that
        // GitHubSCMBuilder constructs the refspec based on the branch name, without a specific "arbitrary ref"
        // SCMHead subclass we cannot do anything here
        return null;
    }

    @NonNull
    private Set<String> updateCollaboratorNames(@NonNull TaskListener listener, @CheckForNull StandardCredentials credentials,
                                                @NonNull GitRepositoryWithAzureContext repo) {
        if (credentials == null) {
            listener.getLogger().println("Anonymous cannot query list of collaborators, assuming none");
            return collaboratorNames = Collections.emptySet();
        } else {
            try {
                //TODO Looks like we need list collaborators. TBD. - LUke
                //return collaboratorNames = new HashSet<>(repo.getCollaboratorNames());
                return collaboratorNames = new HashSet<>(new ArrayList<>());
            } catch (Exception e) {
                listener.getLogger().println("Not permitted to query list of collaborators, assuming none");
                return collaboratorNames = Collections.emptySet();
            }
        }
    }

    private void checkApiUrlValidity(StandardCredentials credentials) throws IOException {
        try {
            AzureConnector.INSTANCE.checkConnectionValidity(collectionUrl, credentials);
        } catch (Exception e) {
            String message = String.format("It seems %s is unreachable", collectionUrl);
            throw new IOException(message, e);
        }
    }

    private static class WrappedException extends RuntimeException {

        public WrappedException(Throwable cause) {
            super(cause);
        }

        public void unwrap() throws IOException, InterruptedException {
            Throwable cause = getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw this;
        }

    }

    @NonNull
    @Override
    protected SCMProbe createProbe(@NonNull SCMHead head, @CheckForNull final SCMRevision revision) {
        StandardCredentials credentials = Connector.lookupScanCredentials((Item) getOwner(), collectionUrl, credentialsId);
        final GitRepositoryWithAzureContext repo = AzureConnector.INSTANCE.getRepository(collectionUrl, credentials, projectName, repository);
        return new AzureDevOpsRepoSCMProbe(repo, head, revision);

    }

    @Override
    @CheckForNull
    protected SCMRevision retrieve(SCMHead head, TaskListener listener) throws IOException {
        StandardCredentials credentials = Connector.lookupScanCredentials((Item) getOwner(), collectionUrl, credentialsId);
        AzureConnector.INSTANCE.checkConnectionValidity(collectionUrl, listener, credentials);
        gitRepository = AzureConnector.INSTANCE.getRepository(collectionUrl, credentials, projectName, repository);
        repositoryUrl = new URL(gitRepository.getGitRepository().getRemoteUrl());
        if (head instanceof PullRequestSCMHead) {
            PullRequestSCMHead prhead = (PullRequestSCMHead) head;
            int number = prhead.getNumber();
            GitPullRequest pr = AzureConnector.INSTANCE.getPullRequest(gitRepository, number);
            String baseSha = pr.getLastMergeTargetCommit().getCommitId();
            String pullSha = pr.getLastMergeSourceCommit().getCommitId();
            String mergeSha = pr.getLastMergeCommit().getCommitId();
            if (prhead.getCheckoutStrategy() == ChangeRequestCheckoutStrategy.MERGE) {
                GitRef baseRef = AzureConnector.INSTANCE.getRef(gitRepository, pr.getTargetRefName().replace("refs/", ""), false);
                if (baseRef != null) {
                    baseSha = baseRef.getObjectId();
                }
            }
            return new PullRequestSCMRevision(prhead, baseSha, pullSha, mergeSha);
        } else if (head instanceof AzureDevOpsRepoTagSCMHead) {
            AzureDevOpsRepoTagSCMHead tagHead = (AzureDevOpsRepoTagSCMHead) head;
            GitRef tag = AzureConnector.INSTANCE.getRef(gitRepository, "tags/" + tagHead.getName(), true);
            if (tag != null) {
                return new GitTagSCMRevision(tagHead, tag.getPeeledObjectId());
            } else {
                throw new IOException("Tag " + tagHead.getName() + " cannot be found.");
            }
        } else {
            BranchSCMHead branchSCMHead = (BranchSCMHead) head;
            String filter;
            switch (branchSCMHead.realBranchType) {
                case branch:
                    filter = "heads/" + head.getName();
                    break;
                case pr:
                    filter = "pull/" + head.getName();
                    break;
                default:
                    filter = "tags/" + head.getName();
                    break;
            }
            GitRef branch = AzureConnector.INSTANCE.getRef(gitRepository, filter, true);
            if (branch != null) {
                if (branchSCMHead.realBranchType == BranchSCMHead.RealBranchType.tag) {
                    return new SCMRevisionImpl(head, branch.getPeeledObjectId());
                } else {
                    return new SCMRevisionImpl(head, branch.getObjectId());
                }
            } else {
                throw new IOException("Branch " + head.getName() + " cannot be found.");
            }
        }
    }

    @Override
    public SCM build(SCMHead head, SCMRevision revision) {
        return new AzureDevOpsRepoSCMBuilder(this, head, revision).withTraits(traits).build();
    }

    @CheckForNull
        /*package*/ URL getRepositoryUrl() {
        return repositoryUrl;
    }

    @Deprecated
        // TODO remove once migration from 1.x is no longer supported
    PullRequestSource retrievePullRequestSource(int number) {
        // we use a big honking great lock to prevent concurrent requests to github during job loading
        Map<Integer, PullRequestSource> pullRequestSourceMap;
        synchronized (pullRequestSourceMapLock) {
            pullRequestSourceMap = this.pullRequestSourceMap;
            if (pullRequestSourceMap == null) {
                this.pullRequestSourceMap = pullRequestSourceMap = new HashMap<>();
                if (!repository.isEmpty()) {
                    String fullName = projectName + "/" + repository;
                    LOGGER.log(Level.INFO, "Getting remote pull requests from {0}", fullName);
                    StandardCredentials credentials = Connector.lookupScanCredentials((Item) getOwner(), collectionUrl, credentialsId);
                    LogTaskListener listener = new LogTaskListener(LOGGER, Level.INFO);
                    try {
                        GitHub github = Connector.connect(collectionUrl, credentials);
                        try {
                            checkApiUrlValidity(credentials);
                            Connector.checkApiRateLimit(listener, github);
                            ghRepository = github.getRepository(fullName);
                            LOGGER.log(Level.INFO, "Got remote pull requests from {0}", fullName);
                            int n = 0;
                            for (GHPullRequest pr : ghRepository.queryPullRequests().state(GHIssueState.OPEN).list()) {
                                GHRepository repository = pr.getHead().getRepository();
                                // JENKINS-41246 repository may be null for deleted forks
                                pullRequestSourceMap.put(pr.getNumber(), new PullRequestSource(
                                        repository == null ? null : repository.getOwnerName(),
                                        repository == null ? null : repository.getName(),
                                        pr.getHead().getRef()));
                                n++;
                                if (n % 30 == 0) { // default page size is 30
                                    Connector.checkApiRateLimit(listener, github);
                                }
                            }
                        } finally {
                            Connector.release(github);
                        }
                    } catch (IOException | InterruptedException e) {
                        LOGGER.log(Level.WARNING, "Could not get all pull requests from " + fullName + ", there may be rebuilds", e);
                    }
                }
            }
            return pullRequestSourceMap.get(number);
        }
    }

    /**
     * Retained to migrate legacy configuration.
     *
     * @deprecated use {@link MergeWithGitSCMExtension}.
     */
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @Deprecated
    private static class MergeWith extends GitSCMExtension {
        private final String baseName;
        private final String baseHash;

        private MergeWith(String baseName, String baseHash) {
            this.baseName = baseName;
            this.baseHash = baseHash;
        }

        private Object readResolve() throws ObjectStreamException {
            return new MergeWithGitSCMExtension("remotes/origin/" + baseName, baseHash);
        }
    }

    @Override
    public SCMRevision getTrustedRevision(SCMRevision revision, final TaskListener listener)
            throws IOException, InterruptedException {
        if (revision instanceof PullRequestSCMRevision) {
            PullRequestSCMHead head = (PullRequestSCMHead) revision.getHead();

            try (AzureDevOpsRepoSCMSourceRequest request = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none())
                    .withTraits(traits)
                    .newRequest(this, listener)) {
                if (collaboratorNames != null) {
                    request.setCollaboratorNames(collaboratorNames);
                } else {
                    request.setCollaboratorNames(new DeferredContributorNames(listener));
                }
                request.setPermissionsSource(new DeferredPermissionsSource(listener));
                if (request.isTrusted(head)) {
                    return revision;
                }
            } catch (WrappedException wrapped) {
                try {
                    wrapped.unwrap();
                } catch (HttpException e) {
                    listener.getLogger().format("It seems %s is unreachable, assuming no trusted collaborators%n", collectionUrl);
                    collaboratorNames = Collections.singleton(projectName);
                }
            }
            PullRequestSCMRevision rev = (PullRequestSCMRevision) revision;
            listener.getLogger().format("Loading trusted files from base branch %s at %s rather than %s%n", head.getTarget().getName(), rev.getBaseHash(), rev.getPullHash());
            return new SCMRevisionImpl(head.getTarget(), rev.getBaseHash());
        }
        return revision;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isCategoryEnabled(@NonNull SCMHeadCategory category) {
        for (SCMSourceTrait trait : traits) {
            if (trait.isCategoryEnabled(category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMHead head,
                                           @CheckForNull SCMHeadEvent event,
                                           @NonNull TaskListener listener) {
        // TODO when we have support for trusted events, use the details from event if event was from trusted source
        List<Action> result = new ArrayList<>();
        SCMSourceOwner owner = getOwner();
        if (owner instanceof Actionable) {
            AzureDevOpsRepoLink repoLink = ((Actionable) owner).getAction(AzureDevOpsRepoLink.class);
            if (repoLink != null) {
                String url;
                ObjectMetadataAction metadataAction;
                if (head instanceof PullRequestSCMHead) {
                    // pull request to this repository
                    int number = ((PullRequestSCMHead) head).getNumber();
                    url = repoLink.getUrl() + "/pullrequest/" + number;
                    metadataAction = pullRequestMetadataCache.get(number);
                    if (metadataAction == null) {
                        // best effort
                        metadataAction = new ObjectMetadataAction(null, null, url);
                    }
                    if (pullRequestContributorCache != null) {
                        ContributorMetadataAction contributor = pullRequestContributorCache.get(number);
                        if (contributor != null) {
                            result.add(contributor);
                        }
                    }
                } else {
                    // branch in this repository
                    url = repoLink.getUrl() + "?version=GB" + head.getName();
                    metadataAction = new ObjectMetadataAction(head.getName(), null, url);
                }
                result.add(new AzureDevOpsRepoLink("icon-github-branch", url));
                result.add(metadataAction);
            }
            if (head instanceof BranchSCMHead) {
                for (AzureDevOpsRepoDefaultBranch p : ((Actionable) owner).getActions(AzureDevOpsRepoDefaultBranch.class)) {
                    if (StringUtils.equals(getProjectName(), p.getRepoOwner())
                            && StringUtils.equals(repository, p.getRepository())
                            && StringUtils.equals(p.getDefaultBranch(), head.getName())) {
                        result.add(new PrimaryInstanceMetadataAction());
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected List<Action> retrieveActions(@CheckForNull SCMSourceEvent event,
                                           @NonNull TaskListener listener) throws IOException {
        List<Action> result = new ArrayList<>();
        result.add(new AzureDevOpsRepoRepoMetadataAction());
        StandardCredentials credentials = AzureConnector.INSTANCE.lookupCredentials(getOwner(), collectionUrl, credentialsId);
        AzureConnector.INSTANCE.checkConnectionValidity(collectionUrl, listener, credentials);
        try {
            gitRepository = AzureConnector.INSTANCE.getRepository(collectionUrl, credentials, getProjectName(), repository);
            repositoryUrl = new URL(gitRepository.getGitRepository().getRemoteUrl());
        } catch (Exception e) {
            throw new AbortException(String.format("Invalid scan credentials when using %s to connect to %s/%s on %s", CredentialsNameProvider.name(credentials), projectName, repository, collectionUrl));
        }
        result.add(new ObjectMetadataAction(null, gitRepository.getGitRepository().getProject().getDescription(), Util.fixEmpty(gitRepository.getGitRepository().getRemoteUrl())));
        result.add(new AzureDevOpsRepoLink("icon-github-repo", gitRepository.getGitRepository().getRemoteUrl()));
        if (StringUtils.isNotBlank(gitRepository.getGitRepository().getDefaultBranch())) {
            result.add(new AzureDevOpsRepoDefaultBranch(getProjectName(), repository, gitRepository.getGitRepository().getDefaultBranch()));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterSave() {
        SCMSourceOwner owner = getOwner();
        if (owner != null) {
            GitHubWebHook.get().registerHookFor(owner);
        }
    }

    @Symbol("github")
    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {

        @Deprecated
        @Restricted(DoNotUse.class)
        @RestrictedSince("2.2.0")
        public static final String defaultIncludes = "*";
        @Deprecated
        @Restricted(DoNotUse.class)
        @RestrictedSince("2.2.0")
        public static final String defaultExcludes = "";
        public static final String ANONYMOUS = "ANONYMOUS";
        public static final String SAME = "SAME";
        // Prior to JENKINS-33161 the unconditional behavior was to build fork PRs plus origin branches, and try to build a merge revision for PRs.
        @Deprecated
        @Restricted(DoNotUse.class)
        @RestrictedSince("2.2.0")
        public static final boolean defaultBuildOriginBranch = true;
        @Deprecated
        @Restricted(DoNotUse.class)
        @RestrictedSince("2.2.0")
        public static final boolean defaultBuildOriginBranchWithPR = true;
        @Deprecated
        @Restricted(DoNotUse.class)
        @RestrictedSince("2.2.0")
        public static final boolean defaultBuildOriginPRMerge = false;
        @Deprecated
        @Restricted(DoNotUse.class)
        @RestrictedSince("2.2.0")
        public static final boolean defaultBuildOriginPRHead = false;
        @Deprecated
        @Restricted(DoNotUse.class)
        @RestrictedSince("2.2.0")
        public static final boolean defaultBuildForkPRMerge = true;
        @Deprecated
        @Restricted(DoNotUse.class)
        @RestrictedSince("2.2.0")
        public static final boolean defaultBuildForkPRHead = false;

        @Initializer(before = InitMilestone.PLUGINS_STARTED)
        public static void addAliases() {
            XSTREAM2.addCompatibilityAlias("org.jenkinsci.plugins.azure_devops_repo_branch_source.OriginAzureDevOpsRepoSCMSource", AzureDevOpsRepoSCMSource.class);
        }

        /**
         * Creates a list box model from a list of values.
         * ({@link ListBoxModel#ListBoxModel(Collection)} takes {@link ListBoxModel.Option}s,
         * not {@link String}s, and those are not {@link Comparable}.)
         */
        private static ListBoxModel nameAndValueModel(Collection<String> items) {
            ListBoxModel model = new ListBoxModel();
            for (String item : items) {
                model.add(item);
            }
            return model;
        }

        public ListBoxModel doFillCredentialsIdItems(@CheckForNull @AncestorInPath Item context,
                                                     @QueryParameter String collectionUrl,
                                                     @QueryParameter String credentialsId) {
            if (context == null
                    ? !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
                    : !context.hasPermission(Item.EXTENDED_READ)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return AzureConnector.INSTANCE.listCredentials(context, collectionUrl);
        }

        @Override
        public String getDisplayName() {
            return Messages.AzureDevOpsRepoSCMSource_DisplayName();
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckIncludes(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.warning(Messages.AzureDevOpsRepoSCMSource_did_you_mean_to_use_to_match_all_branches());
            }
            return FormValidation.ok();
        }

        @RequirePOST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckCredentialsId(@CheckForNull @AncestorInPath Item context,
                                                   @QueryParameter String collectionUrl,
                                                   @QueryParameter String value) {
            return AzureConnector.INSTANCE.checkCredentials(context, collectionUrl, value);
        }

        @RequirePOST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckProjectName(@CheckForNull @AncestorInPath Item context,
                                                 @QueryParameter String collectionUrl,
                                                 @QueryParameter String credentialsId,
                                                 @QueryParameter String value) {
            return AzureConnector.INSTANCE.checkProjectName(context, collectionUrl, credentialsId, value);
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckRepository(@CheckForNull @AncestorInPath Item context,
                                                @QueryParameter String collectionUrl,
                                                @QueryParameter String credentialsId,
                                                @QueryParameter String projectName,
                                                @QueryParameter String value) {
            return AzureConnector.INSTANCE.checkRepository(context, collectionUrl, credentialsId, projectName, value);
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckBuildOriginPRHead(@QueryParameter boolean buildOriginBranchWithPR, @QueryParameter boolean buildOriginPRMerge, @QueryParameter boolean buildOriginPRHead) {
            if (buildOriginBranchWithPR && buildOriginPRHead) {
                return FormValidation.warning("Redundant to build an origin PR both as a branch and as an unmerged PR.");
            }
            if (buildOriginPRMerge && buildOriginPRHead) {
                return FormValidation.ok("Merged vs. unmerged PRs will be distinguished in the job name (*-merge vs. *-head).");
            }
            return FormValidation.ok();
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckBuildOriginBranchWithPR(
                @QueryParameter boolean buildOriginBranch,
                @QueryParameter boolean buildOriginBranchWithPR,
                @QueryParameter boolean buildOriginPRMerge,
                @QueryParameter boolean buildOriginPRHead,
                @QueryParameter boolean buildForkPRMerge,
                @QueryParameter boolean buildForkPRHead
        ) {
            if (buildOriginBranch && !buildOriginBranchWithPR && !buildOriginPRMerge && !buildOriginPRHead && !buildForkPRMerge && !buildForkPRHead) {
                // TODO in principle we could make doRetrieve populate originBranchesWithPR without actually including any PRs, but it would be more work and probably never wanted anyway.
                return FormValidation.warning("If you are not building any PRs, all origin branches will be built.");
            }
            return FormValidation.ok();
        }

        @RequirePOST
        public ListBoxModel doFillProjectNameItems(@CheckForNull @AncestorInPath Item context, @QueryParameter String collectionUrl, @QueryParameter String credentialsId) {
            if (credentialsId == null || credentialsId.isEmpty() || collectionUrl == null || collectionUrl.isEmpty()) {
                return new ListBoxModel();
            }
            ListBoxModel result = new ListBoxModel();
            List<String> projectNameList = AzureConnector.INSTANCE.listProjectNames(context, collectionUrl, credentialsId);
            if (projectNameList != null) {
                result.add("- none -", "");
                for (String name : projectNameList) {
                    result.add(name, name);
                }
            }
            return result;
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckBuildForkPRHead/* web method name controls UI position of message; we want this at the bottom */(
                @QueryParameter boolean buildOriginBranch,
                @QueryParameter boolean buildOriginBranchWithPR,
                @QueryParameter boolean buildOriginPRMerge,
                @QueryParameter boolean buildOriginPRHead,
                @QueryParameter boolean buildForkPRMerge,
                @QueryParameter boolean buildForkPRHead
        ) {
            if (!buildOriginBranch && !buildOriginBranchWithPR && !buildOriginPRMerge && !buildOriginPRHead && !buildForkPRMerge && !buildForkPRHead) {
                return FormValidation.warning("You need to build something!");
            }
            if (buildForkPRMerge && buildForkPRHead) {
                return FormValidation.ok("Merged vs. unmerged PRs will be distinguished in the job name (*-merge vs. *-head).");
            }
            return FormValidation.ok();
        }

        @RequirePOST
        public ListBoxModel doFillRepositoryItems(@CheckForNull @AncestorInPath Item context, @QueryParameter String collectionUrl, @QueryParameter String credentialsId, @QueryParameter String projectName) {
            if (credentialsId == null || credentialsId.isEmpty() || collectionUrl == null || collectionUrl.isEmpty() || projectName == null || projectName.isEmpty()) {
                return new ListBoxModel();
            }
            if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) || context != null && !context.hasPermission(Item.EXTENDED_READ)) {
                return new ListBoxModel(); // not supposed to be seeing this form
            }
            if (context != null && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                return new ListBoxModel(); // not permitted to try connecting with these credentials
            }
            ListBoxModel result = new ListBoxModel();
            List<String> repositoryNameList = AzureConnector.INSTANCE.listRepositoryNames(context, collectionUrl, credentialsId, projectName);
            if (repositoryNameList != null) {
                result.add("- none -", "");
                for (String repositoryName : repositoryNameList) {
                    result.add(repositoryName, repositoryName);
                }
            }
            return result;
        }

        public List<NamedArrayList<? extends SCMTraitDescriptor<?>>> getTraitsDescriptorLists() {
            List<SCMTraitDescriptor<?>> all = new ArrayList<>();
            all.addAll(SCMSourceTrait._for(this, AzureDevOpsRepoSCMSourceContext.class, null));
            all.addAll(SCMSourceTrait._for(this, null, AzureDevOpsRepoSCMBuilder.class));
            Set<SCMTraitDescriptor<?>> dedup = new HashSet<>();
            for (Iterator<SCMTraitDescriptor<?>> iterator = all.iterator(); iterator.hasNext(); ) {
                SCMTraitDescriptor<?> d = iterator.next();
                if (dedup.contains(d) || d instanceof GitBrowserSCMSourceTrait.DescriptorImpl) {
                    iterator.remove();
                } else {
                    dedup.add(d);
                }
            }
            List<NamedArrayList<? extends SCMTraitDescriptor<?>>> result = new ArrayList<>();
            NamedArrayList.select(all, "Within repository", NamedArrayList.anyOf(NamedArrayList.withAnnotation(Discovery.class),
                    NamedArrayList.withAnnotation(Selection.class)), true, result);
            NamedArrayList.select(all, "General", null, true, result);
            return result;
        }

        public List<SCMSourceTrait> getTraitsDefaults() {
            return Arrays.asList( // TODO finalize
                    new BranchDiscoveryTrait(true, false),
                    new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE)),
                    new ForkPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE), new ForkPullRequestDiscoveryTrait.TrustPermission())
            );
        }

        @NonNull
        @Override
        protected SCMHeadCategory[] createCategories() {
            return new SCMHeadCategory[]{
                    new UncategorizedSCMHeadCategory(Messages._AzureDevOpsRepoSCMSource_UncategorizedCategory()),
                    new ChangeRequestSCMHeadCategory(Messages._AzureDevOpsRepoSCMSource_ChangeRequestCategory()),
                    new TagSCMHeadCategory(Messages._AzureDevOpsRepoSCMSource_TagCategory())
            };
        }
    }

    private static class LazyBranchesAzure extends LazyIterable<GitRef> {
        private final AzureDevOpsRepoSCMSourceRequest request;
        private final GitRepositoryWithAzureContext repo;

        public LazyBranchesAzure(AzureDevOpsRepoSCMSourceRequest request, GitRepositoryWithAzureContext repo) {
            this.request = request;
            this.repo = repo;
        }

        @Override
        protected Iterable<GitRef> create() {
            try {
                request.checkApiRateLimit();
                Set<String> branchNames = request.getRequestedOriginBranchNames();
                if (branchNames != null && branchNames.size() == 1) {
                    String branchName = branchNames.iterator().next();
                    request.listener().getLogger().format("%n  Getting remote branch %s...%n", branchName);
                    GitRef branch = AzureConnector.INSTANCE.getRef(repo, branchName, false);
                    if (branch != null) {
                        return Collections.singletonList(branch);
                    } else {
                        return Collections.emptyList();
                    }
                }
                request.listener().getLogger().format("%n  Getting remote branches...%n");
                List<GitRef> values = AzureConnector.INSTANCE.listBranches(repo);
                //TODO We may treat PR as branch. But since we can detect PR separately we won't do that. - luke
//                List<GitRef> values2 = AzureConnector.INSTANCE.listPullRequestsAsRefs(repo);
//                if (values != null && values2 != null) {
//                    values.addAll(values2);
//                }
                //TODO end
                final String defaultBranch = StringUtils.defaultIfBlank(repo.getGitRepository().getDefaultBranch(), "master");
                Collections.sort(values, new Comparator<GitRef>() {
                    @Override
                    public int compare(GitRef o1, GitRef o2) {
                        if (defaultBranch.equals(o1.getName())) {
                            return -1;
                        }
                        if (defaultBranch.equals(o2.getName())) {
                            return 1;
                        }
                        return 0;
                    }
                });
                return values;
            } catch (Exception e) {
                throw new AzureDevOpsRepoSCMSource.WrappedException(e);
            }
        }
    }

    private static class LazyTagsAzure extends LazyIterable<GitRef> {
        private final AzureDevOpsRepoSCMSourceRequest request;
        private final GitRepositoryWithAzureContext repo;

        public LazyTagsAzure(AzureDevOpsRepoSCMSourceRequest request, GitRepositoryWithAzureContext repo) {
            this.request = request;
            this.repo = repo;
        }

        @Override
        protected Iterable<GitRef> create() {
            Set<String> tagNames = request.getRequestedTagNames();
            if (tagNames != null && tagNames.size() == 1) {
                String tagName = tagNames.iterator().next();
                request.listener().getLogger().format("%n  Getting remote tag %s...%n", tagName);
                return Collections.singletonList(AzureConnector.INSTANCE.getRef(repo, "tags/" + tagName, true));
            }
            request.listener().getLogger().format("%n  Getting remote tags...%n");
            final Iterable<GitRef> iterable = AzureConnector.INSTANCE.listTags(repo);
            return new Iterable<GitRef>() {
                @Override
                public Iterator<GitRef> iterator() {
                    final Iterator<GitRef> iterator;
                    try {
                        iterator = iterable.iterator();
                    } catch (Error e) {
                        return Collections.<GitRef>emptyList().iterator();
                    }
                    return new Iterator<GitRef>() {
                        boolean hadAtLeastOne;

                        @Override
                        public boolean hasNext() {
                            try {
                                boolean hasNext = iterator.hasNext();
                                hadAtLeastOne = hadAtLeastOne || hasNext;
                                return hasNext;
                            } catch (Error e) {
                                return false;
                            }
                        }

                        @Override
                        public GitRef next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            return iterator.next();
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }
                    };
                }
            };
        }
    }

    private static class MergabilityWitness
            implements SCMSourceRequest.Witness<PullRequestSCMHead, PullRequestSCMRevision> {
        private final GitPullRequest pr;
        private final ChangeRequestCheckoutStrategy strategy;
        private final TaskListener listener;

        public MergabilityWitness(GitPullRequest pr, ChangeRequestCheckoutStrategy strategy, TaskListener listener) {
            this.pr = pr;
            this.strategy = strategy;
            this.listener = listener;
        }

        @Override
        public void record(@NonNull PullRequestSCMHead head, PullRequestSCMRevision revision, boolean isMatch) {
            if (isMatch) {
                Boolean mergeable;
                mergeable = (pr.getMergeStatus() == PullRequestAsyncStatus.succeeded);
                if (Boolean.FALSE.equals(mergeable)) {
                    if (strategy == ChangeRequestCheckoutStrategy.MERGE) {
                        listener.getLogger().format("      Not mergeable, build likely to fail%n");
                    } else {
                        listener.getLogger().format("      Not mergeable, but will be built anyway%n");
                    }
                }
            }
        }
    }

    private static class CriteriaWitness implements SCMSourceRequest.Witness {
        private final TaskListener listener;

        public CriteriaWitness(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        public void record(@NonNull SCMHead head, SCMRevision revision, boolean isMatch) {
            if (isMatch) {
                listener.getLogger().format("    Met criteria%n");
            } else {
                listener.getLogger().format("    Does not meet criteria%n");
            }
        }
    }

    private class LazyPullRequestsAzure extends LazyIterable<GitPullRequest> implements Closeable {
        private final AzureDevOpsRepoSCMSourceRequest request;
        private final GitRepositoryWithAzureContext repo;
        private Set<Integer> pullRequestMetadataKeys = new HashSet<>();
        private boolean fullScanRequested = false;
        private boolean iterationCompleted = false;

        public LazyPullRequestsAzure(AzureDevOpsRepoSCMSourceRequest request, GitRepositoryWithAzureContext repo) {
            this.request = request;
            this.repo = repo;
        }

        @Override
        protected Iterable<GitPullRequest> create() {
            try {
                request.checkApiRateLimit();
                Set<Integer> prs = request.getRequestedPullRequestNumbers();
                if (prs != null && prs.size() == 1) {
                    Integer number = prs.iterator().next();
                    request.listener().getLogger().format("%n  Getting remote pull request #%d...%n", number);
                    GitPullRequest pullRequest = AzureConnector.INSTANCE.getPullRequest(repo, number);
                    if (pullRequest != null && pullRequest.getStatus() != PullRequestStatus.active) {
                        return Collections.emptyList();
                    }
                    return new CacheUdatingIterable(Collections.singletonList(pullRequest));
                }
                Set<String> branchNames = request.getRequestedOriginBranchNames();
                if (branchNames != null && branchNames.size() == 1) {
                    String branchName = branchNames.iterator().next();
                    request.listener().getLogger().format("%n  Getting remote pull requests from branch %s...%n", branchName);
                    return new CacheUdatingIterable(AzureConnector.INSTANCE.listPullRequests(repo, PullRequestStatus.active, branchName));
                }
                request.listener().getLogger().format("%n  Getting remote pull requests...%n");
                fullScanRequested = true;
                return new CacheUdatingIterable(AzureConnector.INSTANCE.listPullRequests(repo, PullRequestStatus.active, null));
            } catch (IOException | InterruptedException e) {
                throw new AzureDevOpsRepoSCMSource.WrappedException(e);
            }
        }

        @Override
        public void close() {
            if (fullScanRequested && iterationCompleted) {
                if (pullRequestMetadataCache != null) {
                    pullRequestMetadataCache.keySet().retainAll(pullRequestMetadataKeys);
                }
                if (pullRequestContributorCache != null) {
                    pullRequestContributorCache.keySet().retainAll(pullRequestMetadataKeys);
                }
                if (Jenkins.get().getInitLevel().compareTo(InitMilestone.JOB_LOADED) > 0) {
                    synchronized (pullRequestSourceMapLock) {
                        pullRequestSourceMap = null;
                    }
                }
            }
        }

        private class CacheUdatingIterable extends SinglePassIterable<GitPullRequest> {

            CacheUdatingIterable(Iterable<GitPullRequest> delegate) {
                super(delegate);
            }

            @Override
            public void observe(GitPullRequest pr) {
                int number = pr.getPullRequestId();
                try {
                    pullRequestMetadataCache.put(number,
                            new ObjectMetadataAction(
                                    pr.getTitle(),
                                    pr.getDescription(),
                                    new URL(pr.getUrl()).toExternalForm()
                            )
                    );
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                pullRequestMetadataKeys.add(number);
            }

            @Override
            public void completed() {
                iterationCompleted = true;
            }
        }
    }

    private class LazyContributorNames extends LazySet<String> {
        private final TaskListener listener;
        private final GitRepositoryWithAzureContext repo;
        private final StandardCredentials credentials;

        public LazyContributorNames(TaskListener listener, GitRepositoryWithAzureContext repo, StandardCredentials credentials) {
            this.listener = listener;
            this.repo = repo;
            this.credentials = credentials;
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        protected Set<String> create() {
            return updateCollaboratorNames(listener, credentials, repo);
        }
    }

    private class DeferredContributorNames extends LazySet<String> {
        private final TaskListener listener;

        public DeferredContributorNames(TaskListener listener) {
            this.listener = listener;
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        protected Set<String> create() {
            if (collaboratorNames != null) {
                return collaboratorNames;
            }
            listener.getLogger().format("Connecting to %s to obtain list of collaborators for %s/%s%n", collectionUrl, projectName, repository);
            StandardCredentials credentials = Connector.lookupScanCredentials((Item) getOwner(), collectionUrl, credentialsId);
            try {
                checkApiUrlValidity(credentials);
                AzureConnector.INSTANCE.checkConnectionValidity(collectionUrl, listener, credentials);
                String credentialsName = credentials == null ? "anonymous access" : CredentialsNameProvider.name(credentials);
                listener.getLogger().format("Connecting to %s using %s%n", collectionUrl, credentialsName);
                if (repository.isEmpty()) {
                    collaboratorNames = Collections.singleton(projectName);
                } else {
                    gitRepository = AzureConnector.INSTANCE.getRepository(collectionUrl, credentials, projectName, repository);
                    repositoryUrl = new URL(gitRepository.getGitRepository().getRemoteUrl());
                    return new LazyContributorNames(listener, gitRepository, credentials);
                }
                return collaboratorNames;
            } catch (IOException e) {
                throw new WrappedException(e);
            }
        }
    }

    private class DeferredPermissionsSource extends AzureDevOpsRepoPermissionsSource implements Closeable {

        private final TaskListener listener;
        private GitRepository repo;

        public DeferredPermissionsSource(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        public AzurePermissionType fetch(String username) {
            if (repo == null) {
                listener.getLogger().format("Connecting to %s to check permissions of obtain list of %s for %s/%s%n", collectionUrl, username, projectName, repository);
                StandardCredentials credentials = AzureConnector.INSTANCE.lookupCredentials(getOwner(), collectionUrl, credentialsId);
            }
            //return repo.getPermission(username);
            return AzurePermissionType.ADMIN;
        }

        @Override
        public void close() {
        }
    }
}
