package com.nexxserve.cavgodrivers

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import com.nexxserve.cavgodrivers.TokenRepository.refreshToken
import com.nexxserve.cavgodrivers.TokenRepository.removeRefresh
import com.nexxserve.cavgodrivers.TokenRepository.removeToken
import com.nexxserve.cavgodrivers.TokenRepository.resetRepo
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

private class AuthorizationInterceptor : Interceptor {

    private val TOKEN_EXPIRY_TIME = 50 * 60 * 1000L // 50 minutes in milliseconds
    private var isTokenRefreshing = false



    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenRepository.getToken()
        // Run the coroutine for token refresh and validation


        val requestBuilder = chain.request().newBuilder()

        token?.let {
            Log.d("AuthorizationInterceptor", "Adding token: $it")
            requestBuilder.addHeader("Authorization", "Bearer $it")
        } ?: Log.w("AuthorizationInterceptor", "No token found")

        return chain.proceed(requestBuilder.build())
    }

}

val apolloClient = ApolloClient.Builder()
    .serverUrl("https://cavgo.onrender.com/graphql")
    .webSocketServerUrl("wss://apollo-fullstack-tutorial.herokuapp.com/graphql")

    .okHttpClient(
        OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor())
            .build()
    )
    .webSocketReopenWhen { throwable, attempt ->
        Log.d("Apollo", "WebSocket got disconnected, reopening after a delay", throwable)
        kotlinx.coroutines.delay(attempt * 1000)
        true
    }
    .build()
