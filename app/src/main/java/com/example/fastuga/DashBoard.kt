package com.example.fastuga

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Objects

class DashBoard : AppCompatActivity() {
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash_board)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)

        val toggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle);
        toggle.syncState()

        requestQueue = Volley.newRequestQueue(this)

        val logoutButton = findViewById<View>(R.id.nav_logout) as TextView

        logoutButton.setOnClickListener(View.OnClickListener {
            logoutUser()
        })
    }

    private fun logoutUser() {
        val url = "http://10.0.2.2/api/auth/logout"

        //retrieve token from shared preferences
        val sharedpreferences = applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val accessToken = sharedpreferences.getString("access_token", "DEFAULT")

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener
            { response ->
                //remove token from shared preferences
                val editor: SharedPreferences.Editor = sharedpreferences.edit()
                editor.clear()
                editor.remove("access_token")
                editor.apply();

                //go to login
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)

            }, Response.ErrorListener { error -> error.networkResponse })
        {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $accessToken"
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

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed();
        }
    }
}