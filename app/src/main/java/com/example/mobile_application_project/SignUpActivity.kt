package com.example.mobile_application_project

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_application_project.databinding.ActivitySignUpBinding
import com.example.mobile_application_project.ui.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignUpActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.textView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.button.setOnClickListener {
            val name = binding.nameEt.text.toString()
            val surname = binding.surnameEt.text.toString()
            val username = binding.usernameEt.text.toString()
            val age = binding.ageEt.text.toString()
            val email = binding.emailEt.text.toString()
            val emailVerified = false
            val signUpDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val pass = binding.passEt.text.toString()
            val confirmPass = binding.RepeatPassEt.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {
                    checkUsernameAvailability(username) { isAvailable ->
                        if (isAvailable) {
                            firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    val UID = FirebaseAuth.getInstance().currentUser?.uid
                                    database = FirebaseDatabase.getInstance().getReference("Users")
                                    val user = User(name, surname, email, emailVerified, signUpDate, age, username)
                                    database.child(UID.toString()).setValue(user).addOnCompleteListener {
                                        binding.nameEt.text!!.clear()
                                        binding.surnameEt.text!!.clear()
                                        binding.emailEt.text!!.clear()
                                        binding.ageEt.text!!.clear()
                                        binding.usernameEt.text!!.clear()
                                        Toast.makeText(this, "Successfully Saved", Toast.LENGTH_SHORT).show()
                                        Log.d("SignUpActivity", "User saved successfully")

                                        // After saving to Firebase, send data to external server
                                        sendUserDataToServer(user, UID.toString())
                                    }.addOnFailureListener {
                                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                                    }

                                    val intent = Intent(this, HomeActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Username is already taken", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUsernameAvailability(username: String, callback: (Boolean) -> Unit) {
        database = FirebaseDatabase.getInstance().getReference("Users")
        val query: Query = database.orderByChild("username").equalTo(username)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(!snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SignUpActivity, error.message, Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    private fun sendUserDataToServer(user: User, uid: String) {
        val url = "https://voidmelon.pythonanywhere.com/user"

        val userData = JSONObject().apply {
            put("id", uid)
            put("name", user.name)
            put("surname", user.surname)
            put("email", user.email)
            put("email_verified", false)
            put("sign_up_date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            put("username", user.username)
            put("age", user.age)
        }

        Log.d("SignUpActivity", "User Data JSON: $userData")

        val sendUserTask = object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    makePostRequest(url, userData.toString())
                    return true
                } catch (e: Exception) {
                    Log.e("SignUpActivity", "Error sending user data to server", e)
                }
                return false
            }

            override fun onPostExecute(result: Boolean) {
                if (result) {
                    Log.d("SignUpActivity", "User data sent successfully")
                }
            }
        }
        sendUserTask.execute()
    }

    private fun makePostRequest(urlString: String, jsonBody: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        try {
            val wr = OutputStreamWriter(connection.outputStream)
            wr.write(jsonBody)
            wr.flush()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                Log.d("SignUpActivity", "Post request successful")
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }
}
