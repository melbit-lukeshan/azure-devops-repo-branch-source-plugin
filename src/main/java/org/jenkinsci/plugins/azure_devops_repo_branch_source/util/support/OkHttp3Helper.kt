package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.reflect.KClass
import okhttp3.Request as OkHttp3Request

object OkHttp3Helper {
    private var isInDebugMode = false
    private var nukeSSL = false
    private val MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8")
    private const val ACCEPT = "Accept"
    private const val ACCEPT_JSON_AND_ALL = "application/json,*/*"
    @PublishedApi
    internal const val CANCELED = "Canceled"

    /**
     * OkHttp3Helper will do logging if isInDebugMode is true
     *
     * @param debugFlag please pass the application's BuildConfig.DEBUG
     */
    fun setDebugMode(debugFlag: Boolean) {
        isInDebugMode = debugFlag
    }

    /**
     * OkHttp3Helper will ignore SSL stuff if nukeSSL is true
     * Usually in production this should be false
     */
    fun setNukeSSL(nukeSSLFlag: Boolean) {
        nukeSSL = nukeSSLFlag
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            retryOnConnectionFailure(false)
            if (isInDebugMode) {
                HttpLoggingInterceptor().also {
                    it.level = HttpLoggingInterceptor.Level.BODY
                    addInterceptor(it)
                }
            }
            if (nukeSSL) {
                allowAllSSLOkHttp(this)
            }
        }.build()
    }

    private val okHttpClientRetryable: OkHttpClient by lazy {
        okHttpClient.newBuilder().retryOnConnectionFailure(true).build()
    }

    private val okHttpClientSequential: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            retryOnConnectionFailure(false)
            if (isInDebugMode) {
                HttpLoggingInterceptor().also {
                    it.level = HttpLoggingInterceptor.Level.BODY
                    addInterceptor(it)
                }
            }
            if (nukeSSL) {
                allowAllSSLOkHttp(this)
            }
        }.build().apply {
            dispatcher().maxRequests = 1
        }
    }

    private val okHttpClientSequentialRetryable: OkHttpClient by lazy {
        okHttpClientSequential.newBuilder().retryOnConnectionFailure(true).build()
    }

    suspend inline fun <reified T : Any, reified R : Any> executeRequestAwait(request: Request<T, R>, inSequentialQueue: Boolean = false): Result<T, R> =
            suspendCancellableCoroutine { cancellableContinuation ->
                requestToCall(request, inSequentialQueue).apply {
                    cancellableContinuation.invokeOnCancellation {
                        cancel()
                    }
                    enqueue(OkHttpJsonCallback(request, T::class, R::class) { result, _ -> cancellableContinuation.resume(result) })
                }
            }

    inline fun <reified T : Any, reified R : Any> executeRequest(request: Request<T, R>): Result<T, R> =
            requestToCall(request).let {
                try {
                    responseToResult(it.execute(), request, T::class, R::class)
                } catch (e: Exception) {
                    LogUtil.logThrowable(e)
                    if (it.isCanceled) {
                        Result.Canceled(if (e.message.equals(CANCELED)) null else e)
                    } else {
                        Result.IoError(e)
                    }
                }
            }

    public fun <T : Any, R : Any> executeRequest2(request: Request<T, R>, targetClass: Class<T>, errorClass: Class<R>): Result<T, R> =
            requestToCall(request).let {
                try {
                    responseToResult(it.execute(), request, targetClass.kotlin, errorClass.kotlin)
                } catch (e: Exception) {
                    LogUtil.logThrowable(e)
                    if (it.isCanceled) {
                        Result.Canceled(if (e.message.equals(CANCELED)) null else e)
                    } else {
                        Result.IoError(e)
                    }
                }
            }

    inline fun <reified T : Any, reified R : Any> enqueueRequest(request: Request<T, R>, noinline resultHandler: (Result<T, R>, Request<T, R>) -> Unit, inSequentialQueue: Boolean = false) {
        requestToCall(request, inSequentialQueue).enqueue(OkHttpJsonCallback(request, T::class, R::class, resultHandler))
    }

    fun cancelSingleRequest(request: Request<*, *>?) {
        request?.let {
            listOf(okHttpClient, okHttpClientRetryable, okHttpClientSequential, okHttpClientSequentialRetryable).forEach { client ->
                (client.dispatcher().queuedCalls() + client.dispatcher().runningCalls()).forEach { call ->
                    if ((call.request().tag() as Request.Tag) == request.tag) {
                        call.cancel()
                        return
                    }
                }
            }
        }
    }

    fun cancelAllRequestsByCategory(category: String?) {
        category?.let {
            listOf(okHttpClient, okHttpClientRetryable, okHttpClientSequential, okHttpClientSequentialRetryable).forEach { client ->
                (client.dispatcher().queuedCalls() + client.dispatcher().runningCalls()).forEach { call ->
                    if ((call.request().tag() as Request.Tag).category == category) {
                        call.cancel()
                    }
                }
            }
        }
    }

    fun cancelSingleRequestById(id: String?) {
        id?.let {
            listOf(okHttpClient, okHttpClientRetryable, okHttpClientSequential, okHttpClientSequentialRetryable).forEach { client ->
                (client.dispatcher().queuedCalls() + client.dispatcher().runningCalls()).forEach { call ->
                    if ((call.request().tag() as Request.Tag).id == id) {
                        call.cancel()
                        return
                    }
                }
            }
        }
    }

    private fun allowAllSSLOkHttp(builder: OkHttpClient.Builder) {
        suppressThrowable {
            val sc = SSLContext.getInstance("SSL")
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
            sc.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            builder.sslSocketFactory(sc.socketFactory, trustManager)
            builder.hostnameVerifier { _, _ -> true }
        }
    }

    @PublishedApi
    internal fun requestToCall(request: Request<*, *>, inSequentialQueue: Boolean = false): Call {
        return if (request.retryIfConnectionFail) {
            if (inSequentialQueue) okHttpClientSequentialRetryable.newBuilder() else okHttpClientRetryable.newBuilder()
        } else {
            if (inSequentialQueue) okHttpClientSequential.newBuilder() else okHttpClient.newBuilder()
        }.apply {
            connectTimeout(request.timeout.toLong(), TimeUnit.SECONDS)
            readTimeout(request.timeout.toLong(), TimeUnit.SECONDS)
            writeTimeout(request.timeout.toLong(), TimeUnit.SECONDS)
        }.build().newCall(generateOkHttpRequest(request))
    }

    @PublishedApi
    internal fun <T : Any, R : Any> responseToResult(response: Response, request: Request<T, R>, targetClass: KClass<T>, errorClass: KClass<R>): Result<T, R> {
        return response.use { theResponse ->
            var responseBodyString = theResponse.body()?.string()
            if (theResponse.isSuccessful) {
                try {
                    responseBodyString!!.let {
                        request.goodResponseBodyWrapper?.takeIf { wrapper -> wrapper.isNotBlank() }?.replace("*", it)
                                ?: it
                    }.also {
                        responseBodyString = it
                    }.let {
                        Result.Good(request.jsonProcessor.instanceFromJson(it, targetClass)!!, Result.HttpStatus(theResponse.code(), theResponse.message()))
                    }
                } catch (e: Exception) {
                    Result.Malformed(responseBodyString, Result.HttpStatus(theResponse.code(), theResponse.message()), e)
                }
            } else {
                try {
                    responseBodyString!!.let {
                        request.httpErrorResponseBodyWrapper?.takeIf { wrapper -> wrapper.isNotBlank() }?.replace("*", it)
                                ?: it
                    }.also {
                        responseBodyString = it
                    }.let {
                        Result.HttpError(request.jsonProcessor.instanceFromJson(it, errorClass)!!, Result.HttpStatus(theResponse.code(), theResponse.message()))
                    }
                } catch (e: Exception) {
                    Result.HttpErrorMalformed(responseBodyString, Result.HttpStatus(theResponse.code(), theResponse.message()), e)
                }
            }
        }
    }

    private fun generateOkHttpRequest(request: Request<*, *>): OkHttp3Request =
            OkHttp3Request.Builder().apply {
                url(request.fullUrl)
                header(ACCEPT, ACCEPT_JSON_AND_ALL)
                request.headersAsPairList.orEmpty().forEach {
                    addHeader(it.first, it.second)
                }
                when (request.method) {
                    Request.Method.DELETE -> delete()
                    Request.Method.GET -> get()
                    Request.Method.HEAD -> head()
                    Request.Method.PATCH, Request.Method.POST, Request.Method.PUT ->
                        request.bodyAsJson?.let {
                            method(request.method.name, RequestBody.create(MEDIA_TYPE_JSON, it))
                        }
                                ?: request.parametersAsMap?.takeIf { parametersMap -> parametersMap.isNotEmpty() }?.let { parametersMap ->
                                    FormBody.Builder().apply {
                                        parametersMap.forEach { parameter ->
                                            addEncoded(parameter.key, parameter.value)
                                        }
                                        method(request.method.name, build())
                                    }
                                }
                                ?: method(request.method.name, RequestBody.create(null, ByteString.EMPTY))
                }
                tag(request.tag)
            }.build()

    @PublishedApi
    internal class OkHttpJsonCallback<T : Any, R : Any>(val request: Request<T, R>, private val targetClass: KClass<T>, private val errorClass: KClass<R>, private val resultHandler: (Result<T, R>, Request<T, R>) -> Unit) : Callback {

        override fun onFailure(call: Call, e: IOException) {
            LogUtil.logThrowable(e)
            if (call.isCanceled) {
                resultHandler(Result.Canceled(if (e.message.equals(CANCELED)) null else e), request)
            } else {
                resultHandler(Result.IoError(e), request)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            resultHandler(responseToResult(response, request, targetClass, errorClass), request)
        }
    }
}