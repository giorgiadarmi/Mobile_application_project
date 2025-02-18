package com.example.mobile_application_project

import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
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
import java.net.HttpURLConnection
import java.net.URL


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
                                // Delete account on the server
                                deleteUserFromServer(userId)
                            }
                            .addOnFailureListener { e ->
                                Log.e("SettingsActivity", "Failed to delete account from Firebase", e)
                            }
                    }
                } else {
                    Log.e("SettingsActivity", "Failed to sign out from Google", task.exception)
                }
            }
    }

    private fun deleteUserFromServer(userId: String) {
        val url = "https://voidmelon.pythonanywhere.com/user/$userId"
        val deleteTask = object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    makeDeleteRequest(url)
                    return true
                } catch (e: Exception) {
                    Log.e("SettingsActivity", "Error deleting account from server", e)
                }
                return false
            }

            override fun onPostExecute(result: Boolean) {
                if (result) {
                    val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Handle failure
                }
            }
        }
        deleteTask.execute()
    }

    private fun makeDeleteRequest(urlString: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")

        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                Log.d("SettingsActivity", "Account deleted from server successfully")
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } finally {
            connection.disconnect()
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
