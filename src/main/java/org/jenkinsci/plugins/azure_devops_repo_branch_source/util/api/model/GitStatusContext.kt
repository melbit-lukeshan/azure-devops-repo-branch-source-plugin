package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class GitStatusContext(
        val genre: String?,
        val name: String
) {
    override fun toString(): String {
        return if (genre != null) {
            "$genre/$name"
        } else {
            name
        }
    }
}