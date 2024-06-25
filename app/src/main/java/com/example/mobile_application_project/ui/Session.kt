package com.example.mobile_application_project.ui

data class Session(
    val id: Int,
    val name: String,
    val date_of_creation: String,
    val date_of_end: String,
    val active: Boolean,
    val creator_id: String,
    val avgSpeed: Double,
    val number_of_step: Int,
    val totalDistance: Double,
    val session_type: String,
    val session_type_id: Int
)
