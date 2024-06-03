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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
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
import java.io.File
import java.io.FileOutputStream

// copy from the user interface (myruns1)
class ProfileActivity : AppCompatActivity() {
    // variable for change button
    private lateinit var imageView: ImageView
    private lateinit var database : DatabaseReference
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
        val view: View = findViewById(R.id.info);
        onLoad(view)

        imageView = findViewById(R.id.imageProfile)

        val tempImgFile = File(getExternalFilesDir(null), tempImgFileName)
        val imgFile = File(getExternalFilesDir(null), imgFileName)
        tempImgUri = FileProvider.getUriForFile(this, "com.example.mobile_application_project", tempImgFile)
        imgUri =  FileProvider.getUriForFile(this, "com.example.mobile_application_project", imgFile)

        if(imgFile.exists()) {
            val bitmap = Util.getBitmap(this, imgUri)
            var bitmapRotate = rotateBimap(this, -90, bitmap)
            imageView.setImageBitmap(bitmapRotate)
        }

        cameraResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if(result.resultCode == Activity.RESULT_OK){
                // here rotate for -90 degrees aas the image saved is 90 degrees rotated
                val bitmap = Util.getBitmap(this, tempImgUri)
                var bitmapRotate = rotateBimap(this, -90, bitmap)
                imageView.setImageBitmap(bitmapRotate)
            }
        }

        albumResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                val albumUrl : Uri = data?.data!!
                compressImg(this, File(getExternalFilesDir(null), tempImgFileName), albumUrl)
                // here rotate for -90 degrees aas the image saved is 90 degrees rotated
                val bitmap = Util.getBitmap(this, albumUrl)
                var bitmapRotate = rotateBimap(this, -90, bitmap)

                imageView.setImageBitmap(bitmapRotate)
            }
        }
    }

    fun onChangeButtonClicked(view: View) {
        // set up a alertdialog for user to choose take a photo or from album
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Profile Picture")
        builder.setItems(arrayOf("Open Camera", "Select from Gallery")) { _, pos ->
            when(pos){
                0->{
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, tempImgUri)
                    cameraResult.launch(intent)
                }
                else->{
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    albumResult.launch(intent)
                }
            }

        }
        builder.create().show()
    }

    fun onSaveButtonClicked(view: View) {

        if(File(getExternalFilesDir(null), tempImgFileName).exists()){
            val tempImgFile = File(getExternalFilesDir(null), tempImgFileName)
            val imgFile = File(getExternalFilesDir(null), imgFileName)
            tempImgFile.renameTo(imgFile)
        }
        val sp: SharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sp.edit()

        val first_name: EditText = findViewById(R.id.edit_message_name)
        editor.putString("Name", first_name.text.toString().trim())
        val email: EditText = findViewById(R.id.edit_message_email)
        editor.putString("Email", email.text.toString().trim())
        val ageNum: EditText = findViewById(R.id.edit_message_age)
        editor.putString("ClassNum", ageNum.text.toString().trim())
        val username: EditText = findViewById(R.id.edit_message_username)
        editor.putString("Username", username.text.toString().trim())

        editor.apply()
        Toast.makeText(applicationContext, "saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    fun onCancelButtonClicked(view: View) {
        onLoad(view)
        val tempImgFile = File(getExternalFilesDir(null), tempImgFileName)
        tempImgFile.delete()
        val sp: SharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sp.edit()

        editor.apply();
        finish()
    }

    fun onLoad(view: View) {
        val sp: SharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        val UID = FirebaseAuth.getInstance().getCurrentUser()?.getUid()
        readData(UID.toString())
    }

    private fun readData(UID: String) {
        database = FirebaseDatabase.getInstance().getReference("Users")
        database.child(UID.toString()).get().addOnSuccessListener {
            if (it.exists()){
                val name = it.child("name").value
                val email = it.child("email").value
                val age = it.child("age").value
                val username = it.child("username").value
                Toast.makeText(this,"Successfuly Read",Toast.LENGTH_SHORT).show()
                binding.name.text = name.toString()
                binding.email.text = email.toString()
                binding.age.text = age.toString()
                binding.username.text = username.toString()
            }else{
                Toast.makeText(this,"User Doesn't Exist",Toast.LENGTH_SHORT).show()
            }

        }.addOnFailureListener{
            Toast.makeText(this,"Failed",Toast.LENGTH_SHORT).show()
        }

    }
    private fun rotateBimap(context: Context, degree: Int, srcBitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.reset()
        matrix.setRotate(degree.toFloat())
        return Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
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

    fun compressImg(context: Context, tempImaFile : File,  tempImgUri: Uri){
        var fileOutputStream  = FileOutputStream(tempImaFile)
        var bitmapTemp = BitmapFactory.decodeStream(contentResolver.openInputStream(tempImgUri))
        bitmapTemp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
    }


}