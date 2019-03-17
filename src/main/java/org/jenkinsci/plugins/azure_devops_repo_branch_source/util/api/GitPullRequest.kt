package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName
import java.util.*

data class GitPullRequest(
        @SerializedName("_links") val links: ReferenceLinks,
        val artifactId: String,
        val autoCompleteSetBy: IdentityRef,
        val closedBy: IdentityRef,
        val closedDate: Date,
        val codeReviewId: Int,
        val commits: List<GitCommitRef>,
        val completionOptions: GitPullRequestCompletionOptions,
        val completionQueueTime: String,
        val createdBy: IdentityRef,
        val creationDate: Date,
        val description: String,
//        val forkSource: GitForkRef,
        val isDraft: Boolean,
//        val labels: List<WebApiTagDefinition>,
        val lastMergeCommit: GitCommitRef,
        val lastMergeSourceCommit: GitCommitRef,
        val lastMergeTargetCommit: GitCommitRef,
        val mergeFailureMessage: String,
        val mergeFailureType: PullRequestMergeFailureType,
        val mergeId: String,
        val mergeOptions: GitPullRequestMergeOptions,
        val mergeStatus: PullRequestAsyncStatus,
        val pullRequestId: Int,
        val remoteUrl: String,
        val repository: GitRepository,
//        val reviewers: List<IdentityRefWithVote>,
        val sourceRefName: String,
        val status: PullRequestStatus,
        val supportsIterations: Boolean,
        val targetRefName: String,
        val title: String,
        val url: String,
        val workItemRefs: List<ResourceRef>
)
