package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class BranchPolicySettings(
        val statusName: String,
        val statusGenre: String?,
        val authorId: String?,
        val invalidateOnSourceUpdate: Boolean,
        val defaultDisplayName: String?,
        val policyApplicability: String?,
        val scope: List<PolicyScope>
)
