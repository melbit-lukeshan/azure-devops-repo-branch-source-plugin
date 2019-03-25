package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import java.util.*

data class GitUserDate(
        val date: Date,
        val email: String,
        val imageUrl: String,
        val name: String
)
