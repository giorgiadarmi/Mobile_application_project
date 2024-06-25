package com.example.mobile_application_project

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_application_project.databinding.ActivityMessageBinding
import com.example.mobile_application_project.ui.Message
import com.example.mobile_application_project.ui.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding
    private lateinit var database: DatabaseReference
    private lateinit var messageListView: ListView
    private lateinit var messageList: MutableList<Message>
    private lateinit var currentUserUsername: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        currentUserUsername = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        messageListView = findViewById(R.id.messageListView)
        messageList = mutableListOf()

        fetchCurrentUser()
        fetchMessages()

        binding.btnHome?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
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
        database = FirebaseDatabase.getInstance().getReference("Messages")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null && (message.user_send == currentUserUsername || message.user_recv == currentUserUsername)) {
                        messageList.add(message)
                    }
                }
                val adapter = MessageAdapter(this@MessageActivity, R.layout.message_item, messageList)
                messageListView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MessageActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
