package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import java.util.*

data class AzureRef(val isLocked: Boolean,
                    val name: String,
                    val objectId: String,
                    val peeledObjectId: ProjectState,
                    val url: String,
                    val statuses: List<Status>?,
                    val visibility: ProjectVisibility,
                    val lastUpdateTime: Date)
