package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMSource
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

object LogUtil {
    private val TAG: String = LogUtil::class.java.simpleName
    private const val NULL_STRING = "{NULL}"
    private var isInDebugMode = false
    private val dateTimeParserFormatter: DateTimeParserFormatter = DateTimeParserFormatter(Constants.DATE_TIME_FORMAT_ISO8601_ANDROID)
    private val LOGGER = Logger.getLogger(AzureDevOpsRepoSCMSource::class.java.name)

    private val caller: StackTraceElement?
        get() {
            val stElements = Thread.currentThread().stackTrace
            for (i in 1 until stElements.size) {
                val ste = stElements[i]
                if (ste.className != LogUtil::class.java.name && ste.className.indexOf("java.lang.Thread") != 0) {
                    return ste
                }
            }
            return null
        }

    private val callerCaller: StackTraceElement?
        get() {
            val stElements = Thread.currentThread().stackTrace
            var callerClassName: String? = null
            for (i in 1 until stElements.size) {
                val ste = stElements[i]
                if (ste.className != LogUtil::class.java.name && ste.className.indexOf("java.lang.Thread") != 0) {
                    if (callerClassName == null) {
                        callerClassName = ste.className
                    } else if (callerClassName != ste.className) {
                        return ste
                    }
                }
            }
            return null
        }

    private fun switchCoroutinesDebugMode() {
        System.setProperty("kotlinx.coroutines.debug", if (isInDebugMode) "on" else "off")
    }

    /**
     * Pass the application's BuildConfig.DEBUG here
     *
     * LogUtil only do the log in debug mode
     *
     * @param debugFlag please pass the application's BuildConfig.DEBUG
     */
    fun setDebugMode(debugFlag: Boolean) {
        isInDebugMode = debugFlag
        switchCoroutinesDebugMode()
    }

    fun logThrowable(throwable: Throwable) {
        if (isInDebugMode) {
            throwable.printStackTrace()
        }
    }

    fun logTime(timeName: String? = null, tag: String? = null) {
        if (isInDebugMode) {
            dateTimeParserFormatter.toString(Date()).let { timeString ->
                caller?.let { theCaller ->
                    (tag ?: theCaller.className).let { theTag ->
                        (timeName?.let { theTimeName -> "=== Time from ${theCaller.methodName}.$theTimeName is $timeString ===" }
                                ?: "=== Time from ${theCaller.methodName} is $timeString ===").let { theMessage ->
                            LOGGER.log(Level.INFO, theMessage)
                        }
                    }
                } ?: (tag ?: TAG).let { theTag ->
                    (timeName?.let { theTimeName -> "=== Time from $theTimeName is $timeString ===" } ?: "=== Time is $timeString ===").let { theMessage ->
                        LOGGER.log(Level.INFO, theMessage)
                    }
                }
            }
        }
    }

    fun logProcess(tag: String, process: Process?) {
        if (process != null) {
            //logDebug(tag, ShellCommandUtil.getShellCommandResult(process).toString());
        }
    }

    fun logDebug(message: Any? = null, showThreadInfo: Boolean = false, tag: String? = null) {
        if (isInDebugMode) {
            (message ?: NULL_STRING).let { theMessage ->
                (tag ?: caller?.className ?: TAG).let { theTag ->
                    theMessage.toString().let { theMessageString ->
                        LOGGER.log(Level.INFO, if (showThreadInfo) "${Thread.currentThread().name} $theMessageString" else theMessageString)
                    }
                }
            }
        }
    }

    fun logWarning(message: Any? = null, showThreadInfo: Boolean = false, tag: String? = null) {
        if (isInDebugMode) {
            (message ?: NULL_STRING).let { theMessage ->
                (tag ?: caller?.className ?: TAG).let { theTag ->
                    theMessage.toString().let { theMessageString ->
                        LOGGER.log(Level.WARNING, if (showThreadInfo) "${Thread.currentThread().name} $theMessageString" else theMessageString)
                    }
                }
            }
        }
    }

    fun logError(message: Any? = null, showThreadInfo: Boolean = false, tag: String? = null) {
        if (isInDebugMode) {
            (message ?: NULL_STRING).let { theMessage ->
                (tag ?: caller?.className ?: TAG).let { theTag ->
                    theMessage.toString().let { theMessageString ->
                        LOGGER.log(Level.SEVERE, if (showThreadInfo) "${Thread.currentThread().name} $theMessageString" else theMessageString)
                    }
                }
            }
        }
    }
}