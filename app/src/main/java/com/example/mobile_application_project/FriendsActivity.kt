package com.example.mobile_application_project

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_application_project.databinding.ActivityFriendsBinding
import com.example.mobile_application_project.ui.Friend
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
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
                searchUser(username)
            } else {
                Toast.makeText(this, "Please enter a username to search", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHome?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Chiamata per ottenere la lista degli amici dell'utente corrente
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            getFriendList(userId)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchUser(username: String) {
        val url = "https://voidmelon.pythonanywhere.com/search/$username"

        val searchUserTask = object : AsyncTask<Void, Void, String>() {
            override fun doInBackground(vararg params: Void?): String {
                return try {
                    makeGetRequest(url)
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching user", e)
                    ""
                }
            }

            override fun onPostExecute(result: String) {
                if (result.isNotBlank()) {
                    try {
                        val jsonArray = JSONArray(result)
                        if (jsonArray.length() > 0) {
                            val jsonObject = jsonArray.getJSONObject(0)
                            val name = jsonObject.optString("name", "")
                            val surname = jsonObject.optString("surname", "")
                            val email = jsonObject.optString("email", "")
                            val userId = jsonObject.optString("id", "")

                            binding.searchResultsContainer.visibility = View.VISIBLE
                            binding.userDetailsTextView.text = "Name: $name\nSurname: $surname\nEmail: $email"

                            binding.addFriendActionButton.setOnClickListener {
                                sendFriendRequest(userId)
                            }

                        } else {
                            Toast.makeText(this@FriendsActivity, "User not found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: JSONException) {
                        Log.e(TAG, "Error parsing JSON: ${e.localizedMessage}")
                        Toast.makeText(this@FriendsActivity, "Error parsing response", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@FriendsActivity, "User not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        searchUserTask.execute()
    }

    private fun sendFriendRequest(sentFriendRequestId: String) {
        val url = "https://voidmelon.pythonanywhere.com/search/sendfriendrequest"
        val requestBody = JSONObject().apply {
            put("user_id", FirebaseAuth.getInstance().currentUser?.uid)
            put("sent_friend_request_id", sentFriendRequestId)
        }

        Log.d("Friend Request", "User Data JSON: $requestBody")

        val sendFriendTask = object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg params: Void?): Boolean {
                return try {
                    makePostRequest(url, requestBody.toString())
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending friend request", e)
                    false
                }
            }

            override fun onPostExecute(result: Boolean) {
                if (result) {
                    Toast.makeText(this@FriendsActivity, "Friend request sent successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@FriendsActivity, "Failed to send friend request", Toast.LENGTH_SHORT).show()
                }
            }
        }
        sendFriendTask.execute()
    }

    private fun getFriendList(userId: String) {
        val url = "https://voidmelon.pythonanywhere.com/friendlist/$userId"

        val getFriendListTask = object : AsyncTask<Void, Void, String>() {
            override fun doInBackground(vararg params: Void?): String {
                return try {
                    makeGetRequest(url)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting friend list", e)
                    ""
                }
            }

            override fun onPostExecute(result: String) {
                if (result.isNotBlank()) {
                    try {
                        val jsonArray = JSONArray(result)
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val name = jsonObject.optString("name", "")
                            val surname = jsonObject.optString("surname", "")
                            val email = jsonObject.optString("email", "")
                            val friendId = jsonObject.optString("id", "")

                            friendsList.add(Friend(friendId, name, surname, email))
                        }
                        friendsAdapter.notifyDataSetChanged()
                    } catch (e: JSONException) {
                        Log.e(TAG, "Error parsing JSON: ${e.localizedMessage}")
                        Toast.makeText(this@FriendsActivity, "Error parsing friend list", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@FriendsActivity, "Friend list is empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
        getFriendListTask.execute()
    }

    private fun makeGetRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")

        return try {
            val inputStream = BufferedInputStream(connection.inputStream)
            val response = convertInputStreamToString(inputStream)
            inputStream.close()
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error in GET request", e)
            ""
        } finally {
            connection.disconnect()
        }
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
                Log.d(TAG, "Post request successful")
            } else {
                throw IOException("HTTP error code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in POST request", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun convertInputStreamToString(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        return sb.toString()
    }

    private fun removeFriend(friend: Friend) {
        val position = friendsList.indexOf(friend)
        if (position != -1) {
            val friendId = friend.id  // Ottieni l'id dell'amico da rimuovere
            friendsList.removeAt(position)
            friendsAdapter.notifyItemRemoved(position)

            // Prepara il corpo della richiesta DELETE
            val requestBody = JSONObject().apply {
                put("user_id", FirebaseAuth.getInstance().currentUser?.uid)
                put("friend_id", friendId)
            }

            // Esegui la chiamata DELETE al server
            val url = "https://voidmelon.pythonanywhere.com/friendlist/remove"
            val deleteFriendTask = object : AsyncTask<Void, Void, Boolean>() {
                override fun doInBackground(vararg params: Void?): Boolean {
                    return try {
                        makeDeleteRequest(url, requestBody.toString())
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting friend", e)
                        false
                    }
                }

                override fun onPostExecute(result: Boolean) {
                    if (result) {
                        Toast.makeText(this@FriendsActivity, "Friend removed successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@FriendsActivity, "Failed to remove friend", Toast.LENGTH_SHORT).show()
                        // Se la rimozione fallisce, puoi aggiungere nuovamente l'amico alla lista qui
                    }
                }
            }
            deleteFriendTask.execute()
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
                Log.d(TAG, "Delete request successful")
            } else {
                throw IOException("HTTP error code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in DELETE request", e)
        } finally {
            connection.disconnect()
        }
    }


    companion object {
        private const val TAG = "FriendsActivity"
    }
}
