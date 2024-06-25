package com.example.mobile_application_project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_application_project.databinding.ActivityFriendsBinding
import com.example.mobile_application_project.ui.Friend
import com.google.firebase.auth.FirebaseAuth

class FriendsActivity : AppCompatActivity() {

    private lateinit var searchFriendEditText: EditText
    private lateinit var addFriendButton: Button
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendsAdapter: FriendsAdapter
    private val friendsList = mutableListOf<Friend>()
    private lateinit var binding: ActivityFriendsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchFriendEditText = findViewById(R.id.searchFriendEditText)
        addFriendButton = findViewById(R.id.addFriendButton)
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView)

        friendsAdapter = FriendsAdapter(friendsList) { friend -> removeFriend(friend) }
        friendsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsRecyclerView.adapter = friendsAdapter

        addFriendButton.setOnClickListener {
            val username = searchFriendEditText.text.toString()
            if (username.isNotBlank()) {
                addFriend(username)
            }
        }

        binding.btnHome?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun addFriend(username: String) {
        val newFriend = Friend(FirebaseAuth.getInstance().currentUser?.uid.toString(), username)
        friendsList.add(newFriend)
        friendsAdapter.notifyItemInserted(friendsList.size - 1)
        searchFriendEditText.text.clear()
    }

    private fun removeFriend(friend: Friend) {
        val position = friendsList.indexOf(friend)
        if (position != -1) {
            friendsList.removeAt(position)
            friendsAdapter.notifyItemRemoved(position)
        }
    }
}