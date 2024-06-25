package com.example.mobile_application_project

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.mobile_application_project.databinding.ActivitySettingsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private lateinit var currentUser: FirebaseUser
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        currentUser = firebaseAuth.currentUser!!
        database = FirebaseDatabase.getInstance().reference

        binding.btnHome?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }


        // Initialize Google sign-in client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize UI components and set click listeners
        initUI()

        sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("darkMode", false)

        binding.darkModeSwitch.isChecked = isDarkMode

        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                saveThemeState(true)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                saveThemeState(false)
            }
        }

    }

    private fun saveThemeState(isDarkMode: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("darkMode", isDarkMode)
        editor.apply()
    }

    private fun initUI() {
        // Change password button click listener
        binding.changePasswordBtn.setOnClickListener {
            // Implement change password functionality
            // Example: navigate to ChangePasswordActivity
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // Forgot password button click listener
        binding.forgotPasswordBtn.setOnClickListener {
            // Implement forgot password functionality
            // Example: send reset password email
            sendPasswordResetEmail()
        }



        // Delete account button click listener
        binding.deleteAccountBtn.setOnClickListener {
            // Implement delete account functionality
            deleteAccount()
        }



        // Sign out button click listener
        binding.signOutBtn.setOnClickListener {
            firebaseAuth.signOut()
            mGoogleSignInClient.signOut()
            val logoutIntent = Intent(this, LoginActivity::class.java)
            startActivity(logoutIntent)
            finish()
        }
    }

    private fun deleteAccount() {
        val userId = firebaseAuth.currentUser?.uid
        firebaseAuth.currentUser?.delete()
        firebaseAuth.signOut()
        mGoogleSignInClient.signOut()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (userId != null) {
                        val databaseRef = FirebaseDatabase.getInstance().reference
                            .child("Users")
                            .child(userId)

                        databaseRef.removeValue()
                            .addOnSuccessListener {
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                            }
                    }
                } else {

                }
            }
    }
    private fun sendPasswordResetEmail() {
        // Example implementation to send reset password email to current user's email
        firebaseAuth.sendPasswordResetEmail(currentUser.email!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Password reset email sent successfully
                    // Show a toast or notification
                } else {
                    // Handle failure to send reset password email
                }
            }
    }

}
