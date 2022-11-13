package com.example.fastuga

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    //TODO FIX ERRORS/WARNINGS WITH LAYOUT AND (HONESTLY MAKE A DECENT DESIGN)

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberCheckBox: CheckBox
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var textInputLayoutEmail: TextInputLayout
    private lateinit var textInputLayoutPassword: TextInputLayout
    var requestQueue: RequestQueue? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        requestQueue = Volley.newRequestQueue(this)

        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        rememberCheckBox = findViewById(R.id.checkBoxRemember)
        loginButton = findViewById(R.id.btnLogin)
        registerTextView = findViewById(R.id.registerLink)
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail)
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword)

        loginButton.setOnClickListener {
            if (validateData()) {
                loginUser()
            }
        }

    }

    //GET ARRAY TESTE
    /*
    private fun getArrayRequest() {
        val url = "http://10.0.2.2/api/users"
        val request = JsonArrayRequest(Request.Method.GET, url, null, { response ->
            try {
                for (i in 0 until response.length()) {
                   var email = response.getJSONObject(i).get("email")
                    Log.w("@", email.toString())
                }
            } catch (error: JSONException) {
                Log.w("@", error.message.toString())
            }
        }, { error ->
            //error.printStackTrace()
            Log.w("@", error.message.toString())
            Log.w("@", error.networkResponse.statusCode.toString())
        })
        requestQueue?.add(request)
    }
    */

    //LOGIN OBJECT TESTE
    private fun loginUser() {
        val email: String = emailEditText.getText().toString().trim()
        val password: String = passwordEditText.getText().toString().trim()
        val url = "http://10.0.2.2/api/auth/login"

        val obj = JSONObject()
        obj.put("email", email)
        obj.put("password", password)
        if (rememberCheckBox.isChecked) {
            obj.put("remeber", rememberCheckBox.isChecked)
        }

        val jsObjRequest = JsonObjectRequest(
            Request.Method.POST, url, obj,
            { response ->
                //verify if remember was checked
                //obj.get("remember")
                //get remember token and save to shared preferences
                //go to dashboard
                startActivity(Intent(this, DashBoard::class.java))
            }
        ) { error ->
            val networkResponse = error.networkResponse
            if (networkResponse?.data != null) {
                //EXEMPLE ON HOW TO GET ERRO MESSAGES
                /*
                val jsonError = String(networkResponse.data)
                val json = JSONObject(jsonError)
                var message = json.get("message")
                Log.w("@", message.toString())
                Log.w("@", error.networkResponse.statusCode.toString())
                */
            }
        }
        requestQueue?.add(jsObjRequest)
    }

    private fun validateData(): Boolean {
        textInputLayoutEmail.isErrorEnabled = false;
        textInputLayoutPassword.isErrorEnabled = false;

        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        val passwordPattern = ""

        var validation = true

        if (!emailEditText.text.matches(emailPattern.toRegex())) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutEmail.error = "Email is not valid"
            textInputLayoutEmail.isErrorEnabled = true;
            validation = false
        }
        if (emailEditText.length() == 0) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutEmail.error = "Email is required"
            textInputLayoutEmail.isErrorEnabled = true;
            validation = false
        }

        //DECIDE WHAT IS NOT ACCEPTABLE FOR A PASSWORD
        if (passwordEditText.length() == 0) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutPassword.error = "Password is required"
            textInputLayoutPassword.isErrorEnabled = true;
            validation = false
        }

        //TODO
        /*
         if (!passwordEditText.text.matches(passwordPattern.toRegex())) {
             //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
             textInputLayoutPassword.error =
                 "Password should have X format (should not contain x-x-x-x-x)"
             textInputLayoutPassword.isErrorEnabled = true;
             validation = false
         }
         */

        return validation
    }


}
