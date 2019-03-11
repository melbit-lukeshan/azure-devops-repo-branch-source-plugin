package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import java.io.InputStream

inline fun <T : InputStream?> T.useBufferUntilEnd(bufferSize: Int, block: (ByteArray, Int) -> Unit) {
    this?.apply {
        var contentSize: Int
        val buffer = ByteArray(bufferSize)
        while (read(buffer).also { contentSize = it } >= 0) {
            block(buffer, contentSize)
        }
    }
}