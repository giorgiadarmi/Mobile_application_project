package com.example.mobile_application_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_application_project.databinding.ActivitySignUpBinding
import com.example.mobile_application_project.ui.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private lateinit var database : DatabaseReference
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
            val username = binding.usernameEt.text.toString()
            val age = binding.ageEt.text.toString()
            val email = binding.emailEt.text.toString()
            val pass = binding.passEt.text.toString()
            val confirmPass = binding.RepeatPassEt.text.toString()



            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {

                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                        if (it.isSuccessful) {
                            val UID = FirebaseAuth.getInstance().getCurrentUser()?.getUid()
                            Log.d("qui", UID.toString())
                            database = FirebaseDatabase.getInstance().getReference("Users")
                            val User = User(name,email,age,username)
                            database.child(UID.toString()).setValue(User).addOnCompleteListener {
                                binding.nameEt.text!!.clear()
                                binding.emailEt.text!!.clear()
                                binding.ageEt.text!!.clear()
                                binding.usernameEt.text!!.clear()
                                Toast.makeText(this,"Successfully Saved",Toast.LENGTH_SHORT).show()
                                Log.d("SignUpActivity", "User saved successfully")
                            }.addOnFailureListener{
                                Toast.makeText(this,"Failed",Toast.LENGTH_SHORT).show()
                            }
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()

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
}