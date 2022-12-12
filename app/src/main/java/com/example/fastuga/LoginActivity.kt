package com.example.fastuga

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject


class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberCheckBox: CheckBox
    private lateinit var registerTextView: TextView
    private lateinit var loginErrorTextView: TextView
    private lateinit var textInputLayoutEmail: TextInputLayout
    private lateinit var textInputLayoutPassword: TextInputLayout
    private lateinit var logo: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val myPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val accessToken = myPrefs.getString("access_token_rm", "")
        if (accessToken?.isNotEmpty() == true) {
            //already login
            val intent = Intent(applicationContext, DashBoard::class.java)
            startActivity(intent)
        }

        // deactivate dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //set logo in imageView
        logo = findViewById(R.id.imageView)
        logo.setImageResource(R.drawable.fastuga_logo);

        requestQueue = Volley.newRequestQueue(this)

        loginErrorTextView = findViewById(R.id.textLoginError)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        rememberCheckBox = findViewById(R.id.checkBoxRemember)
        registerTextView = findViewById(R.id.registerLink)
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail)
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword)
        progressBar = findViewById(R.id.progressBar);


        val loginButton = findViewById<View>(R.id.btnLogin) as Button

        loginButton.setOnClickListener(View.OnClickListener {
            if (validateData()) {
                loginUser()
            }
        })


        val registerLink = findViewById<View>(R.id.registerLink) as TextView
        registerLink.setOnClickListener(View.OnClickListener {
            val intent = Intent(applicationContext, RegisterActivity::class.java)
            startActivity(intent)
        })

    }

    private fun loginUser() {
        loginErrorTextView.visibility = View.GONE
        val email: String = emailEditText.text.toString().trim()
        val password: String = passwordEditText.text.toString().trim()
        val url = "http://10.0.2.2/api/auth/login"

        val obj = JSONObject()
        obj.put("email", email)
        obj.put("password", password)
        if (rememberCheckBox.isChecked) {
            obj.put("remember", rememberCheckBox.isChecked)
        }

        progressBar.visibility = View.VISIBLE
        val jsObjRequest = JsonObjectRequest(
            Request.Method.POST, url, obj,
            { response ->
                //verify if remember was checked
                val accessToken = response.getString("access_token")
                val userName = response.getString("name")
                val userEmail = response.getString("email")
                if (rememberCheckBox.isChecked) {
                    val sharedPreferences =
                        applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    //get auth access_token and save to shared preferences
                    editor.putString("access_token_rm", accessToken)
                    editor.putString("name", userName)
                    editor.putString("email", userEmail)
                    editor.apply();
                    //go to dashboard
                    val intent = Intent(applicationContext, DashBoard::class.java)
                    startActivity(intent)
                } else {
                    val sharedpreferences =
                        applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor: SharedPreferences.Editor = sharedpreferences.edit()
                    //get auth access_token and save to shared preferences
                    editor.putString("access_token", accessToken)
                    editor.putString("name", userName)
                    editor.putString("email", userEmail)
                    editor.apply();
                    //go to dashboard
                    val intent = Intent(applicationContext, DashBoard::class.java)
                    startActivity(intent)
                }
            }) { error ->
            val networkResponse = error.networkResponse
            if (networkResponse?.data != null) {
                loginErrorTextView.visibility = View.VISIBLE
                loginErrorTextView.text = "Login failed. Wrong credentials"
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

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

}
