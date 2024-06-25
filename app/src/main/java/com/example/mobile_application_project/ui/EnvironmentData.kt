package com.example.mobile_application_project.ui

data class EnvironmentData(
    val temperature: Float,
    val humidity: Float,
    val pressure: Float,
    val latitude: Double,
    val longitude: Double,
    val date_of_measurement: String
)