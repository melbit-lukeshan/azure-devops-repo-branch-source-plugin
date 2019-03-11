package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import java.util.*

inline fun <R> suppressThrowable(default: R? = null, block: () -> R): R? =
        try {
            block()
        } catch (e: Throwable) {
            LogUtil.logThrowable(e)
            default
        }

fun uuidString(): String = UUID.randomUUID().toString()

fun bytesToHex(source: ByteArray?, separator: String = ""): String? =
        source?.joinToString(separator = separator) { String.format("%02X", it) }

fun hexToBytes(source: String?, separator: String = ""): ByteArray? =
        source?.let {
            val compactSource = source.replace(separator, "")
            val bytesLength = compactSource.length / 2
            ByteArray(bytesLength).also { bytes ->
                for (bytesIndex in 0 until bytesLength) {
                    bytes[bytesIndex] = Integer.parseInt(compactSource.substring(bytesIndex * 2, bytesIndex * 2 + 2), 16).toByte()
                }
            }
        }