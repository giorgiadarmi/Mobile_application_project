package com.example.mobile_application_project

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_application_project.databinding.ActivityTeamsBinding
import com.example.mobile_application_project.ui.Message
import com.example.mobile_application_project.ui.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class TeamsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTeamsBinding
    private lateinit var database: DatabaseReference
    private lateinit var userListView: ListView
    private lateinit var userList: MutableList<User>
    private lateinit var currentUserID: String
    private lateinit var currentUserUsername: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeamsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        currentUserID = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        userListView = findViewById(R.id.usersListView)
        userList = mutableListOf()


        fetchCurrentUser()
        fetchUsers()

        binding.btnHome?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchCurrentUser() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserID)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                currentUserUsername = user?.username.orEmpty()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TeamsActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUsers() {
        database = FirebaseDatabase.getInstance().getReference("Users")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null && userSnapshot.key != currentUserID) {
                        userList.add(user)
                    }
                }
                val adapter = UserAdapter(this@TeamsActivity, R.layout.user_item, userList)
                userListView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TeamsActivity, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun sendInvite(receiver: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_invite, null)
        val alertDialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Invite Friend")
            .setPositiveButton("Send") { _, _ ->
                val text = dialogView.findViewById<EditText>(R.id.editTextText).text.toString()
                val date = dialogView.findViewById<EditText>(R.id.editTextDate).text.toString()
                val time = dialogView.findViewById<EditText>(R.id.editTextTime).text.toString()
                val place = dialogView.findViewById<EditText>(R.id.editTextPlace).text.toString()

                val messageRef = FirebaseDatabase.getInstance().getReference("Messages").push()
                val message = Message(
                    id = messageRef.key, // Imposta l'ID del messaggio
                    user_send = currentUserUsername,
                    user_recv = receiver.username,
                    text = text,
                    date = date,
                    time = time,
                    place = place,
                    response = null
                )

                messageRef.setValue(message)
                Toast.makeText(this, "Invite sent to ${receiver.username}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialogView.findViewById<EditText>(R.id.editTextDate).setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                dialogView.findViewById<EditText>(R.id.editTextDate).setText("$year-${month + 1}-$day")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogView.findViewById<EditText>(R.id.editTextTime).setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                dialogView.findViewById<EditText>(R.id.editTextTime).setText("$hourOfDay:$minute")
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        alertDialog.show()
    }
}
