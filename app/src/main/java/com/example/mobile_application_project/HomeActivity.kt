package com.example.mobile_application_project

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_application_project.databinding.ActivityHomeBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth


class HomeActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        firebaseAuth = FirebaseAuth.getInstance()


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            toggle = ActionBarDrawerToggle(this@HomeActivity, drawerLayout, R.string.open, R.string.close)
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()

            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            navView.setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.Home -> {
                        Toast.makeText(this@HomeActivity, "Home Item Clicked", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeActivity, HomeActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.Profile -> {
                        Toast.makeText(this@HomeActivity, "Profile Item Clicked", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeActivity, ProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.Weather -> {
                        Toast.makeText(this@HomeActivity, "Weather Item Clicked", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeActivity, WeatherActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.Training -> {
                        Toast.makeText(this@HomeActivity, "Training Item Clicked", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeActivity, TrainingActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.Maps -> {
                        Toast.makeText(this@HomeActivity, "Maps Item Clicked", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeActivity, TrackingActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.Teams -> {
                        Toast.makeText(this@HomeActivity, "Teams Item Clicked", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeActivity, FriendsActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.Messages -> {
                        Toast.makeText(this@HomeActivity, "Messages Item Clicked", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeActivity, MessageActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.Settings -> {
                        Toast.makeText(this@HomeActivity, "Settings Item Clicked", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                }
                true
            }

        }
    }
}