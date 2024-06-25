package com.example.mobile_application_project

import com.example.mobile_application_project.ui.EnvironmentData
import com.example.mobile_application_project.ui.Session
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("user/{userId}/allsessions")
    fun getAllSessions(@Path("userId") userId: String): Call<List<Session>>

    @GET("users/{userId}/sessions/{sessionId}/environmentdatas")
    fun getEnvironmentData(@Path("userId") userId: String, @Path("sessionId") sessionId: Int): Call<List<EnvironmentData>>
}
