package com.example.fastuga

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
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
import com.github.mikephil.charting.components.Legend
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
    var deliveriesTotal: Float = 0.0f

    var deliveryTimeArray: ArrayList<Int> = ArrayList()
    var deliveryTime = 0
    var deliveryTimeTotal = 0.0f
    var avgTime = 0.0f


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
        deliveriesTotal = sharedpreferencesCustomer.getInt("deliveries",0).toFloat()
        deliveryTimeTotal = sharedpreferencesCustomer.getFloat("totalTime",0f)

        avgTime = deliveryTimeTotal/deliveriesTotal


        pieChart(balance, customersTotal, deliveriesTotal, deliveryTimeTotal, avgTime)



        return view
    }

    private fun pieChart(balance: Float, customers: Float, deliveries: Float, totalTime: Float, avg: Float){
        val list: ArrayList<PieEntry> = ArrayList()

        list.add(PieEntry(deliveries,"quantity"))
        list.add(PieEntry(avg,"avg time (min)"))
        list.add(PieEntry(totalTime,"total time (min)"))
        list.add(PieEntry(balance,"earned €"))
        list.add(PieEntry(customers,"customers"))

        val mColors = ArrayList<Int>()

        ColorTemplate.MATERIAL_COLORS.forEach {
            mColors.add(it)
        }

        ColorTemplate.VORDIPLOM_COLORS.forEach {
            mColors.add(it)
        }


        val pieDataSet = PieDataSet(list, "")
        pieDataSet.colors = mColors
        pieDataSet.valueTextSize=15f
        pieDataSet.valueTextColor = Color.BLACK
        pieDataSet.valueTextSize = 20f

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData
        pieChart.centerText = "DELIVERIES"
        pieChart.description.text = ""
        pieChart.description.textSize = 500f
        pieChart.setCenterTextSize(20f)

        val l = pieChart.legend
        l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        l.orientation = Legend.LegendOrientation.VERTICAL
        l.setDrawInside(false)
        l.textSize = 18f

        pieChart.animateY(1500)
        pieChart.setDrawEntryLabels(false)
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
                        deliveryTimeArray.add(jobject.getInt("delivery_time"))
                    }

                    val sharedPreferences =
                        context!!.getSharedPreferences("myCustomers", Context.MODE_PRIVATE)
                    val mEdit1 = sharedPreferences.edit()
                    mEdit1.putInt("customer_size", customersArray.distinct().size)
                    mEdit1.commit()

                    val deliveries = sharedPreferences.edit()
                    deliveries.putInt("deliveries", customersArray.size)
                    deliveries.commit()

                    deliveryTimeArray.forEach {
                        deliveryTime += it
                    }

                    val time = sharedPreferences.edit()
                    time.putFloat("totalTime", deliveryTime.toFloat())
                    time.commit()

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