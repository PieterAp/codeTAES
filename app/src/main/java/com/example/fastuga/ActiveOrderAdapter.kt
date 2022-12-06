package com.example.fastuga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ActiveOrderAdapter(private val ordersData: Array<OrderModel>) :
    RecyclerView.Adapter<ActiveOrderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val listItem =
            layoutInflater.inflate(R.layout.orders_item, parent, false)
        return ViewHolder(listItem)
    }

    override fun getItemCount(): Int {
        return ordersData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val days: Int

        //region date format
        var stuff = ordersData[position].orderTime!!.replace(" UTC", "")
        stuff = stuff.replace("T", " ")
        stuff = stuff.replace(".000000Z", "")

        val sdf = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.getDefault())
        var diff: Long = 0
        try {
            val currentTime = Calendar.getInstance().time
            diff = currentTime.time - sdf.parse(stuff)!!.time
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        days = (hours.toInt() / 24)

        if (minutes in 1..60) {
            holder.orderTime.text = "$minutes minutes ago"
        } else if (hours in 1..24) {
            holder.orderTime.text = "$hours hours ago"
        } else if (days >= 1) {
            holder.orderTime.text = "$days days ago"
        } else if (seconds >= 1) {
            holder.orderTime.text = "$seconds seconds ago"
        } else {
            holder.orderTime.text = "just now"
        }
        //endregion

        //region data format
        if (ordersData[position].customerName != "null") {
            holder.customerName.text = ordersData[position].customerName
        } else {
            holder.customerName.text = ""
        }
        holder.pickupAddress.text = ordersData[position].pickup_address
        holder.deliveryAddress.text = ordersData[position].delivery_address
        holder.ticketNumber.text = "Order #" + ordersData[position].ticketNumber
        holder.profit.text = "" + ordersData[position].profit
        holder.distance.text =
            ((ordersData[position].distance * 10.0).roundToInt() / 10.0).toString() + " km"
        //endregion

        //region profit calculation
        when (ordersData[position].distance) {
            in 3.1..10.0 -> {
                holder.profit.text = "3"
            }
            in 0.0..3.0 -> {
                holder.profit.text = "2"
            }
            else -> {
                holder.profit.text = "4"
            }
        }
        //endregion

        //region item click
        holder.cardView.setOnClickListener { view ->
            val bundle = Bundle()
            bundle.putInt("orderID", ordersData[position].orderId)
            val activity = view.context as AppCompatActivity
            val myFragment: Fragment = ActiveOrderDetailsFragment()
            myFragment.arguments = bundle
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, myFragment).addToBackStack(null).commit()
        }
        //endregion
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var orderTime: TextView
        var customerName: TextView
        var ticketNumber: TextView
        val pickupAddress: TextView
        val deliveryAddress: TextView
        var profit: TextView
        var distance: TextView
        var cardView: CardView

        init {
            orderTime = itemView.findViewById(R.id.tvOrderTime)
            customerName = itemView.findViewById(R.id.tvCustumerName)
            ticketNumber = itemView.findViewById(R.id.tvTicketNumber)
            pickupAddress = itemView.findViewById(R.id.tvPickupAddress)
            deliveryAddress = itemView.findViewById(R.id.tvDeliveryAddress)
            profit = itemView.findViewById(R.id.tvProfit)
            distance = itemView.findViewById(R.id.tvDistance)
            cardView = itemView.findViewById(R.id.cardViewOrder_item)
        }
    }
}