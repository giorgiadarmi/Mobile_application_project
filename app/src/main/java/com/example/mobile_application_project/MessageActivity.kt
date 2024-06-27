package com.example.mobile_application_project

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_application_project.databinding.ActivityMessageBinding
import com.example.mobile_application_project.ui.Message
import com.example.mobile_application_project.ui.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

class MessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding
    private lateinit var workoutRequestsListView: ListView
    private lateinit var friendRequestsListView: ListView
    private lateinit var workoutRequestsList: MutableList<Message>
    private lateinit var friendRequestsList: MutableList<FriendRequest>
    private lateinit var currentUserUsername: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserUsername = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        workoutRequestsListView = findViewById(R.id.workoutRequestsListView)
        friendRequestsListView = findViewById(R.id.friendRequestsListView)
        workoutRequestsList = mutableListOf()
        friendRequestsList = mutableListOf()

        fetchCurrentUser()
        fetchMessages()
        fetchFriendRequests()

        binding.btnHome?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        workoutRequestsListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val message = workoutRequestsList[position]
            // Implementa il gestore per l'elemento della lista
        }

        friendRequestsListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val friendRequest = friendRequestsList[position]
            // Implementa il gestore per l'elemento della lista
        }
    }

    private fun fetchCurrentUser() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserUsername)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                currentUserUsername = user?.username.orEmpty()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MessageActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchMessages() {
        val database = FirebaseDatabase.getInstance().reference.child("Messages")
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                workoutRequestsList.clear()

                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null && (message.user_send == currentUserUsername || message.user_recv == currentUserUsername)) {
                        if (message.type == "workout") {
                            workoutRequestsList.add(message)
                        }
                    }
                }

                val workoutRequestsAdapter = MessageAdapter(
                    this@MessageActivity,
                    R.layout.workout_request_item,
                    workoutRequestsList
                )
                workoutRequestsListView.adapter = workoutRequestsAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MessageActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchFriendRequests() {
        val client = OkHttpClient()
        val url = "https://voidmelon.pythonanywhere.com/pendinglist/$currentUserUsername"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MessageActivity, "Failed to load friend requests", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let {
                        val jsonArray = JSONArray(it)
                        friendRequestsList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val friendRequest = FriendRequest(
                                jsonObject.getString("id"),
                                jsonObject.getString("name"),
                                jsonObject.getString("surname"),
                                jsonObject.getString("email")
                            )
                            friendRequestsList.add(friendRequest)
                        }

                        runOnUiThread {
                            val friendRequestsAdapter = FriendRequestAdapter(
                                this@MessageActivity,
                                R.layout.friend_request_item,
                                friendRequestsList
                            )
                            friendRequestsListView.adapter = friendRequestsAdapter
                        }
                    }
                }
            }
        })
    }
}

data class FriendRequest(
    val id: String,
    val name: String,
    val surname: String,
    val email: String
)
