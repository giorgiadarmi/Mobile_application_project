package com.example.mobile_application_project

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_application_project.databinding.ActivityLoginBinding
import com.example.mobile_application_project.ui.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LoginActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val reqCode:Int=123


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        binding.textView?.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        binding.button?.setOnClickListener {
            val email = binding.emailEt?.text.toString()
            val pass = binding.passET?.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {

                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()

                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()

            }
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.signIn?.setOnClickListener {
            Toast.makeText(this, "Logging In", Toast.LENGTH_SHORT).show()
            signInWithGoogle()
        }
    }


    private  fun signInWithGoogle(){

        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent,reqCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==reqCode){
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val googleId = account.id ?: ""
                Log.i("Google ID", googleId)
                val googleFirstName = account.givenName ?: ""
                Log.i("Google First Name", googleFirstName)
                val googleLastName = account.familyName ?: ""
                Log.i("Google Last Name", googleLastName)
                val googleEmail = account.email ?: ""
                Log.i("Google Email", googleEmail)
                val googleProfilePicURL = account.photoUrl.toString()
                Log.i("Google Profile Pic URL", googleProfilePicURL)
                val googleIdToken = account.idToken ?: ""
                Log.i("Google ID Token", googleIdToken)

                val user = User(
                    name = googleFirstName,
                    surname = googleLastName,
                    email = googleEmail,
                    username = googleFirstName + googleLastName + "_fitapp",
                    signUpDate = "",
                    age = "99"
                )
                updateUI(account, user)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount, user: User) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
                sendUserDataToFirebase(user, uid)
                checkUserInServerAndSendData(user, uid)
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, task.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendUserDataToFirebase(user: User, uid: String) {
        val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
        databaseReference.child("Users").child(uid).setValue(user)
            .addOnSuccessListener {
                Log.d("LoginActivity", "User data added to Firebase successfully")
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error adding user data to Firebase", e)
            }
    }

    private fun checkUserInServerAndSendData(user: User, uid: String) {
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

        Log.d("LoginActivity", "User Data JSON: $userData")

        val sendUserTask = object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    makePostRequest(url, userData.toString())
                    return true
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error sending user data to server", e)
                }
                return false
            }

            override fun onPostExecute(result: Boolean) {
                if (result) {
                    Log.d("LoginActivity", "User data sent successfully")
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
                Log.d("LoginActivity", "Post request successful")
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }


}