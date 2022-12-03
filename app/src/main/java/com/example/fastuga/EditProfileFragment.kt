package com.example.fastuga

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONException
import org.json.JSONObject

class EditProfileFragment : Fragment() {

    private lateinit var requestQueue: RequestQueue
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var licensePlateEditText: EditText
    private lateinit var editErrorTextView: TextView
    private lateinit var textInputLayoutEmail: TextInputLayout
    private lateinit var textInputLayoutPhoneNumber: TextInputLayout
    private lateinit var textInputLayoutPassword: TextInputLayout
    private lateinit var textInputLayoutName: TextInputLayout
    private lateinit var textInputLayoutLicensePlate: TextInputLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarLoad: ProgressBar
    lateinit var layout: ConstraintLayout
    lateinit var access_token: String
    private lateinit var loadEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestQueue = Volley.newRequestQueue(context)
        val sharedpreferences = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        access_token = sharedpreferences?.getString("access_token","DEFAULT")!!
    }

    //graphical initialization
    //is called after onCreate
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        nameEditText = view.findViewById(R.id.editTextNameFragment)
        emailEditText = view.findViewById(R.id.editTextEmailFragment)
        phoneNumberEditText = view.findViewById(R.id.editTextPhoneNumberFragment)
        passwordEditText = view.findViewById(R.id.editTextPasswordFragment)
        licensePlateEditText = view.findViewById(R.id.editTextLicensePlateFragment)

        editErrorTextView = view.findViewById(R.id.textEditErrorFragment)

        textInputLayoutEmail = view.findViewById(R.id.textInputLayoutEmailFragment)
        textInputLayoutPhoneNumber = view.findViewById(R.id.textInputLayoutPhoneNumberFragment)
        textInputLayoutPassword = view.findViewById(R.id.textInputLayoutPasswordFragment)
        textInputLayoutName = view.findViewById(R.id.textInputLayoutNameFragment)
        textInputLayoutLicensePlate = view.findViewById(R.id.textInputLayoutLicensePlateFragment)
        //progressBar = view.findViewById(R.id.progressBar)

        progressBarLoad = view.findViewById(R.id.progressBarLoad) as ProgressBar
        layout = view.findViewById(R.id.layout)
        loadEditText = view.findViewById(R.id.editTextLoading)

        getData()

        val editButton = view.findViewById<View>(R.id.btnEdit) as Button


        editButton.setOnClickListener(View.OnClickListener{
            if (validateData()){
                editUser()
                val toast = Toast.makeText(this.context,"Edit with success", Toast.LENGTH_LONG)
                toast.view?.setBackgroundColor(Color.parseColor("#198754"))
                toast.show()
            }

        })

        return view
    }

    private fun editUser(){
        val url = "http://10.0.2.2/api/users/profile"


        val stringRequest = object : StringRequest(
            Method.PUT, url,
            Response.Listener
            {

            }, Response.ErrorListener {})
        {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["name"] = nameEditText.text.toString()
                params["phone"] = phoneNumberEditText.text.toString()
                params["email"] = emailEditText.text.toString()

                //says that if has empty body but this works
                if (passwordEditText.text.isNotEmpty()){
                    params["password"] = passwordEditText.text.toString()
                }

                params["license_plate"] = licensePlateEditText.text.toString()
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] =
                    "Bearer $access_token"
                return headers
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(stringRequest)
    }

    private fun getData(){
        val url = "http://10.0.2.2/api/users/profile"

        requestQueue = Volley.newRequestQueue(context)

        val stringRequest = object : StringRequest(
            Method.GET, url,
            Response.Listener
            { response ->
                try {

                    val jsonObject = JSONObject(response)
                    val data = jsonObject.getJSONObject("data")

                    val name: String = data.getString("name")
                    nameEditText.setText(name)

                    val email: String = data.getString("email")
                    emailEditText.setText(email)

                    val phone: Int = data.getInt("phone")
                    phoneNumberEditText.setText(phone.toString())

                    val license_plate: String = data.getString("license_plate")
                    licensePlateEditText.setText(license_plate)

                    loadEditText.visibility = View.GONE
                    progressBarLoad.visibility = View.GONE
                    layout.visibility = View.GONE

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }, Response.ErrorListener { error ->
                error.networkResponse
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] =
                    "Bearer $access_token"
                return headers
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(stringRequest)

    }

    private fun validateData() : Boolean{

        textInputLayoutEmail.isErrorEnabled = false
        textInputLayoutPhoneNumber.isErrorEnabled = false
        textInputLayoutPassword.isErrorEnabled = false
        textInputLayoutName.isErrorEnabled = false
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

        if (!phoneNumberEditText.text.matches(phoneNumberPattern.toRegex())){
            textInputLayoutPhoneNumber.error = "Phone Number is invalid"
            textInputLayoutPhoneNumber.isErrorEnabled = true
            validation = false
        }

        if (!emailEditText.text.matches(emailPattern.toRegex())) {
            //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
            textInputLayoutEmail.error = "Email is not valid"
            textInputLayoutEmail.isErrorEnabled = true
            validation = false
        }


        if (!passwordEditText.text.isEmpty()){
            if (passwordEditText.length() < 3) {
                //CHANGE ERROR PLACE FOR A MORE VISIBLE PLACE
                textInputLayoutPassword.error = "Password is invalid"
                textInputLayoutPassword.isErrorEnabled = true
                validation = false
            }
        }

        if (!licensePlateEditText.text.matches(licencePlatePattern.toRegex())){
            textInputLayoutLicensePlate.error = "License Plate is invalid"
            textInputLayoutLicensePlate.isErrorEnabled = true
            validation = false
        }

        return validation
    }

}