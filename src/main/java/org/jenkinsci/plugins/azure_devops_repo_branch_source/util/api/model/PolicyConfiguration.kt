package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import com.google.gson.annotations.SerializedName
import java.time.OffsetDateTime

data class PolicyConfiguration(
        @SerializedName("_links") val links: ReferenceLinks,
        val createdBy: IdentityRef,
        val createdDate: OffsetDateTime,
        val id: Int,
        val isBlocking: Boolean,
        val isDeleted: Boolean,
        val isEnabled: Boolean,
        val revision: Int,
        val settings: BranchPolicySettings,
        val type: PolicyTypeRef,
        val url: String
)
