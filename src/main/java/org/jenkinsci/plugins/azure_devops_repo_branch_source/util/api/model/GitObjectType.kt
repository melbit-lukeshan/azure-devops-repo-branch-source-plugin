package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

enum class GitObjectType {
    bad,
    blob,
    commit,
    ext2,
    ofsDelta,
    refDelta,
    tag,
    tree
}
