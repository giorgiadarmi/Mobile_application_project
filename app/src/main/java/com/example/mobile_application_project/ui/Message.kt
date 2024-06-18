package com.example.mobile_application_project.ui

data class Message(
    val id: String? = null,
    val user_send: String? = null,
    val user_recv: String? = null,
    val text: String? = null,
    val date: String? = null,
    val time: String? = null,
    val place: String? = null,
    val response: Boolean? = null
)
