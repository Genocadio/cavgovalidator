package com.nexxserve.cavgodrivers

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.apollographql.apollo.network.ws.GraphQLWsProtocol
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport

class AuthorizationInterceptor : HttpInterceptor {

    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
        val token = TokenRepository.getToken() // Fetch the token dynamically
        Log.d("AuthorizationInterceptor", "Token: $token")

        val requestBuilder = request.newBuilder()
        if (!token.isNullOrEmpty()) {
            // Adding the token to the Authorization header
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.e("AuthorizationInterceptor", "Token is null or empty or refreshing")
        }

        return chain.proceed(requestBuilder.build())
    }
}

val cacheFactory = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)

val apolloClient = ApolloClient.Builder()
    .serverUrl("https://cavgo.onrender.com/graphql")
    .addHttpInterceptor(AuthorizationInterceptor())
    .normalizedCache(cacheFactory)
    .subscriptionNetworkTransport(
        WebSocketNetworkTransport.Builder()
            .protocol(GraphQLWsProtocol.Factory())
            .serverUrl("wss://cavgo.onrender.com/graphql")
            .build()
    )

    .build()




