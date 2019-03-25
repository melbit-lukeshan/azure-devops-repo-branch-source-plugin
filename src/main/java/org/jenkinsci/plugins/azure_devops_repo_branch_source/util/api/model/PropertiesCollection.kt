package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

data class PropertiesCollection(
        val count: Int,
        val item: Any,
        val keys: List<String>,
        val values: List<String>
)
