package com.example.fastuga

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

private lateinit var map: MapView
private const val TAG = "OsmActivity"
private lateinit var tvAOPickupAddress: TextView
private lateinit var tvAODeliveryAddress: TextView
private lateinit var buttonCancelActive: Button
private lateinit var openNav: FloatingActionButton
private var activeOrdersDetailsTag: String = "orderTag"
private var deliveryTime: Float = 0.0f


class ActiveOrderDetailsFragment : Fragment() {

    private lateinit var requestQueue: RequestQueue
    private var profit: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getOrder(arguments!!.getInt("orderID"))

        val sharedpreferences = context?.getSharedPreferences("time", Context.MODE_PRIVATE)
        deliveryTime = sharedpreferences!!.getFloat("delivery_time",0f)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        (activity as AppCompatActivity).supportActionBar?.title = "Active Order Details"
        val rootView: View =
            inflater.inflate(R.layout.fragment_active_order_details, container, false)
        tvAOPickupAddress = rootView.findViewById<View>(R.id.tvAOPickupAddress) as TextView
        tvAODeliveryAddress = rootView.findViewById<View>(R.id.tvAODeliveryAddress) as TextView
        buttonCancelActive = rootView.findViewById<Button>(R.id.buttonCancelActive) as Button
        map = rootView.findViewById(R.id.map)

        val confirmOrderButton = rootView.findViewById(R.id.btnConfirmOrder) as AppCompatButton

        confirmOrderButton.setOnClickListener {
            confirmDeliver(arguments!!.getInt("orderID"))
        }

        openNav = rootView.findViewById(R.id.openNav)
        openNav.setOnClickListener(View.OnClickListener {
            val gmmIntentUri =
                Uri.parse("google.navigation:q=" + tvAODeliveryAddress.text)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        })

        buttonCancelActive.setOnClickListener(View.OnClickListener {
            buttonCancelActive(arguments!!.getInt("orderID"))
        })

        return rootView
    }

    private fun buttonCancelActive(orderID: Int) {
        val url = "http://10.0.2.2/api/orders/$orderID"
        requestQueue = Volley.newRequestQueue(context)
        var accessToken: String

        val obj = JSONObject()
        obj.put("status", "C")

        accessToken = this.activity!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("access_token_rm", "DEFAULT")!!
        if (accessToken == "DEFAULT") {
            accessToken = this.activity!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                .getString("access_token", "DEFAULT")!!
        }

        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.PUT, url,
            obj, Response.Listener {
                val myFragment: Fragment = OrdersFragment()
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

    private fun confirmDeliver(orderID: Int){
        val url = "http://10.0.2.2/api/orders/$orderID/confirm"
        requestQueue = Volley.newRequestQueue(context)
        var accessToken: String

        val obj = JSONObject()
        obj.put("status", "D")
        obj.put("balance", profit)
        obj.put("delivery_time", deliveryTime)

        accessToken = this.activity!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("access_token_rm", "DEFAULT")!!
        if (accessToken == "DEFAULT") {
            accessToken = this.activity!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                .getString("access_token", "DEFAULT")!!
        }

        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.PUT, url,
            obj, Response.Listener {
                //send profit to other fragment
                val bundle = Bundle()
                bundle.putDouble("profit", profit)
                val myFragment: Fragment = CompleteOrderFragment()
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
                //endregion
                tvAOPickupAddress.text = order.getString("pickup_address")
                tvAODeliveryAddress.text = order.getString("delivery_address")

                //region route map load
                if (order.getString("pickup_address") != "null" && order.getString("delivery_address") != "null") {
                    loadMap(
                        context,
                        order.getString("pickup_address"),
                        order.getString("delivery_address")
                    )
                }

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
        jsonObjectRequest.tag = activeOrdersDetailsTag
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
        //region map config
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().userAgentValue = "MyOwnUserAgent/1.0";
        val roadManager: RoadManager = OSRMRoadManager(context, "MyOwnUserAgent/1.0")
        if (Build.VERSION.SDK_INT >= 23) {
            isStoragePermissionGranted()
        }

        map.setMultiTouchControls(true)
        val waypoints = ArrayList<GeoPoint>()
        //endregion

        //region addresses config
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

        if (road.mLength != 0.0) {
            //map.setScrollableAreaLimitDouble(road.mBoundingBox)
            map.zoomToBoundingBox(road.mBoundingBox, true, 275)
        } else {
            val center = GeoPoint(startPoint.latitude, startPoint.longitude)
            map.controller.animateTo(center)
            map.controller.setZoom(21.0)
        }

        profit = when (road.mLength) {
            in 3.1..10.0 -> {
                3.0
            }
            in 0.0..3.0 -> {
                2.0
            }
            else -> {
                4.0
            }
        }

        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.add(roadOverlay)
        //endregion

        //region DIRECTION NODES
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_CAR)
        val nodeIcon = resources.getDrawable(R.drawable.marker_node, context.theme)
        for (i in road.mNodes.indices) {
            val node = road.mNodes[i]
            val nodeMarker = Marker(map)
            nodeMarker.position = node.mLocation
            nodeMarker.icon = nodeIcon
            nodeMarker.title = "Step $i"
            nodeMarker.snippet = node.mInstructions
            nodeMarker.subDescription =
                Road.getLengthDurationText(context, node.mLength, node.mManeuverType.toDouble())

            when (node.mManeuverType) {
                0 -> nodeMarker.image = resources.getDrawable(R.drawable.ic_continue, context.theme)
                2 -> nodeMarker.image = resources.getDrawable(R.drawable.ic_continue, context.theme)

                4 -> nodeMarker.image =
                    resources.getDrawable(R.drawable.ic_turn_left, context.theme)
                5 -> nodeMarker.image =
                    resources.getDrawable(R.drawable.ic_turn_left, context.theme)

                6 -> nodeMarker.image =
                    resources.getDrawable(R.drawable.ic_turn_right, context.theme)
                7 -> nodeMarker.image =
                    resources.getDrawable(R.drawable.ic_turn_right, context.theme)

                20 -> nodeMarker.image =
                    resources.getDrawable(R.drawable.ic_continue, context.theme)
                21 -> nodeMarker.image =
                    resources.getDrawable(R.drawable.ic_continue, context.theme)

                24 -> nodeMarker.image = resources.getDrawable(R.drawable.ic_arrived, context.theme)

                /* first exit */ 27 -> nodeMarker.image =
                resources.getDrawable(R.drawable.rotunda_primeira_trans_65x65, context.theme)
                /* second exit */ 28 -> nodeMarker.image =
                resources.getDrawable(R.drawable.rotunda_segunda_trans_65x65, context.theme)
                /* third exit */ 29 -> nodeMarker.image =
                resources.getDrawable(R.drawable.rotunda_terceira_trans_65x65, context.theme)
            }
            map.overlays.add(nodeMarker)
        }
        //endregion
    }

    override fun onDetach() {
        super.onDetach()
        requestQueue.cancelAll(activeOrdersDetailsTag)
    }


}