package com.example.fastuga

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.lang.Double


class ActiveOrders : Fragment() {

    private lateinit var requestQueue: RequestQueue
    private lateinit var tvAOLoadingOrders: TextView
    private var activeOrdersTag: String = "activeOrderTag"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getActiveOrders()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView: View = inflater.inflate(R.layout.fragment_active_orders, container, false)
        this.tvAOLoadingOrders = rootView.findViewById<View>(R.id.tvAOLoadingOrders) as TextView
        return rootView
    }

    private fun getActiveOrders() {
        val url = "http://10.0.2.2/api/users/orders"
        var order: JSONObject

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
                try {
                    val ordersJson = JSONObject("$response}")
                    val array = ordersJson.getJSONArray("data")
                    var distance = 0.0

                    val myOderData = Array(array.length()) { OrderModel() }
                    if (myOderData.isEmpty()) {
                        tvAOLoadingOrders.text = "No active orders to show"
                    } else {
                        for (i in 0 until array.length()) {
                            order = array.getJSONObject(i)
                            distance = try {
                                Double.parseDouble(order.getString("delivery_distance"))
                            } catch (e: NumberFormatException) {
                                0.0
                            }
                            myOderData[i] = OrderModel(
                                order.getString("id").toInt(),
                                order.getString("created_at"),
                                order.getString("customer_name"),
                                order.getString("pickup_address"),
                                order.getString("delivery_address"),
                                order.getString("delivered_by"),
                                order.getString("status"),
                                order.getString("ticket_number").toInt(),
                                0,
                                distance
                            )

                            tvAOLoadingOrders.visibility = View.GONE
                        }
                    }

                    val recyclerView = view!!.findViewById<RecyclerView>(R.id.rvActiveOrders)
                    val adapter = ActiveOrderAdapter(myOderData)
                    recyclerView.setHasFixedSize(true)
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.adapter = adapter

                } catch (e: JSONException) {
                    tvAOLoadingOrders.text = "No active orders to show"
                }
            }, Response.ErrorListener {
                tvAOLoadingOrders.text = "No active orders to show"
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
        jsonObjectRequest.tag = activeOrdersTag
        //region timeout policy
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        //endregion
        requestQueue.add(jsonObjectRequest)
    }


    override fun onDetach() {
        super.onDetach()
        requestQueue.cancelAll(activeOrdersTag)
    }
}