package com.example.mobile_application_project

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.example.mobile_application_project.ui.User

class UserAdapter(private val context: Context, private val layoutResId: Int, private val userList: List<User>) : BaseAdapter() {

    override fun getCount(): Int {
        return userList.size
    }

    override fun getItem(position: Int): Any {
        return userList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(layoutResId, parent, false)
        val user = userList[position]

        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val ageTextView = view.findViewById<TextView>(R.id.ageTextView)
        val inviteButton = view.findViewById<Button>(R.id.inviteButton)

        nameTextView.text = user.name
        usernameTextView.text = user.username
        ageTextView.text = user.age

        inviteButton.setOnClickListener {
            if (context is FriendsActivity) {
                context.sendInvite(user)
            }
        }

        return view
    }
}
