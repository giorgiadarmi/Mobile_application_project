package com.example.mobile_application_project

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.mobile_application_project.ui.Message
import com.google.firebase.database.FirebaseDatabase

class MessageAdapter(private val context: Context, private val layoutResId: Int, private val messageList: List<Message>) : BaseAdapter() {

    override fun getCount(): Int {
        return messageList.size
    }

    override fun getItem(position: Int): Any {
        return messageList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(layoutResId, parent, false)
        val message = messageList[position]

        val messageTextView = view.findViewById<TextView>(R.id.messageTextView)
        val responseTextView = view.findViewById<TextView>(R.id.responseTextView)
        val respondYesButton = view.findViewById<Button>(R.id.respondYesButton)
        val respondNoButton = view.findViewById<Button>(R.id.respondNoButton)

        messageTextView.text = """
            From: ${message.user_send}
            To: ${message.user_recv}
            Message: ${message.text}
            Date: ${message.date}
            Time: ${message.time}
            Place: ${message.place}
        """.trimIndent()

        responseTextView.text = "Response: ${if (message.response == true) "Yes" else if (message.response == false) "No" else "Pending"}"

        respondYesButton.setOnClickListener {
            respondToMessage(message, true)
        }

        respondNoButton.setOnClickListener {
            respondToMessage(message, false)
        }

        return view
    }

    private fun respondToMessage(message: Message, response: Boolean) {
        val messageRef = FirebaseDatabase.getInstance().getReference("Messages").child(message.id.orEmpty())
        messageRef.child("response").setValue(response)
        Toast.makeText(context, "Response sent", Toast.LENGTH_SHORT).show()
    }
}