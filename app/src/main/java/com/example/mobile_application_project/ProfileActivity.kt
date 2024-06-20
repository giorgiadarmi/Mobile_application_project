package com.example.mobile_application_project

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.mobile_application_project.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var database: DatabaseReference
    private lateinit var tempImgUri: Uri
    private lateinit var imgUri: Uri
    private lateinit var binding: ActivityProfileBinding
    private val tempImgFileName = "tempImg.jpg"
    private val imgFileName = "image.jpg"
    private lateinit var cameraResult: ActivityResultLauncher<Intent>
    private lateinit var albumResult: ActivityResultLauncher<Intent>
    private lateinit var firebaseAuth: FirebaseAuth

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "MyRuns2-TheUserProfile"

        Util.checkPermissions(this)
        val view: View = findViewById(R.id.info)
        onLoad(view)

        imageView = findViewById(R.id.imageProfile)

        val tempImgFile = File(getExternalFilesDir(null), tempImgFileName)
        val imgFile = File(getExternalFilesDir(null), imgFileName)
        tempImgUri = FileProvider.getUriForFile(this, "com.example.mobile_application_project", tempImgFile)
        imgUri = FileProvider.getUriForFile(this, "com.example.mobile_application_project", imgFile)

        if (imgFile.exists()) {
            val bitmap = Util.getBitmap(this, imgUri)
            val bitmapRotate = rotateBitmap(this, -90, bitmap)
            imageView.setImageBitmap(bitmapRotate)
        }

        cameraResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = Util.getBitmap(this, tempImgUri)
                val bitmapRotate = rotateBitmap(this, -90, bitmap)
                imageView.setImageBitmap(bitmapRotate)
                uploadImageToFirebase(tempImgUri)
            }
        }

        albumResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val albumUrl: Uri = data?.data!!
                compressImg(this, File(getExternalFilesDir(null), tempImgFileName), albumUrl)
                val bitmap = Util.getBitmap(this, albumUrl)
                val bitmapRotate = rotateBitmap(this, -90, bitmap)
                imageView.setImageBitmap(bitmapRotate)
                uploadImageToFirebase(albumUrl)
            }
        }
    }

    fun onChangeButtonClicked(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Profile Picture")
        builder.setItems(arrayOf("Open Camera", "Select from Gallery")) { _, pos ->
            when (pos) {
                0 -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, tempImgUri)
                    cameraResult.launch(intent)
                }
                else -> {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    albumResult.launch(intent)
                }
            }
        }
        builder.create().show()
    }

    fun onSaveButtonClicked(view: View) {
        if (File(getExternalFilesDir(null), tempImgFileName).exists()) {
            val tempImgFile = File(getExternalFilesDir(null), tempImgFileName)
            val imgFile = File(getExternalFilesDir(null), imgFileName)
            tempImgFile.renameTo(imgFile)
        }
        val sp: SharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sp.edit()
        val name = binding.editMessageName.text.toString()
        val surname = binding.editMessageSurname.text.toString()
        val email = binding.editMessageEmail.text.toString()
        val age = binding.editMessageAge.text.toString()
        val username = binding.editMessageUsername.text.toString()

        updateData(name, surname, email, age, username)

        editor.apply()
        Toast.makeText(applicationContext, "Saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateData(name: String, surname: String, email: String, age: String, username: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            database = FirebaseDatabase.getInstance().getReference("Users")
            val user = mapOf(
                "name" to name,
                "surname" to surname,
                "email" to email,
                "age" to age,
                "username" to username
            )
            database.child(uid).updateChildren(user).addOnSuccessListener {
                binding.editMessageName.text!!.clear()
                binding.editMessageSurname.text!!.clear()
                binding.editMessageEmail.text!!.clear()
                binding.editMessageAge.text!!.clear()
                binding.editMessageUsername.text!!.clear()
                Toast.makeText(this, "Successfully Updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to Update", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onCancelButtonClicked(view: View) {
        onLoad(view)
        val tempImgFile = File(getExternalFilesDir(null), tempImgFileName)
        tempImgFile.delete()
        val sp: SharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sp.edit()

        editor.apply()
        finish()
    }

    fun onLoad(view: View) {
        val sp: SharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            readData(uid)
        }
    }

    private fun readData(uid: String) {
        database = FirebaseDatabase.getInstance().getReference("Users")
        database.child(uid).get().addOnSuccessListener {
            if (it.exists()) {
                val name = it.child("name").value.toString()
                val surname = it.child("surname").value.toString()
                val email = it.child("email").value.toString()
                val age = it.child("age").value.toString()
                val username = it.child("username").value.toString()
                val imageUrl = it.child("imageUrl").value.toString()

                Toast.makeText(this, "Successfully Read", Toast.LENGTH_SHORT).show()
                binding.name.text = name
                binding.surname.text = surname
                binding.email.text = email
                binding.age.text = age
                binding.username.text = username

                if (imageUrl.isNotEmpty()) {
                    loadImage(imageUrl)
                }
            } else {
                Toast.makeText(this, "User Doesn't Exist", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rotateBitmap(context: Context, degree: Int, srcBitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.reset()
        matrix.setRotate(degree.toFloat())
        return Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val storageReference = FirebaseStorage.getInstance().reference.child("profileImages/$uid.jpg")

            // Log URI for debugging
            Log.d("ProfileActivity", "Uploading image to: ${storageReference.path}")
            Log.d("ProfileActivity", "Image URI: $imageUri")

            storageReference.putFile(imageUri)
                .addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        Log.d("ProfileActivity", "Download URL: $uri")
                        updateImageUrl(uri.toString())
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileActivity", "Failed to upload image", exception)
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImageUrl(imageUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            database = FirebaseDatabase.getInstance().getReference("Users")
            Log.d("ProfileActivity", "Updating image URL for user: $uid")
            database.child(uid).child("imageUrl").setValue(imageUrl)
                .addOnSuccessListener {
                    Toast.makeText(this, "Image URL Updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileActivity", "Failed to update image URL", exception)
                    Toast.makeText(this, "Failed to update image URL", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImage(imageUrl: String) {
        try {
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageView.setImageBitmap(bitmap)
            }.addOnFailureListener { exception ->
                Log.e("ProfileActivity", "Failed to load image", exception)
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IllegalArgumentException) {
            Log.e("ProfileActivity", "Invalid image URL", e)
            Toast.makeText(this, "Invalid image URL", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, 1, 1, "setting")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> Toast.makeText(applicationContext, "setting", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    fun compressImg(context: Context, tempImgFile: File, tempImgUri: Uri) {
        val fileOutputStream = FileOutputStream(tempImgFile)
        val bitmapTemp = BitmapFactory.decodeStream(contentResolver.openInputStream(tempImgUri))
        bitmapTemp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
    }
}
