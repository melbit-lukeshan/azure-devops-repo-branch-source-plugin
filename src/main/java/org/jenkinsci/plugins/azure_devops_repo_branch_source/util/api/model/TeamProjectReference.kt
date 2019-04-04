package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import java.util.*

data class TeamProjectReference(
        val id: String,
        val name: String,
        val url: String,
        val state: ProjectState,
        val revision: Int,
        val description: String?,
        val visibility: ProjectVisibility,
        val lastUpdateTime: Date
)