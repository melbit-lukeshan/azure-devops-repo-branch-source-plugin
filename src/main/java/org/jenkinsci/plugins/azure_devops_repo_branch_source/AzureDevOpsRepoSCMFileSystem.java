/*
 * The MIT License
 *
 * Copyright (c) 2016-2017 CloudBees, Inc.
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
 *
 */

package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.*;
import org.apache.commons.lang.time.FastDateFormat;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.AzureConnector;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitCommit;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitCommitRef;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitRef;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitRepositoryWithAzureContext;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Implements {@link SCMFileSystem} for Azure DevOps.
 */
public class AzureDevOpsRepoSCMFileSystem extends SCMFileSystem implements AzureDevOpsRepoClosable {
    private final GitRepositoryWithAzureContext repo;
    private final String ref;
    private boolean open;

    /**
     * Constructor.
     *
     * @param repo    the {@link GitRepositoryWithAzureContext}
     * @param refName the ref name, e.g. {@code heads/branchName}, {@code tags/tagName}, {@code pull/N/head} or the SHA.
     * @param rev     the optional revision.
     */
    protected AzureDevOpsRepoSCMFileSystem(GitRepositoryWithAzureContext repo, String refName, @CheckForNull SCMRevision rev) {
        super(rev);
        //TODO Debug - Luke
        System.out.println("AzureDevOpsRepoSCMFileSystem refName " + refName);
        System.out.println("AzureDevOpsRepoSCMFileSystem rev " + rev);
        this.open = true;
        this.repo = repo;
        if (rev != null) {
            if (rev.getHead() instanceof PullRequestSCMHead) {
                System.out.println("AzureDevOpsRepoSCMFileSystem PullRequestSCMHead");
                PullRequestSCMHead pr = (PullRequestSCMHead) rev.getHead();
                this.ref = ((PullRequestSCMRevision) rev).getPullHash();
            } else if (rev instanceof AbstractGitSCMSource.SCMRevisionImpl) {
                System.out.println("AzureDevOpsRepoSCMFileSystem AbstractGitSCMSource.SCMRevisionImpl");
                this.ref = ((AbstractGitSCMSource.SCMRevisionImpl) rev).getHash();
            } else {
                System.out.println("AzureDevOpsRepoSCMFileSystem other head");
                this.ref = refName;
            }
        } else {
            this.ref = refName;
        }
        System.out.println("AzureDevOpsRepoSCMFileSystem ref " + ref);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isOpen() {
        return open;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() {
        // Note: ref is a SHA1 string
        if (ref != null) {
            GitCommit commit = AzureConnector.INSTANCE.getCommit(repo, ref);
            if (commit != null) {
                //TODO Debug - Luke
                System.out.println("AzureDevOpsRepoSCMFileSystem lastModified " + commit.getPush().getDate().toInstant().toEpochMilli());
                return commit.getPush().getDate().toInstant().toEpochMilli();
            }
        }
        //TODO Debug - Luke
        System.out.println("AzureDevOpsRepoSCMFileSystem lastModified 0");
        return 0L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean changesSince(SCMRevision revision, @NonNull OutputStream changeLogStream) throws UnsupportedOperationException, IOException {
        //TODO Debug - Luke
        System.out.println("AzureDevOpsRepoSCMFileSystem changesSince revision " + revision);
        System.out.println("AzureDevOpsRepoSCMFileSystem changesSince getRevision() " + getRevision());
        if (Objects.equals(getRevision(), revision)) {
            // special case where somebody is asking one of two stupid questions:
            // 1. what has changed between the latest and the latest
            // 2. what has changed between the current revision and the current revision
            System.out.println("AzureDevOpsRepoSCMFileSystem changesSince revision == getRevision()");
            return false;
        }
        int count = 0;
        FastDateFormat iso = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssZ");
        StringBuilder log = new StringBuilder(1024);
        String endHash;
        if (revision instanceof AbstractGitSCMSource.SCMRevisionImpl) {
            endHash = ((AbstractGitSCMSource.SCMRevisionImpl) revision).getHash().toLowerCase(Locale.ENGLISH);
        } else {
            endHash = null;
        }
        // this is the format expected by GitSCM, so we need to format each GHCommit with the same format
        // commit %H%ntree %T%nparent %P%nauthor %aN <%aE> %ai%ncommitter %cN <%cE> %ci%n%n%w(76,4,4)%s%n%n%b
        List<GitCommitRef> commitRefList = AzureConnector.INSTANCE.listCommits(repo);
        if (commitRefList != null) {
            for (GitCommitRef commit : commitRefList) {
                if (commit.getCommitId().toLowerCase(Locale.ENGLISH).equals(endHash)) {
                    break;
                }
                log.setLength(0);
                log.append("commit ").append(commit.getCommitId()).append('\n');
                log.append("tree ").append(" ").append('\n');
                log.append("parent");
                if (commit.getParents() != null) {
                    for (String parent : commit.getParents()) {
                        log.append(' ').append(parent);
                    }
                }
                log.append('\n');
                log.append("author ")
                        .append(commit.getAuthor().getName())
                        .append(" <")
                        .append(commit.getAuthor().getEmail())
                        .append("> ")
                        .append(iso.format(commit.getAuthor().getDate()))
                        .append('\n');
                log.append("committer ")
                        .append(commit.getCommitter().getName())
                        .append(" <")
                        .append(commit.getCommitter().getEmail())
                        .append("> ")
                        .append(iso.format(commit.getCommitter().getDate()))
                        .append('\n');
                log.append('\n');
                String msg = commit.getComment();
                if (msg.endsWith("\r\n")) {
                    msg = msg.substring(0, msg.length() - 2);
                } else if (msg.endsWith("\n")) {
                    msg = msg.substring(0, msg.length() - 1);
                }
                msg = msg.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\n    ");
                log.append("    ").append(msg).append('\n');
                changeLogStream.write(log.toString().getBytes(StandardCharsets.UTF_8));
                changeLogStream.flush();
                count++;
                if (count >= GitSCM.MAX_CHANGELOG) {
                    break;
                }
            }
        }
        System.out.println("AzureDevOpsRepoSCMFileSystem changesSince count " + count);
        return count > 0;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public SCMFile getRoot() {
        return new AzureDevOpsRepoSCMFile(this, repo, ref);
    }

    @Extension
    public static class BuilderImpl extends SCMFileSystem.Builder {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean supports(SCM source) {
            return false;
        }

        @Override
        protected boolean supportsDescriptor(SCMDescriptor scmDescriptor) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean supports(SCMSource source) {
            return source instanceof AzureDevOpsRepoSCMSource;
        }

        @Override
        protected boolean supportsDescriptor(SCMSourceDescriptor scmSourceDescriptor) {
            return scmSourceDescriptor instanceof AzureDevOpsRepoSCMSource.DescriptorImpl;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCMFileSystem build(@NonNull Item owner, @NonNull SCM scm, @CheckForNull SCMRevision rev) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCMFileSystem build(@NonNull SCMSource source, @NonNull SCMHead head, @CheckForNull SCMRevision rev) {
            AzureDevOpsRepoSCMSource src = (AzureDevOpsRepoSCMSource) source;
            String collectionUrl = src.getCollectionUrl();
            StandardCredentials credentials = AzureConnector.INSTANCE.lookupCredentials(src.getOwner(), collectionUrl, src.getCredentialsId());
            GitRepositoryWithAzureContext repo = AzureConnector.INSTANCE.getRepository(collectionUrl, credentials, src.getProjectName(), src.getRepository());
            if (repo != null) {
                String refName;
                //TODO Debug - Luke
                System.out.println("SCMFileSystem BuilderImpl head " + head);
                if (head instanceof BranchSCMHead) {
                    refName = "heads/" + head.getName();
                    System.out.println("SCMFileSystem BuilderImpl BranchSCMHead");
                } else if (head instanceof AzureDevOpsRepoTagSCMHead) {
                    refName = "tags/" + head.getName();
                    System.out.println("SCMFileSystem BuilderImpl AzureDevOpsRepoTagSCMHead");
                } else if (head instanceof PullRequestSCMHead) {
                    PullRequestSCMHead pr = (PullRequestSCMHead) head;
                    refName = "pull/" + pr.getNumber() + "/merge";
                    System.out.println("SCMFileSystem BuilderImpl PullRequestSCMHead");
                    System.out.println("SCMFileSystem BuilderImpl PullRequestSCMHead getSourceBranch() " + pr.getSourceBranch());
                    System.out.println("SCMFileSystem BuilderImpl PullRequestSCMHead getOriginName() " + pr.getOriginName());
//                    if (!pr.isMerge() && pr.getSourceRepo() != null) {
//                        return new AzureDevOpsRepoSCMFileSystem(repo, pr.getSourceBranch(), rev);
//                    }
//                    return null; // TODO support merge revisions somehow
                } else {
                    System.out.println("SCMFileSystem BuilderImpl return null");
                    return null;
                }

                System.out.println("SCMFileSystem BuilderImpl refName " + refName);

                if (rev == null) {
                    System.out.println("SCMFileSystem BuilderImpl rev is null");
                    GitRef ref = AzureConnector.INSTANCE.getRef(repo, refName, true);
                    if (ref != null) {
                        if (head instanceof BranchSCMHead) {
                            rev = new AbstractGitSCMSource.SCMRevisionImpl(head, ref.getObjectId());
                        } else if (head instanceof AzureDevOpsRepoTagSCMHead) {
                            rev = new GitTagSCMRevision((AzureDevOpsRepoTagSCMHead) head, ref.getPeeledObjectId());
                        } else if (head instanceof PullRequestSCMHead) {
                            rev = new AbstractGitSCMSource.SCMRevisionImpl(head, ref.getObjectId());
                        }
                    }
                    System.out.println("SCMFileSystem BuilderImpl rev " + rev);
                } else {
                    System.out.println("SCMFileSystem BuilderImpl rev is not null");
                }
                return new AzureDevOpsRepoSCMFileSystem(repo, refName, rev);
            }
            return null;
        }
    }
}
