package com.example.fastuga

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.volley.RequestQueue

class StatisticsFragment : Fragment() {

    private lateinit var requestQueue: RequestQueue
    private var statisticsTag: String = "statisticsTag"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }


    override fun onDetach() {
        super.onDetach()
        //requestQueue.cancelAll(statisticsTag)
    }

}