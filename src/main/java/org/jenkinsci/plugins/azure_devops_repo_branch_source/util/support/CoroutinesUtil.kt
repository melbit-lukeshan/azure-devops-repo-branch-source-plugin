package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val TAG = "CoroutinesUtil"

fun <T> cancelCompletableDeferred(completableDeferred: CompletableDeferred<T>?, cause: Throwable) = completableDeferred?.completeExceptionally(cause)

fun cancelJob(job: Job?) = job?.cancel()

fun launchNewScopeUIBlock(coroutineName: String? = null, completionHandler: CompletionHandler? = null, block: suspend CoroutineScope.() -> Unit): Job = launchNewScopeBlock(true, coroutineName, completionHandler, block)

fun launchNewScopeIOBlock(coroutineName: String? = null, completionHandler: CompletionHandler? = null, block: suspend CoroutineScope.() -> Unit): Job = launchNewScopeBlock(false, coroutineName, completionHandler, block)

fun CoroutineScope.launchUIBlock(coroutineName: String? = null, completionHandler: CompletionHandler? = null, block: suspend CoroutineScope.() -> Unit): Job = launchBlock(true, coroutineName, completionHandler, block)

fun CoroutineScope.launchIOBlock(coroutineName: String? = null, completionHandler: CompletionHandler? = null, block: suspend CoroutineScope.() -> Unit): Job = launchBlock(false, coroutineName, completionHandler, block)

fun <T> CoroutineScope.asyncIOBlock(coroutineName: String? = null, completionHandler: CompletionHandler? = null, block: suspend CoroutineScope.() -> T): Deferred<T> {
    val dispatcher = coroutineName?.let { Dispatchers.IO + CoroutineName(coroutineName) }
            ?: Dispatchers.IO
    val blockName = coroutineName ?: "asyncIOBlock"
    return async(dispatcher) {
        LogUtil.logDebug("$blockName start", true, TAG)
        block().also { LogUtil.logDebug("$blockName end", true, TAG) }
    }.also { deferred ->
        deferred.invokeOnCompletion(LogCompletionHandler(blockName, completionHandler))
    }
}

suspend fun <T> CoroutineScope.asyncIOBlockAwait(coroutineName: String? = null, completionHandler: CompletionHandler? = null, block: suspend CoroutineScope.() -> T): T = asyncIOBlock(coroutineName, completionHandler, block).await()

private fun launchNewScopeBlock(uiDispatcher: Boolean = true, coroutineName: String? = null, completionHandler: CompletionHandler? = null, block: suspend CoroutineScope.() -> Unit): Job = Job().also {
    object : CoroutineScope {
        override val coroutineContext: CoroutineContext = it + EmptyCoroutineContext
        fun doInScope() {
            val blockName = coroutineName ?: (if (uiDispatcher) {
                "launchNewScopeUIBlock"
            } else {
                "launchNewScopeIOBlock"
            })
            launchBlock(uiDispatcher, blockName, completionHandler, block)
        }
    }.doInScope()
}

private fun CoroutineScope.launchBlock(uiDispatcher: Boolean = true, coroutineName: String? = null, completionHandler: CompletionHandler? = null, block: suspend CoroutineScope.() -> Unit): Job {
    val coroutineContext = (if (uiDispatcher) Dispatchers.Main else Dispatchers.IO).let { coroutineDispatcher ->
        coroutineName?.let { coroutineDispatcher + CoroutineName(coroutineName) }
                ?: coroutineDispatcher
    }
    val blockName = coroutineName ?: (if (uiDispatcher) {
        "launchUIBlock"
    } else {
        "launchIOBlock"
    })
    return launch(coroutineContext) {
        LogUtil.logDebug("$blockName start", true, TAG)
        block().also { LogUtil.logDebug("$blockName end", true, TAG) }
    }.also { job ->
        job.invokeOnCompletion(LogCompletionHandler(blockName, completionHandler))
    }
}

class LogCompletionHandler(private val blockName: String, private val completionHandler: CompletionHandler? = null) : CompletionHandler {
    override fun invoke(cause: Throwable?) {
        completionHandler?.invoke(cause)
        when (cause) {
            null -> LogUtil.logDebug("$blockName completed", true, TAG)
            is CancellationException -> LogUtil.logDebug("$blockName cancelled", true, TAG)
            else -> {
                LogUtil.logThrowable(cause)
                LogUtil.logDebug("$blockName failed", true, TAG)
            }
        }
    }
}