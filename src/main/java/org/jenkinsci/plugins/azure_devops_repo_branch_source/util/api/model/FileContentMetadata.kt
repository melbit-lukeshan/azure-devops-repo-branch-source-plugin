package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class FileContentMetadata(
        val contentType: String,
        val encoding: Int,
        val extension: String,
        val fileName: String,
        val isBinary: Boolean,
        val isImage: Boolean,
        val vsLink: String
)
