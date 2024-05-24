package com.example.mobile_application_project

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.mobile_application_project.databinding.ActivityMainBinding
import com.example.mobile_application_project.ui.login.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityMainBinding
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient= GoogleSignIn.getClient(this,gso)


        binding.logout.setOnClickListener {
            mGoogleSignInClient.signOut().addOnCompleteListener {
                val intent= Intent(this, LoginActivity::class.java)
                Toast.makeText(this,"Logging Out",Toast.LENGTH_SHORT).show()
                startActivity(intent)
                finish()
            }
        }

    }

}