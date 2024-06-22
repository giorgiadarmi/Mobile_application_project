package com.example.mobile_application_project.ui

data class Session(
    val name: String,
    val date_of_creation: String,
    val date_of_end: String,
    val active: Boolean,
    val creator_id: String,
    val creator: String,
    val avgSpeed: Double,
    val number_of_step: Int,
    val totalDistance: Double,
    val environment_data: List<EnvironmentData>,
    val session_type: String,
    val session_type_id: Int
)

data class EnvironmentData(
    val temperature: String,
    val humidity: String,
    val pressure: String,
    val latitude: Double,
    val longitude: Double,
    val date_of_measurement: String
)
