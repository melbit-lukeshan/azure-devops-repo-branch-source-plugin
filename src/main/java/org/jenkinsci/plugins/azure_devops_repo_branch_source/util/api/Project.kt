package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import java.util.*

data class Project(val id: String,
                   val name: String,
                   val url: String,
                   val state: ProjectState,
                   val revision: Int,
                   val visibility: ProjectVisibility,
                   val lastUpdateTime: Date)
