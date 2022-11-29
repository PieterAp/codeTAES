package com.example.fastuga

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.textfield.TextInputLayout
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var licensePlateEditText: EditText
    private lateinit var loginTextView: TextView
    private lateinit var registerErrorTextView: TextView
    private lateinit var textInputLayoutEmail: TextInputLayout
    private lateinit var textInputLayoutPhoneNumber: TextInputLayout
    private lateinit var textInputLayoutPassword: TextInputLayout
    private lateinit var textInputLayoutName: TextInputLayout
    private lateinit var textInputLayoutConfirmPassword: TextInputLayout
    private lateinit var textInputLayoutLicensePlate: TextInputLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        // deactivate dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        nameEditText = findViewById(R.id.editTextName)
        emailEditText = findViewById(R.id.editTextEmail)
        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber)
        passwordEditText = findViewById(R.id.editTextPassword)
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword)
        licensePlateEditText = findViewById(R.id.editTextLicensePlate)
        loginTextView = findViewById(R.id.loginLink)

        registerErrorTextView = findViewById(R.id.textRegisterError)

        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail)
        textInputLayoutPhoneNumber = findViewById(R.id.textInputLayoutPhoneNumber)
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword)
        textInputLayoutName = findViewById(R.id.textInputLayoutName)
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword)
        textInputLayoutLicensePlate = findViewById(R.id.textInputLayoutLicensePlate)

        progressBar = findViewById(R.id.progressBar);
        requestQueue = Volley.newRequestQueue(this)

        val registerButton = findViewById<View>(R.id.btnRegister) as Button

        registerButton.setOnClickListener(View.OnClickListener{
            if (validateData()){
                registerUser()
            }
        })

        val loginLink = findViewById<View>(R.id.loginLink) as TextView
        loginLink.setOnClickListener(View.OnClickListener{
            val intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(intent)
        })

    }

    private fun registerUser(){
        registerErrorTextView.visibility = View.GONE
        val email: String = emailEditText.text.toString().trim()
        val phoneNumber : String = phoneNumberEditText.text.toString().trim()
        val password: String = passwordEditText.text.toString().trim()
        val name: String = nameEditText.text.toString().trim()
        val password_confirmation: String = confirmPasswordEditText.text.toString().trim()
        val license_plate: String = licensePlateEditText.text.toString().trim()
        val url = "http://10.0.2.2/api/auth/register"

        val obj = JSONObject()
        obj.put("email", email)
        obj.put("phone",phoneNumber)
        obj.put("password", password)
        obj.put("password_confirmation", password_confirmation)
        obj.put("name", name)
        obj.put("license_plate", license_plate)
        progressBar.visibility = View.VISIBLE

        val jsObjRequest = JsonObjectRequest(
            Request.Method.POST, url, obj,
            { response ->
                //verify if remember was checked
                //go to login
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }) { error ->
            val networkResponse = error.networkResponse
            if (networkResponse?.data != null) {
                registerErrorTextView.visibility = View.VISIBLE
                registerErrorTextView.text = "The email has already been taken."
                progressBar.visibility = View.GONE
            }
        }
        jsObjRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(jsObjRequest)

    }

    private fun validateData() : Boolean{

        textInputLayoutEmail.isErrorEnabled = false
        textInputLayoutPhoneNumber.isErrorEnabled = false
        textInputLayoutPassword.isErrorEnabled = false
        textInputLayoutName.isErrorEnabled = false
        textInputLayoutConfirmPassword.isErrorEnabled = false
        textInputLayoutLicensePlate.isErrorEnabled = false


        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        val phoneNumberPattern = "^[+]?[0-9]{9,13}\$"
        val namePattern = "^[A-z][\\sA-z]*\$"
        val licencePlatePattern = "(^(?:[A-Z]{2}-\\d{2}-\\d{2})|(?:\\d{2}-[A-Z]{2}-\\d{2})|(?:\\d{2}-\\d{2}-[A-Z]{2})|(?:[A-Z]{2}-\\d{2}-[A-Z]{2})\$)"

        var validation = true

        if (!nameEditText.text.matches(namePattern.toRegex()) ){
            textInputLayoutName.error = "Name is invalid"
            textInputLayoutName.isErrorEnabled = true
            validation = false
        }

        if (nameEditText.length() == 0){
            textInputLayoutName.error = "Name is required"
            textInputLayoutName.isErrorEnabled = true
            validation = false
        }


        if (!phoneNumberEditText.text.matches(phoneNumberPattern.toRegex())){
            textInputLayoutPhoneNumber.error = "Phone Number is invalid"
            textInputLayoutPhoneNumber.isErrorEnabled = true
            validation = false
        }


        if (phoneNumberEditText.length() == 0){
            textInputLayoutPhoneNumber.error = "Phone Number is required"
            textInputLayoutPhoneNumber.isErrorEnabled = true
            validation = false
        }

        if (!emailEditText.text.matches(emailPattern.toRegex())) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutEmail.error = "Email is not valid"
            textInputLayoutEmail.isErrorEnabled = true
            validation = false
        }

        if (emailEditText.length() == 0) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutEmail.error = "Email is required"
            textInputLayoutEmail.isErrorEnabled = true
            validation = false
        }

        if (passwordEditText.length() < 3) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutPassword.error = "Password is invalid"
            textInputLayoutPassword.isErrorEnabled = true
            validation = false
        }

        //DECIDE WHAT IS NOT ACCEPTABLE FOR A PASSWORD
        if (passwordEditText.length() == 0) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutPassword.error = "Password is required"
            textInputLayoutPassword.isErrorEnabled = true
            validation = false
        }

        if (confirmPasswordEditText.text.toString() != passwordEditText.text.toString()){
            textInputLayoutConfirmPassword.error = "Passwords do not match"
            textInputLayoutConfirmPassword.isErrorEnabled = true
            validation = false
        }

        if (confirmPasswordEditText.length() < 3) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutConfirmPassword.error = "Confirm Password is invalid"
            textInputLayoutConfirmPassword.isErrorEnabled = true
            validation = false
        }

        if (confirmPasswordEditText.length() == 0){
            textInputLayoutConfirmPassword.error = "Confirm Password is required"
            textInputLayoutConfirmPassword.isErrorEnabled = true
            validation = false
        }

        if (!licensePlateEditText.text.matches(licencePlatePattern.toRegex())){
            textInputLayoutLicensePlate.error = "License Plate is invalid"
            textInputLayoutLicensePlate.isErrorEnabled = true
            validation = false
        }

        if (licensePlateEditText.length() == 0){
            textInputLayoutLicensePlate.error = "License Plate is required"
            textInputLayoutLicensePlate.isErrorEnabled = true
            validation = false
        }

        return validation
    }
}