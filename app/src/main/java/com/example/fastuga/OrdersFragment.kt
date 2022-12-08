package com.example.fastuga

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.lang.Double
import java.util.*
import kotlin.Array
import kotlin.Int
import kotlin.Long
import kotlin.NumberFormatException
import kotlin.String
import kotlin.Throws
import kotlin.also


class OrdersFragment : Fragment() {

    private lateinit var requestQueue: RequestQueue
    private lateinit var tvLoadingOrders: TextView
    private lateinit var pullToRefresh: SwipeRefreshLayout
    private lateinit var spinnerDistance: Spinner
    private lateinit var myOderData: Array<OrderModel>
    private lateinit var adapter: OrderAdapter
    private var ordersTag: String = "orderTag"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getOrders()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.title = "Orders"
        // Inflate the layout for this fragment
        val rootView: View = inflater.inflate(R.layout.fragment_orders, container, false)
        this.tvLoadingOrders = rootView.findViewById<View>(R.id.tvLoadingOrders) as TextView
        this.pullToRefresh = rootView.findViewById<View>(R.id.pullToRefresh) as SwipeRefreshLayout
        this.spinnerDistance = rootView.findViewById<View>(R.id.spinnerDistance) as Spinner

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            context!!,
            R.array.distance,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinnerDistance.adapter = adapter
        }

        pullToRefresh.setOnRefreshListener {
            tvLoadingOrders.text = "Loading Orders ..."
            getOrders()
            pullToRefresh.isRefreshing = false
        }

        return rootView
    }

    private fun getOrders() {
        val url = "http://10.0.2.2/api/drivers/orders"
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

                    myOderData = Array(array.length()) { OrderModel() }
                    if (myOderData.isEmpty()) {
                        tvLoadingOrders.text = "No orders to show"
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
                                order.getString("ticket_number").toInt(),
                                0,
                                distance
                            )

                            tvLoadingOrders.visibility = View.GONE
                        }
                    }

                    val recyclerView = view!!.findViewById<RecyclerView>(R.id.rvOrders)
                    adapter = OrderAdapter(myOderData)
                    recyclerView.setHasFixedSize(true)
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.adapter = adapter

                    spinnerDistance.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>, view: View, pos: Int,
                                id: Long
                            ) {
                                if (myOderData.isNotEmpty()) {
                                    if (pos == 0) {
                                        myOderData.sortWith { lhs, rhs ->
                                            if (lhs.distance < rhs.distance) -1 else if (lhs.ticketNumber < rhs.ticketNumber) 1 else 0
                                        }
                                        adapter.notifyDataSetChanged()
                                    } else {
                                        myOderData.sortWith { lhs, rhs ->
                                            if (lhs.distance > rhs.distance) -1 else if (lhs.ticketNumber < rhs.ticketNumber) 1 else 0
                                        }
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                myOderData.sortWith { lhs, rhs ->
                                    if (lhs.distance < rhs.distance) -1 else if (lhs.ticketNumber < rhs.ticketNumber) 1 else 0
                                }
                                adapter.notifyDataSetChanged()
                            }
                        }

                } catch (e: JSONException) {
                    tvLoadingOrders.text = "No orders to show"
                }
            }, Response.ErrorListener {
                tvLoadingOrders.text = "No orders to show"
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
        jsonObjectRequest.tag = ordersTag
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        //endregion
        requestQueue.add(jsonObjectRequest)
    }

    override fun onDetach() {
        super.onDetach()
        requestQueue.cancelAll(ordersTag)
    }
}