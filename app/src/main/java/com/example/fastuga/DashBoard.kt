package com.example.fastuga

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.navigation.NavigationView

class DashBoard : AppCompatActivity() {
    private lateinit var requestQueue: RequestQueue
    private lateinit var profileImageView: ImageView
    private lateinit var fragmentManager: FragmentManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash_board)

        fragmentManager = supportFragmentManager

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val header: View = navigationView.getHeaderView(0)
        profileImageView = header.findViewById<View>(R.id.profileImageNav) as ImageView

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

        profileImageView.setOnClickListener {
            val intent = Intent(applicationContext, ProfileActivity::class.java)
            startActivity(intent)
            drawer.close()
        }

        val logoutButton = findViewById<View>(R.id.nav_logout) as TextView

        supportActionBar!!.title = "Orders"
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, OrdersFragment())
        transaction.commit()


        navigationView.menu.findItem(R.id.orderListOrders).setOnMenuItemClickListener {
            supportActionBar!!.title = "Orders"
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, OrdersFragment())
            transaction.commit()
            drawer.close()
            true
        }

        navigationView.menu.findItem(R.id.activeOrder).setOnMenuItemClickListener {
            supportActionBar!!.title = "Active Order"
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, ActiveOrders())
            transaction.commit()
            drawer.close()
            true
        }

        logoutButton.setOnClickListener(View.OnClickListener {
            logoutUser()
        })
    }

    private fun logoutUser() {
        val url = "http://10.0.2.2/api/auth/logout"
        var accessToken: String

        //retrieve token from shared preferences
        val sharedPreferences =
            applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        accessToken = sharedPreferences.getString("access_token_rm", "DEFAULT")!!
        if (accessToken == "DEFAULT") {
            accessToken = sharedPreferences.getString("access_token", "DEFAULT")!!
        }

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener
            {
                //remove token from shared preferences
                if (sharedPreferences.getString("access_token_rm", "DEFAULT") == "DEFAULT") {
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    editor.clear()
                    editor.remove("access_token")
                    editor.apply()

                    //go to login
                    val intent = Intent(applicationContext, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    editor.clear()
                    editor.remove("access_token_rm")
                    editor.apply()

                    //go to login
                    val intent = Intent(applicationContext, LoginActivity::class.java)
                    startActivity(intent)
                }

                val preferences = getSharedPreferences("myBalance", 0)
                preferences?.edit()?.remove("balanceString")?.commit()

                val sharedpreferencesCustomer = getSharedPreferences("myCustomers", Context.MODE_PRIVATE)
                sharedpreferencesCustomer.edit().remove("customer_size").commit()
                sharedpreferencesCustomer.edit().remove("deliveries").commit()
                sharedpreferencesCustomer.edit().remove("totalTime").commit()
            },
            Response.ErrorListener
            { error ->
                error.networkResponse
            }) {
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