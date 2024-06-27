package com.example.mobile_application_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_application_project.databinding.ActivityTrainingBinding
import com.example.mobile_application_project.ui.EnvironmentData
import com.example.mobile_application_project.ui.Session
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TrainingActivity : AppCompatActivity() {

    private lateinit var retrofit: Retrofit
    private lateinit var apiService: ApiService
    private lateinit var sessionsAdapter: SessionsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var binding: ActivityTrainingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startNewTrainingButton?.setOnClickListener {
            val intent = Intent(this, SessionActivity::class.java)
            startActivity(intent)
        }

        binding.btnHome?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        recyclerView = binding.recyclerView
        // Initialize Retrofit
        retrofit = Retrofit.Builder()
            .baseUrl("https://voidmelon.pythonanywhere.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Initialize RecyclerView and Adapter
        sessionsAdapter = SessionsAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = sessionsAdapter

        // Fetch all user sessions
        val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        getAllSessions(userId)
    }

    private fun getAllSessions(userId: String) {
        val call = apiService.getAllSessions(userId)
        call.enqueue(object : Callback<List<Session>> {
            override fun onResponse(call: Call<List<Session>>, response: Response<List<Session>>) {
                if (response.isSuccessful) {
                    val sessions = response.body()
                    sessions?.let {
                        sessionsAdapter.setSessions(it)
                        // Fetch environmental data for each session
                        it.forEach { session ->
                            getEnvironmentData(userId, session.id)
                        }
                    }
                } else {
                    Log.e(TAG, "Error during API call: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Session>>, t: Throwable) {
                Log.e(TAG, "Error during API call", t)
            }
        })
    }

    private fun getEnvironmentData(userId: String, sessionId: Int) {
        val call = apiService.getEnvironmentData(userId, sessionId)
        call.enqueue(object : Callback<List<EnvironmentData>> {
            override fun onResponse(call: Call<List<EnvironmentData>>, response: Response<List<EnvironmentData>>) {
                if (response.isSuccessful) {
                    val environmentData = response.body()
                    environmentData?.let {
                        if (it.isNotEmpty()) {
                            sessionsAdapter.setEnvironmentData(sessionId, it[0])
                        }
                    }
                } else {
                    Log.e(TAG, "Error during API call: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<EnvironmentData>>, t: Throwable) {
                Log.e(TAG, "Error during API call", t)
            }
        })
    }

    companion object {
        private const val TAG = "TrainingActivity"
    }
}
