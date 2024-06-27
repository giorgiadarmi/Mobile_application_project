package com.example.mobile_application_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_application_project.ui.Friend

class FriendsAdapter(private val friends: MutableList<Friend>, private val onDelete: (Friend) -> Unit) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {
    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(friend: Friend) {
            usernameTextView.text = "${friend.name} ${friend.surname} (${friend.email})"
            deleteButton.setOnClickListener { onDelete(friend) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size
}
