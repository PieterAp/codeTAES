package com.example.fastuga

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

class CompleteOrderFragment : Fragment() {

    private lateinit var orderProfit: TextView
    private var profit: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profit = arguments!!.getDouble("profit")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_complete_order, container, false)

        val image = view.findViewById(R.id.imgSuccess) as ImageView

        image.animate().
        scaleX(2.0f).
        scaleY(2.0f).
        setDuration(2000).start()

        orderProfit = view.findViewById(R.id.txtOrderProfit) as TextView
        orderProfit.text = "Profit: $profit€"
        orderProfit.setTextColor(Color.parseColor("#4BB543"))

        val searchOrders = view.findViewById<Button>(R.id.btnSearchOrders)

        searchOrders.setOnClickListener {
            val intent = Intent(context, DashBoard::class.java)
            startActivity(intent)
        }



        return view
    }

}