package com.example.mobile_application_project

import android.content.Context
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class FriendRequestAdapter(
    private val context: Context,
    private val layoutResId: Int,
    private var friendRequestList: MutableList<FriendRequest>
) : BaseAdapter() {

    override fun getCount(): Int {
        return friendRequestList.size
    }

    override fun getItem(position: Int): Any {
        return friendRequestList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(layoutResId, parent, false)
        val friendRequest = friendRequestList[position]

        val usernameTextView: TextView = view.findViewById(R.id.friendRequestTextView)
        val acceptButton: Button = view.findViewById(R.id.acceptButton)
        val rejectButton: Button = view.findViewById(R.id.rejectButton)

        usernameTextView.text = "${friendRequest.name} ${friendRequest.surname} (${friendRequest.email})"

        acceptButton.setOnClickListener {
            respondToFriendRequest(friendRequest, true)
        }

        rejectButton.setOnClickListener {
            respondToFriendRequest(friendRequest, false)
        }

        return view
    }

    private fun respondToFriendRequest(friendRequest: FriendRequest, accept: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val url = if (accept) {
            "https://voidmelon.pythonanywhere.com/pendinglist/confirm"
        } else {
            "https://voidmelon.pythonanywhere.com/pendinglist/remove"
        }

        val jsonBody = if (accept) {
            JSONObject().apply {
                put("user_id", userId)
                put("accepted_friendship_by_user_id", friendRequest.id)
            }.toString()
        } else {
            JSONObject().apply {
                put("user_id", userId)
                put("friend_id", friendRequest.id)
            }.toString()
        }

        val makeRequestTask = object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg params: Void?): Boolean {
                return try {
                    if (accept) {
                        makePostRequest(url, jsonBody)
                    } else {
                        makeDeleteRequest(url, jsonBody)
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            override fun onPostExecute(result: Boolean) {
                if (result) {
                    friendRequestList.remove(friendRequest)
                    notifyDataSetChanged()
                    Toast.makeText(context, if (accept) "Friend request accepted" else "Friend request rejected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to respond to friend request", Toast.LENGTH_SHORT).show()
                }
            }
        }

        makeRequestTask.execute()
    }

    private fun makePostRequest(urlString: String, jsonBody: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        try {
            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.writeBytes(jsonBody)
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                // Request successful
            } else {
                throw IOException("HTTP error code: $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
    }

    private fun makeDeleteRequest(urlString: String, jsonBody: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.requestMethod = "DELETE"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        try {
            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.writeBytes(jsonBody)
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                // Request successful
            } else {
                throw IOException("HTTP error code: $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
    }
}
