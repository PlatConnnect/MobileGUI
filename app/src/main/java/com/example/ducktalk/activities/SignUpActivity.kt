package com.example.ducktalk.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.ducktalk.activities.databinding.ActivitySignUpBinding
import com.example.ducktalk.activities.models.User
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream

class SignUpActivity : AppCompatActivity() {

    private var encodedImage: String? = null
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSignUp.setOnClickListener {
            // Validate input fields
            if (binding.inputName.text.toString().isEmpty()) {
                binding.inputName.error = getString(R.string.name)
                return@setOnClickListener
            }
            if (binding.inputEmail.text.toString().isEmpty()) {
                binding.inputEmail.error = getString(R.string.email)
                return@setOnClickListener
            }
            if (binding.inputPassword.text.toString().isEmpty()) {
                binding.inputPassword.error = getString(R.string.password)
                return@setOnClickListener
            }
            if (binding.inputConfirmPassword.text.toString().isEmpty()) {
                binding.inputConfirmPassword.error = getString(R.string.confirm_password)
                return@setOnClickListener
            }
            if (binding.inputPassword.text.toString() != binding.inputConfirmPassword.text.toString()) {
                binding.inputConfirmPassword.error = getString(R.string.confirm_password_not_same)
                return@setOnClickListener
            }

            // Register user with input data
            val name = binding.inputName.text.toString()
            val email = binding.inputEmail.text.toString()
            val password = binding.inputPassword.text.toString()

            // Create an instance of your user model class
            val user = encodedImage?.let { it1 -> User(name, email, password, it1) }

            // Save the user data to SharedPreferences or your database
            if (user != null) {
                registerUser(user)
            }
        }

        binding.textAddImage.setOnClickListener {
            pickImage.launch(Intent(Intent.ACTION_PICK).setType("image/*"))
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageData: Intent? = result.data
            imageData?.data?.let { imageUri ->
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                    val bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)
                    binding.imageProfile.setImageBitmap(bitmap)
                    binding.textAddImage.visibility = View.GONE
                    encodedImage = bitmap?.let { encodeImage(it) }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun registerUser(user: User) {
        // Instantiate the RequestQueue
        val queue = Volley.newRequestQueue(this)

        // Set up the URL and request headers
        val url = "http://ableytner.ddns.net:2007/user/register"
        val headers = HashMap<String, String>()
        headers["Content-Type"] = "application/json"

        // Create the request body
        val requestBody = Gson().toJson(user)

        // Create a POST request
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            url,
            null,
            { response ->
                // Handle the response from the server
                val success = response.getBoolean("success")
                if (success) {
                    // Show registration success message
                    Toast.makeText(this, R.string.toast_registration_successful, Toast.LENGTH_SHORT)
                        .show()
                    finish()
                } else {
                    val errorMessage = response.getString("message")
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                // Handle errors that occurred while making the request
                Toast.makeText(this, R.string.toast_registration_failure, Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }) {
            // Override the getBody method to return the request body
            override fun getBody(): ByteArray {
                return requestBody.toByteArray()
            }

            // Override the getHeaders method to return the request headers
            override fun getHeaders(): MutableMap<String, String> {
                return headers
            }
        }

        // Add the request to the RequestQueue
        queue.add(jsonObjectRequest)
    }
}