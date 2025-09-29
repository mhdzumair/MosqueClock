package com.mosque.prayerclock.data.network

import com.mosque.prayerclock.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyInterceptor
    @Inject
    constructor() : Interceptor {
        companion object {
            private const val HEADER_AUTHORIZATION = "Authorization"
            private const val BEARER_PREFIX = "Bearer "
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()

            // Add API key to all requests using configurable value from local.properties
            val request =
                originalRequest
                    .newBuilder()
                    .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX${BuildConfig.MOSQUE_CLOCK_API_KEY}")
                    .build()

            return chain.proceed(request)
        }
    }
