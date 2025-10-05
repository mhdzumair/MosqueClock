package com.mosque.prayerclock.data.network

import com.mosque.prayerclock.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyInterceptor
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : Interceptor {
        companion object {
            private const val HEADER_AUTHORIZATION = "Authorization"
            private const val BEARER_PREFIX = "Bearer "
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()

            // Get API key from settings (runtime configuration only)
            val apiKey =
                runBlocking {
                    val settings = settingsRepository.getSettings().first()
                    settings.mosqueClockBackendApiKey
                }

            // Add API key to all requests (only if configured)
            val request =
                if (apiKey.isNotEmpty()) {
                    originalRequest
                        .newBuilder()
                        .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$apiKey")
                        .build()
                } else {
                    originalRequest
                }

            return chain.proceed(request)
        }
    }
