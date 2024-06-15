package com.example.mobile_application_project

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_application_project.databinding.ActivityTrainingBinding
import com.example.mobile_application_project.ui.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class TrainingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrainingBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterActivity

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdapterActivity()
        recyclerView.adapter = adapter

        auth = Firebase.auth
        database = Firebase.database.reference.child("Activities").child(auth.currentUser!!.uid)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activities = mutableListOf<Activity>()
                for (activitySnapshot in snapshot.children) {
                    val activity = activitySnapshot.getValue(Activity::class.java)
                    activity?.let {
                        activities.add(it)
                    }
                }
                adapter.submitList(activities)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        binding.startNewTrainingButton.setOnClickListener {
            val intent = Intent(this, TrackingActivity::class.java)
            startActivity(intent)
        }
    }
}
