package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import java.net.SocketTimeoutException

/**
 * T is the Good result type
 * R is the HttpError result type
 */
sealed class Result<out T : Any, out R : Any> {

    data class Good<out T : Any>(val value: T, val httpStatus: HttpStatus) : Result<T, Nothing>() {
        override fun toString() = "Result.Good{value=$value, httpStatus=$httpStatus}"
    }

    data class Malformed(val value: String?, val httpStatus: HttpStatus, val exception: Throwable) : Result<Nothing, Nothing>() {
        override fun toString() = "Result.Malformed{value=$value, httpStatus=$httpStatus, exception=$exception}"
    }

    data class HttpError<out R : Any>(val value: R, val httpStatus: HttpStatus) : Result<Nothing, R>() {
        override fun toString() = "Result.HttpError{value=$value, httpStatus=$httpStatus}"
    }

    data class HttpErrorMalformed(val value: String?, val httpStatus: HttpStatus, val exception: Throwable) : Result<Nothing, Nothing>() {
        override fun toString() = "Result.HttpErrorMalformed{value=$value, httpStatus=$httpStatus, exception=$exception}"
    }

    data class IoError(val exception: Throwable) : Result<Nothing, Nothing>() {
        private val isTimeout = exception is SocketTimeoutException
        override fun toString() = "Result.IoError{exception=$exception, isTimeout=$isTimeout}"
    }

    data class Canceled(val exception: Throwable?) : Result<Nothing, Nothing>() {
        override fun toString() = "Result.Canceled${exception?.let { "{exception=$it}" } ?: ""}"
    }

    fun getGoodValueOrNull(): T? = (this as? Good)?.value

    fun getHttpErrorValueOrNull(): R? = (this as? HttpError)?.value

    fun getHttpErrorStatusOrNull(): HttpStatus? = (this as? HttpError)?.httpStatus

    data class HttpStatus(val code: Int, private val _message: String? = null) {

        companion object {
            const val OK = 200
            const val CREATED = 201
            const val NON_AUTHORITATIVE_INFORMATION = 203
            const val NO_CONTENT = 204
            const val NOT_MODIFIED = 304
            const val BAD_REQUEST = 400
            const val UNAUTHORIZED = 401
            const val FORBIDDEN = 403
            const val NOT_FOUND = 404
            const val METHOD_NOT_ALLOWED = 405
            const val CONFLICT = 409
            const val GONE = 410
            const val UNSUPPORTED_MEDIA_TYPE = 415
            const val UNPROCESSABLE_ENTITY = 422
            const val TOO_MANY_REQUEST = 429
            const val INTERNAL_SERVER_ERROR = 500
        }

        val message: String = _message?.takeIf { it.isNotBlank() } ?: when (code) {
            OK -> "OK"
            CREATED -> "Created"
            NON_AUTHORITATIVE_INFORMATION -> "Non-Authoritative Information"
            NO_CONTENT -> "No Content"
            NOT_MODIFIED -> "Not Modified"
            BAD_REQUEST -> "Bad Request"
            UNAUTHORIZED -> "Unauthorized"
            FORBIDDEN -> "Forbidden"
            NOT_FOUND -> "Not Found"
            METHOD_NOT_ALLOWED -> "Method Not Allowed"
            CONFLICT -> "Conflict"
            GONE -> "Gone"
            UNSUPPORTED_MEDIA_TYPE -> "Unsupported Media Type"
            UNPROCESSABLE_ENTITY -> "Unprocessable Entity"
            TOO_MANY_REQUEST -> "Too Many Request"
            INTERNAL_SERVER_ERROR -> "Internal Server Error"
            else -> "Unknown"
        }

        override fun toString(): String = "{code=$code, message=$message}"
    }
}