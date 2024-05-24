package com.example.mobile_application_project.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_application_project.MainActivity
import com.example.mobile_application_project.R
import com.example.mobile_application_project.SignUpActivity
import com.example.mobile_application_project.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

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
                        val intent = Intent(this, MainActivity::class.java)
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


    private fun handleResult(completedTask: Task<GoogleSignInAccount>){
        try {
            val account: GoogleSignInAccount? =completedTask.getResult(ApiException::class.java)
            if (account != null) {
                updateUI(account)
            }
        } catch (e: ApiException){
            Toast.makeText(this,e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount){
        val credential= GoogleAuthProvider.getCredential(account.idToken,null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {task->
            if(task.isSuccessful) {
                //account.email.toString() <--- the user's email
                //account.displayName.toString()) <--- the user's name
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if(firebaseAuth.currentUser != null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        else if(GoogleSignIn.getLastSignedInAccount(this)!=null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}