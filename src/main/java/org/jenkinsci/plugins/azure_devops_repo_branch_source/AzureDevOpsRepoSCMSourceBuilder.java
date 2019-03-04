/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
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
import jenkins.scm.api.trait.SCMSourceBuilder;

/**
 * A {@link SCMSourceBuilder} that builds {@link AzureDevOpsRepoSCMSource} instances
 *
 * @since 2.2.0
 */
public class AzureDevOpsRepoSCMSourceBuilder extends SCMSourceBuilder<AzureDevOpsRepoSCMSourceBuilder, AzureDevOpsRepoSCMSource> {
    /**
     * The {@link AzureDevOpsRepoSCMSource#getId()}.
     */
    @CheckForNull
    private final String id;
    /**
     * The {@link AzureDevOpsRepoSCMSource#getApiUri()}.
     */
    @CheckForNull
    private final String apiUri;
    /**
     * The credentials id or {@code null} to use anonymous scanning.
     */
    @CheckForNull
    private final String credentialsId;
    /**
     * The repository owner.
     */
    @NonNull
    private final String repoOwner;

    /**
     * Constructor.
     *
     * @param id            the {@link AzureDevOpsRepoSCMSource#getId()}
     * @param apiUri        the {@link AzureDevOpsRepoSCMSource#getApiUri()}
     * @param credentialsId the credentials id.
     * @param repoOwner     the repository owner.
     * @param repoName      the project name.
     */
    public AzureDevOpsRepoSCMSourceBuilder(@CheckForNull String id, @CheckForNull String apiUri,
                                           @CheckForNull String credentialsId, @NonNull String repoOwner,
                                           @NonNull String repoName) {
        super(AzureDevOpsRepoSCMSource.class, repoName);
        this.id = id;
        this.apiUri = apiUri;
        this.repoOwner = repoOwner;
        this.credentialsId = credentialsId;
    }

    /**
     * The id of the {@link AzureDevOpsRepoSCMSource} that is being built.
     *
     * @return the id of the {@link AzureDevOpsRepoSCMSource} that is being built.
     */
    public final String id() {
        return id;
    }

    /**
     * The endpoint of the {@link AzureDevOpsRepoSCMSource} that is being built.
     *
     * @return the endpoint of the {@link AzureDevOpsRepoSCMSource} that is being built.
     */
    @CheckForNull
    public final String apiUri() {
        return apiUri;
    }

    /**
     * The credentials that the {@link AzureDevOpsRepoSCMSource} will use.
     *
     * @return the credentials that the {@link AzureDevOpsRepoSCMSource} will use.
     */
    @CheckForNull
    public final String credentialsId() {
        return credentialsId;
    }

    /**
     * The repository owner that the {@link AzureDevOpsRepoSCMSource} will be configured to use.
     *
     * @return the repository owner that the {@link AzureDevOpsRepoSCMSource} will be configured to use.
     */
    @NonNull
    public final String repoOwner() {
        return repoOwner;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public AzureDevOpsRepoSCMSource build() {
        AzureDevOpsRepoSCMSource result = new AzureDevOpsRepoSCMSource(repoOwner, projectName());
        result.setId(id());
        result.setApiUri(apiUri());
        result.setCredentialsId(credentialsId());
        result.setTraits(traits());
        return result;
    }
}
