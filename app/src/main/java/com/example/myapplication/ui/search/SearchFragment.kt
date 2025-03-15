package com.example.myapplication.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R

class SearchFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1001
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)

        val searchButton: Button = view.findViewById(R.id.searchButton)
        val textResults: TextView = view.findViewById(R.id.textResults)

        // לחיצה על כפתור – מתחילים חיפוש
        searchButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                viewModel.startSearch()
            }
        }

        // מאזינים לשינויים בתוצאות הסריקה
        viewModel.discoveredDevices.observe(viewLifecycleOwner, Observer { devices ->
            if (devices.isNotEmpty()) {
                textResults.text = devices.joinToString(separator = "\n") { result ->
                    val btDevice = result.device
                    val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            btDevice.name ?: "אלמוני"
                        } else {
                            "אלמוני (אין הרשאת CONNECT)"
                        }
                    } else {
                        btDevice.name ?: "אלמוני"
                    }
                    "נמצא: $deviceName (${btDevice.address})"
                }
            } else {
                textResults.text = "לא נמצאו משתמשים"
            }
        })
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
            false
        } else {
            true
        }
    }

    // ניתן לטפל בתוצאות בקשת ההרשאות ב-Activity או להעביר ל-Fragment
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                viewModel.startSearch()
            } else {
                Toast.makeText(requireContext(), "הרשאות נדרשות לפעולת Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
