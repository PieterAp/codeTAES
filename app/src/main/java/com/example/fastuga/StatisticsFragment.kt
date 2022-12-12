package com.example.fastuga

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import org.json.JSONException
import org.json.JSONObject


class StatisticsFragment : Fragment() {
    private lateinit var pieChart: PieChart
    private var balance: String = "0"
    private lateinit var pullToRefresh: SwipeRefreshLayout
    private lateinit var loadStatistics: TextView

    private lateinit var requestQueue: RequestQueue
    lateinit var access_token: String


    private var balanceTag: String = "balanceTag"
    private var customerTag: String = "customerTag"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedpreferences = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        access_token = sharedpreferences!!.getString("access_token_rm", "DEFAULT")!!
        if (access_token == "DEFAULT") {
            access_token = sharedpreferences.getString("access_token", "DEFAULT")!!
        }

        getBalance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)
        this.pullToRefresh = view.findViewById<View>(R.id.refreshStatistics) as SwipeRefreshLayout
        loadStatistics = view.findViewById(R.id.load_statistics)

        pieChart = view.findViewById(R.id.pie_chart_statistics)

        /*
        loadStatistics.visibility = View.GONE
        pieChart.visibility = View.VISIBLE

         */

        pullToRefresh.setOnRefreshListener {
            loadStatistics.visibility = View.VISIBLE

            getBalance()
            pullToRefresh.isRefreshing = false

        }


        return view
    }

    private fun pieChart(
        balance: Float,
        customers: Float,
        deliveries: Float,
        totalTime: Float,
        avg: Float
    ) {
        val list: ArrayList<PieEntry> = ArrayList()

        list.add(PieEntry(deliveries, "quantity"))
        list.add(PieEntry(avg, "avg time (min)"))
        list.add(PieEntry(totalTime, "total time (min)"))
        list.add(PieEntry(balance, "earned €"))
        list.add(PieEntry(customers, "customers"))

        val mColors = ArrayList<Int>()

        ColorTemplate.MATERIAL_COLORS.forEach {
            mColors.add(it)
        }

        ColorTemplate.VORDIPLOM_COLORS.forEach {
            mColors.add(it)
        }


        val pieDataSet = PieDataSet(list, "")
        pieDataSet.colors = mColors
        pieDataSet.valueTextSize = 15f
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
        pieChart.isClickable = true


        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight) {
                val i = h.x.toInt()
                val t = list[i].label

                val builder = AlertDialog.Builder(context)
                //builder.setTitle("Androidly Alert")
                builder.setMessage(" $t ")
                builder.setPositiveButton(
                    "OK",
                    DialogInterface.OnClickListener { dialog, id -> dialog.dismiss() })
                builder.show()

            }

            override fun onNothingSelected() {}
        })

    }

    private fun getBalance() {
        val url = "http://10.0.2.2/api/users/profile"

        requestQueue = Volley.newRequestQueue(context)
        //var balanceString: String

        val stringRequest = object : StringRequest(
            Method.GET, url,
            Response.Listener
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val data = jsonObject.getJSONObject("data")

                    balance = data.getString("balance")
                    getCustomer()

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

        stringRequest.tag = balanceTag
        stringRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(stringRequest)
    }

    private fun getCustomer() {
        val url = "http://10.0.2.2/api/orders/driver"
        requestQueue = Volley.newRequestQueue(context)
        var deliveryTime = 0
        var deliveryTimeArray: ArrayList<Int> = ArrayList()
        var customersArray: ArrayList<String> = ArrayList()
        var quantity: Int


        val stringRequest = object : StringRequest(
            Method.GET, url,
            Response.Listener
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val data = jsonObject.getJSONArray("data")
                    var avg = 0.0f

                    for (i in 0 until data.length()) {
                        var jobject = data.getJSONObject(i)
                        customersArray.add(jobject.getString("customer_id"))
                        deliveryTimeArray.add(jobject.getInt("delivery_time"))
                    }

                    quantity = customersArray.size

                    deliveryTimeArray.forEach {
                        deliveryTime += it
                    }

                    if (quantity != 0) {
                        avg = deliveryTime.toFloat() / quantity.toFloat()
                    }
                    loadStatistics.visibility = View.GONE
                    //pieChart.visibility = View.VISIBLE

                    pieChart(
                        balance.toFloat(),
                        customersArray.distinct().size.toFloat(),
                        quantity.toFloat(),
                        deliveryTime.toFloat(),
                        avg
                    )

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

        stringRequest.tag = customerTag
        stringRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(stringRequest)
    }

    override fun onDetach() {
        super.onDetach()
        requestQueue.cancelAll(balanceTag)
        requestQueue.cancelAll(customerTag)
    }

}