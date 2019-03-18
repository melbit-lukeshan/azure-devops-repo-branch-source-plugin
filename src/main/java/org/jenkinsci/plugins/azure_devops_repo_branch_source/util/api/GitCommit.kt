package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName

data class GitCommit(
        @SerializedName("_links") val links: ReferenceLinks,
        val author: GitUserDate,
//        val changeCounts: ChangeCountDictionary,
//        val changes: List<GitChange>,
        val comment: String,
        val commentTruncated: Boolean,
        val commitId: String,
        val committer: GitUserDate,
        val parents: List<String>?,
        val push: GitPushRef,
        val remoteUrl: String,
        val statuses: List<GitStatus>,
        val url: String,
        val workItems: List<ResourceRef>
)
