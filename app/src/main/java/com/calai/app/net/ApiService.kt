package com.calai.app.net

import retrofit2.http.GET

interface ApiService {
    @GET("api/hello")
    suspend fun hello(): String


    @GET("api/info")
    suspend fun info(): InfoDTO

}
