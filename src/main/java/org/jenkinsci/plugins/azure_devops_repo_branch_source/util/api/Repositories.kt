package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api

import com.google.gson.annotations.SerializedName
import java.util.*

data class Repositories(val id: Int,
                        @SerializedName("full_name") val fullName: String?,
                        val description: String?,
                        val owner: Owner?,
                        @SerializedName("updated_at") val updatedAt: Date?) {

    data class Owner(val id: Int,
                     val login: String?,
                     val type: String?,
                     val url: String?)
}