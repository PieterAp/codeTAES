package com.example.fastuga

import android.R.attr
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


private lateinit var tvODCustomerName: TextView
private lateinit var tvODOrderTime: TextView
private lateinit var tvODPickupAddress: TextView
private lateinit var tvODDeliveryAddress: TextView
private lateinit var tvODProfit: TextView
private lateinit var tvODDistance: TextView
private lateinit var tvODTimeLeft: TextView
private lateinit var map: MapView
private lateinit var acceptOrderBtn: Button
private const val TAG = "OsmActivity"

class OrderDetailsFragment : Fragment() {
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getOrder(arguments!!.getInt("orderID"))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val rootView: View = inflater.inflate(R.layout.fragment_order_details, container, false)
        tvODCustomerName = rootView.findViewById<View>(R.id.tvODCustomerName) as TextView
        tvODOrderTime = rootView.findViewById<View>(R.id.tvODOrderTime) as TextView
        tvODPickupAddress = rootView.findViewById<View>(R.id.tvODPickupAddress) as TextView
        tvODDeliveryAddress = rootView.findViewById<View>(R.id.tvODDeliveryAddress) as TextView
        tvODProfit = rootView.findViewById<View>(R.id.tvODProfit) as TextView
        tvODDistance = rootView.findViewById<View>(R.id.tvODDistance) as TextView
        tvODTimeLeft = rootView.findViewById<View>(R.id.tvODTimeLeft) as TextView
        map = rootView.findViewById(R.id.map)
        acceptOrderBtn = rootView.findViewById(R.id.btnODAcceptOrder)

        acceptOrderBtn.setOnClickListener(View.OnClickListener {
            activeOrder(arguments!!.getInt("orderID"))
        })

        return rootView
    }

    private fun getOrder(orderID: Int) {
        val url = "http://10.0.2.2/api/orders/$orderID"
        var days = 0

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
                //region data map to textview
                val ordersJson = JSONObject(response)
                val order = ordersJson.getJSONObject("data")
                tvODCustomerName.text = order.getString("customer_name")

                //region time calculation
                var stuff = order.getString("created_at").replace(" UTC", "")
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
                    tvODOrderTime.text = "$minutes minutes ago"
                } else if (hours in 1..24) {
                    tvODOrderTime.text = "$hours hours ago"
                } else if (days >= 1) {
                    tvODOrderTime.text = "$days days ago"
                } else if (seconds >= 1) {
                    tvODOrderTime.text = "$seconds seconds ago"
                } else {
                    tvODOrderTime.text = "just now"
                }
                //endregion
                tvODPickupAddress.text = order.getString("pickup_address")
                tvODDeliveryAddress.text = order.getString("delivery_address")

                //region route map load
                loadMap(
                    context,
                    order.getString("pickup_address"),
                    order.getString("delivery_address")
                )
                //endregion

                //region profit calculation
                if (order.getString("delivery_distance") == "null") {
                    tvODProfit.text = "can't be calculated"
                } else {
                    when (order.getString("delivery_distance").toDouble()) {
                        in 3.1..10.0 -> {
                            tvODProfit.text = "3"
                        }
                        in 0.0..3.0 -> {
                            tvODProfit.text = "2"
                        }
                        else -> {
                            tvODProfit.text = "4"
                        }
                    }
                }

                //endregion

                //endregion
            }, Response.ErrorListener { error ->
                error.networkResponse
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

    private fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return if (context!!.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                context!!.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.v(TAG, "Permission is granted");
                true;
            } else {
                Log.v(TAG, "Permission is revoked");
                this.requestPermissions(
                    arrayOf(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    1
                );
                false;
            }
        } else {
            Log.v(TAG, "Permission is granted");
            return true;
        }

    }

    private fun getLocationFromAddress(context: Context?, strAddress: String?): LatLng? {
        val coder = Geocoder(context!!)
        val address: List<Address>?
        var p1: LatLng? = null
        try {
            address = coder.getFromLocationName(strAddress!!, 5)
            if (address == null) {
                return null
            }
            val location: Address = address[0]
            location.latitude
            location.longitude
            p1 = LatLng(location.latitude, location.longitude)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return p1
    }

    private fun loadMap(context: Context?, pickupAddress: String, deliveryAddress: String) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().userAgentValue = "MyOwnUserAgent/1.0";
        val roadManager: RoadManager = OSRMRoadManager(context, "MyOwnUserAgent/1.0")
        if (Build.VERSION.SDK_INT >= 23) {
            isStoragePermissionGranted()
        }

        map.setMultiTouchControls(false)
        val waypoints = ArrayList<GeoPoint>()
        //endregion

        //region addresses config
        //CONVERTS ADDRESS TO COORDINATES
        val startLatLng = getLocationFromAddress(
            context,
            pickupAddress
        )
        val endLatLng = getLocationFromAddress(
            context,
            deliveryAddress
        )

        val startAddressLat = startLatLng!!.latitude
        val startAddressLng = startLatLng.longitude

        val endAddressLat = endLatLng!!.latitude
        val endAddressLng = endLatLng.longitude

        //endregion

        //region USED TO CREATE THE ROUTE (ROUTE USES WAYPOINTS AND NOT MARKERS)
        val startPoint = GeoPoint(
            startAddressLat,
            startAddressLng
        ) //find an accurate way to get lat lng from address
        val endPoint = GeoPoint(endAddressLat, endAddressLng)
        waypoints.add(startPoint)
        waypoints.add(endPoint)
        //endregion

        //region CREATE START MARKER
        val startMarker = Marker(map)
        startMarker.position = startPoint
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = pickupAddress
        startMarker.icon = context!!.getDrawable(R.drawable.location_pin)
        map.overlays.add(startMarker)
        //endregion

        //region CREATE DESTINATION MARKER
        val destinationMarker = Marker(map)
        destinationMarker.position = endPoint
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        destinationMarker.title = deliveryAddress
        destinationMarker.icon = context.getDrawable(R.drawable.racing_flag)
        map.overlays.add(destinationMarker)
        //endregion

        //region MAKES THE ROUTE TRACE
        val road: Road = roadManager.getRoad(waypoints)
        tvODTimeLeft.text = (road.mDuration / 60).toInt().toString() + " min"

        if (road.mLength != 0.0) {
            map.setScrollableAreaLimitDouble(road.mBoundingBox)
            map.zoomToBoundingBox(road.mBoundingBox, true, 275)
            map.controller.zoomOut()
        } else {
            val center = GeoPoint(startPoint.latitude, startPoint.longitude)
            map.controller.animateTo(center)
            map.controller.setZoom(21.0)
        }

        tvODDistance.text = ((road.mLength * 10.0).roundToInt() / 10.0).toString() + " km"

        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.add(roadOverlay)
        //endregion
    }

    private fun activeOrder(orderID: Int) {
        val url = "http://10.0.2.2/api/orders/$orderID"
        requestQueue = Volley.newRequestQueue(context)
        var accessToken: String

        val obj = JSONObject()
        obj.put("status", "D")
        obj.put("delivered_by", orderID)

        accessToken = this.activity!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("access_token_rm", "DEFAULT")!!
        if (accessToken == "DEFAULT") {
            accessToken = this.activity!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                .getString("access_token", "DEFAULT")!!
        }

        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.PUT, url,
            obj, Response.Listener {
                //TODO IMPLEMENT NOTIFICATION
                Toast.makeText(context, "Order accepted for delivery", Toast.LENGTH_SHORT).show();
            }, Response.ErrorListener { error ->
                //TODO ERROR TREATMENT
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

}