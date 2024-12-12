package com.nexxserve.cavgodrivers

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

private class AuthorizationInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        Log.d("AuthorizationInterceptor", "Intercepting request")
        val request = chain.request().newBuilder()
            .apply {
                TokenRepository.getToken()?.let { token ->
                    Log.d("AuthorizationInterceptor", "Adding token: $token")
                    addHeader("Authorization", "Bearer $token")
                } ?: Log.w("AuthorizationInterceptor", "No token found")
            }
            .build()
        return chain.proceed(request)
    }

}

val apolloClient = ApolloClient.Builder()
    .serverUrl("https://cavgo.onrender.com/graphql")
    .webSocketServerUrl("wss://cavgo.onrender.com/graphql")
    .okHttpClient(
        OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor())
            .build()
    )
    .webSocketReopenWhen { throwable, attempt ->
        Log.d("Apollo", "WebSocket got disconnected, reopening after a delay", throwable)
        delay(attempt * 1000)
        true
    }

    .build()