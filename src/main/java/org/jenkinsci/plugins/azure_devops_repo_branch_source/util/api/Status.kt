package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import java.util.*

data class Status(val creationDate: String,
                  val description: String,
                  val id: Int,
                  val state: StatusState,
                  val targetUrl: String,
                  val updatedDate: Date)
