package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

enum class VersionControlRecursionType {
    full,
    none,
    oneLevel,
    oneLevelPlusNestedEmptyFolders
}
