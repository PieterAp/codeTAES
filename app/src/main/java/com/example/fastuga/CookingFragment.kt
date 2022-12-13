package com.example.fastuga

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.util.*

class CookingFragment : Fragment() {
    private lateinit var requestQueue: RequestQueue
    private lateinit var buttonPickUp: Button
    private lateinit var header_title: TextView
    private lateinit var refreshInst: TextView
    private lateinit var pullToRefreshStatus: SwipeRefreshLayout
    private lateinit var orderStatusImageView: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_cooking, container, false)
        this.header_title = rootView.findViewById<View>(R.id.header_title) as TextView
        this.refreshInst = rootView.findViewById<View>(R.id.refreshInst) as TextView
        this.buttonPickUp = rootView.findViewById<Button>(R.id.btnPickUpOrder) as Button
        this.pullToRefreshStatus = rootView.findViewById<View>(R.id.pullToRefreshStatus) as SwipeRefreshLayout
        this.orderStatusImageView = rootView.findViewById<ImageView>(R.id.orderStatusImageView) as ImageView
        val orderID = arguments!!.getInt("orderID")

        //disable button, API call could take time
        buttonPickUp.isEnabled = false
        buttonPickUp.setBackgroundColor(Color.parseColor("#999DA0"));

        //check if order is ready to be picked up
        orderReady(orderID)

        this.pullToRefreshStatus.setOnRefreshListener {
            //check if order status ir ready for pick-up
            orderReady(orderID)
        }

        buttonPickUp.setOnClickListener(View.OnClickListener {
            orderPickup(orderID)
        })

        return rootView
    }

    private fun orderPickup(orderID: Int) {
        //go to directions fragment and set new status
        val url = "http://10.0.2.2/api/orders/$orderID"
        requestQueue = Volley.newRequestQueue(context)
        var accessToken: String

        val obj = JSONObject()
        obj.put("status", "O")

        accessToken = this.activity!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("access_token_rm", "DEFAULT")!!
        if (accessToken == "DEFAULT") {
            accessToken = this.activity!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                .getString("access_token", "DEFAULT")!!
        }

        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.PUT, url,
            obj, Response.Listener {
                val bundle = Bundle()
                bundle.putInt("orderID", orderID)
                val myFragment: Fragment = ActiveOrderDetailsFragment()
                myFragment.arguments = bundle
                val transaction: FragmentTransaction = fragmentManager!!.beginTransaction()
                transaction.replace(R.id.fragment_container, myFragment)
                transaction.addToBackStack(null)
                transaction.commit()
            }, Response.ErrorListener { error ->
                error.networkResponse
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: HashMap<String, String> = HashMap()
                params["Authorization"] =
                    "Bearer $accessToken"
                params["Content-Type"] = "application/json"
                return params
            }

        }
        //region timeout policy
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        //endregion
        requestQueue.add(jsonObjectRequest)
    }

    //determine if order is ready to be picked up
    private fun orderReady(orderID: Int)  {
        val url = "http://10.0.2.2/api/orders/$orderID"

        requestQueue = Volley.newRequestQueue(context)
        var accessToken: String

        val sharedPreferences =
            context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        accessToken = sharedPreferences!!.getString("access_token_rm", "DEFAULT")!!
        if (accessToken == "DEFAULT") {
            accessToken = sharedPreferences.getString("access_token", "DEFAULT")!!
        }

        val jsonObjectRequest: StringRequest =
            object : StringRequest(Method.GET, url, Response.Listener { response ->
                val ordersJson = JSONObject(response)
                val order = ordersJson.getJSONObject("data")
                val orderStatus = order.getString("status")

                pullToRefreshStatus.isRefreshing = false
                if (orderStatus == "R") {
                    buttonPickUp.isEnabled = true
                    buttonPickUp.setBackgroundColor(Color.parseColor("#1034A6"));
                    header_title.text = "The order is ready to be picked up!"
                    refreshInst.text = "Press the button bellow after collecting the order"
                    //orderStatusImageView.setImageResource(R.drawable.scotter)
                    Glide.with(orderStatusImageView.context).load(R.drawable.scotter).into(orderStatusImageView)
                } else if (orderStatus == "P") {
                    buttonPickUp.isEnabled = false
                    buttonPickUp.setBackgroundColor(Color.parseColor("#999DA0"));
                    header_title.text = "The order is being prepared"
                    refreshInst.text = "Swipe down to refresh the status"
                    //orderStatusImageView.setImageResource(R.drawable.cooking)
                    Glide.with(orderStatusImageView.context).load(R.drawable.cooking).into(orderStatusImageView)
                }

            }, Response.ErrorListener { error ->
                error.networkResponse
                pullToRefreshStatus.isRefreshing = false
            }) {
                //region header config
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params: HashMap<String, String> = HashMap()
                    params["Authorization"] =
                        "Bearer $accessToken"
                    params["Content-Type"] = "application/json"
                    return params
                }
                //endregion
            }
        //region timeout policy
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        //endregion
        requestQueue.add(jsonObjectRequest)
    }
}