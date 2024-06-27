package com.example.mobile_application_project.ui

data class User(
    val name : String? = null,
    val surname : String? = null,
    val email : String? = null,
    val emailVerified: Boolean = false,
    val signUpDate: String? = null,
    val age : String? = null,
    val username : String? = null,
    val imageUrl : String? = null
)