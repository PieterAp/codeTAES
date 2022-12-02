package com.example.fastuga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
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


class OrdersFragment : Fragment() {

    private lateinit var requestQueue: RequestQueue
    private lateinit var tvLoadingOrders: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getOrders()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val rootView: View = inflater.inflate(R.layout.fragment_orders, container, false)
        this.tvLoadingOrders = rootView.findViewById<View>(R.id.tvLoadingOrders) as TextView
        return rootView
    }

    private fun getOrders() {
        val url = "http://10.0.2.2/api/orders"
        var order: JSONObject

        requestQueue = Volley.newRequestQueue(context)

        val jsonObjectRequest: StringRequest =
            object : StringRequest(Method.GET, url, Response.Listener { response ->
                try {
                    val ordersJson = JSONObject("$response}")
                    val array = ordersJson.getJSONArray("data")
                    var distance = 0.0

                    val myOderData = Array(array.length()) { OrderModel() }
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
                    }

                    tvLoadingOrders.visibility = View.GONE

                    val recyclerView = view!!.findViewById<RecyclerView>(R.id.rvOrders)
                    val adapter = OrderAdapter(myOderData)
                    recyclerView.setHasFixedSize(true)
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.adapter = adapter

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }, Response.ErrorListener {
                tvLoadingOrders.text = "No orders to show"
            }) {
                //region header config
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params: HashMap<String, String> = HashMap()
                    params["Authorization"] =
                        "Bearer " + "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIyIiwianRpIjoiMjIxY2RjN2RiMTE4ZWQ4NDY2Y2JmN2Y3MTliNWU4NjE4MjVkNmM3ZDA4Zjk0MDA2Mzg3OGIxYjViMGEwM2E1YmFjYmFiMzQzY2JiM2QzNWEiLCJpYXQiOjE2Njk5MTc0MDIuMTkxNDU2LCJuYmYiOjE2Njk5MTc0MDIuMTkxNDU5LCJleHAiOjE3MDE0NTM0MDEuOTMwMDM1LCJzdWIiOiIyMjUiLCJzY29wZXMiOlsiKiJdfQ.A7MLmAJMY5x0h4LwgJ8Vt9Y298GVxm9LUlfEiIiY-pbfVO0_HQRV0rEL5RH5DVmcx1mZ5Tj2iMNM6kv2tQCuCEfo2hnffuGaWfQelq-oNS6rkndBtSzAxd5kU9_voN0qYakBmAhuoMV0ZOpiP5OxTw4FpeloUKaP4yoEecqQwWuHRt04sJnoPVwLQLli3xnuK7llwBlORpC_zULriNk6SGWk581ssfGgQJRCp0Qlry6Nx3gDEaLuPY_ZuZrQzH5-L7uAhK9qVlGr9PO9vzu3Y95GyK5Trxo2yYiZ6xbiFlrQy8M0zfxMiCMdurF-8F8enrwozFdNvycjYkic95bkipaAqKaT7SR63D8cnmBmk6EfaLVRTCYyx1_DhA2PXFj85JJac2d8jlGiVs1Ndu1kH3kbIXRHuGaFc_MLxw1hfhx6zQ8T_z3m-ZfQz3ax_uVCSDqX7v035yRkU8j6fpaLX2VzQXcoqUgiVMhwWshlfKPfYLgxcveMe2wAQ183ey8EW_HVEK25IKgpExkBaGwXFFt5Dyea31zda0ZS-SAwu29sCCg3X8VyiXfCZd8RUr-WqWvnYlqbh4DQ31tRt2aIQhovIH2HhEGLmDrTcHgdDDsrptZ8pGQ7rE8PDOmDPpnvjmh9iyzKZqW_Y65Iq43G6pRpBGSyZY__IxxuRciRab8"
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