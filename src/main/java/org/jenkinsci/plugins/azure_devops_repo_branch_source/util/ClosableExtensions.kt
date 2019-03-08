package org.jenkinsci.plugins.azure_devops_repo_branch_source.util

import java.io.Closeable

inline fun <T : Closeable?, R> T.useOrDefault(default: R? = null, block: (T) -> R): R? {
    var closed = false
    return try {
        block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this?.close()
        } catch (closeException: Exception) {
        }
        default
    } finally {
        if (!closed) {
            this?.close()
        }
    }
}