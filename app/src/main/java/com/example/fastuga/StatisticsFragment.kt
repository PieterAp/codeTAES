package com.example.fastuga

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import org.json.JSONException
import org.json.JSONObject


class StatisticsFragment : Fragment() {
    private lateinit var pieChart: PieChart
    private var balance: Float = 0.0f

    private lateinit var requestQueue: RequestQueue
    lateinit var access_token: String

    var customersArray: ArrayList<String> = ArrayList()
    var customersTotal: Float = 0.0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestQueue = Volley.newRequestQueue(context)
        val sharedpreferences = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        access_token = sharedpreferences!!.getString("access_token_rm", "DEFAULT")!!
        if (access_token == "DEFAULT") {
            access_token = sharedpreferences.getString("access_token", "DEFAULT")!!
        }
        getBalance()
        getCustomer(255)








    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        pieChart = view.findViewById(R.id.pie_chart_statistics)

        val sharedpreferencesBalance = context?.getSharedPreferences("myBalance", Context.MODE_PRIVATE)
        balance = sharedpreferencesBalance!!.getString("balanceString","0.0f")!!.toFloat()

        val sharedpreferencesCustomer = context?.getSharedPreferences("myCustomers", Context.MODE_PRIVATE)
        customersTotal = sharedpreferencesCustomer!!.getInt("customer_size",0).toFloat()


        pieChart(balance, customersTotal)



        return view
    }

    private fun pieChart(balance: Float, customers: Float){
        val list: ArrayList<PieEntry> = ArrayList()

        list.add(PieEntry(10f,"quantity"))
        list.add(PieEntry(10f,"avg time"))
        list.add(PieEntry(10f,"total time"))
        list.add(PieEntry(balance,"balance €"))
        list.add(PieEntry(customers,"customers"))

        val pieDataSet = PieDataSet(list, "")
        pieDataSet.setColors(ColorTemplate.PASTEL_COLORS,250)
        pieDataSet.valueTextSize=15f
        pieDataSet.valueTextColor = Color.BLACK
        pieDataSet.valueTextSize = 20f

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData
        pieChart.centerText = "DELIVERIES"
        pieChart.description.text = ""

        pieChart.animateY(1500)
    }

    private fun getCustomer(userId: Int){
        val url = "http://10.0.2.2/api/orders/driver/$userId"
        requestQueue = Volley.newRequestQueue(context)

        val stringRequest = object : StringRequest(
            Method.GET, url,
            Response.Listener
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val data = jsonObject.getJSONArray("data")

                    for (i in 0 until data.length()) {
                        var jobject = data.getJSONObject(i)
                        customersArray.add(jobject.getString("customer_id"))
                    }

                    val sharedPreferences =
                        context!!.getSharedPreferences("myCustomers", Context.MODE_PRIVATE)
                    val mEdit1 = sharedPreferences.edit()
                    mEdit1.putInt("customer_size", customersArray.distinct().size)
                    mEdit1.commit()


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

    private fun getBalance() {
        val url = "http://10.0.2.2/api/users/profile"

        requestQueue = Volley.newRequestQueue(context)
        var balanceString: String

        val stringRequest = object : StringRequest(
            Method.GET, url,
            Response.Listener
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val data = jsonObject.getJSONObject("data")

                    balanceString= data.getString("balance")
                    val sharedPreferences =
                        context!!.getSharedPreferences("myBalance", Context.MODE_PRIVATE)
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    editor.putString("balanceString", balanceString)
                    editor.apply()

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

}