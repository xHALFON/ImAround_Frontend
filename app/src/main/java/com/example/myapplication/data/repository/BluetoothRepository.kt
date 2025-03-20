package com.example.myapplication.data.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat

class BluetoothRepository(private val context: Context) {

    private val SERVICE_UUID = "0000180D-0000-1000-8000-00805f9b34fb"
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter.bluetoothLeAdvertiser
    private val scanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner

    fun startAdvertisingWithUserId(userId: String, advertiseCallback: AdvertiseCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "אין הרשאת BLUETOOTH_ADVERTISE")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        // מקצר את מזהה המשתמש ל-8 תווים כדי להתאים למגבלת גודל הפרסום
        val shortenedUserId = userId.take(8)
        val userIdBytes = shortenedUserId.toByteArray(Charsets.UTF_8)

        // יצירת AdvertiseData – שימו לב למגבלת 31 בתים
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
            // מוסיפים את מזהה המשתמש עם Manufacturer ID 0xFF
            .addManufacturerData(0xFF, userIdBytes)
            .build()

        Log.d("BLE", "מנסה להפעיל פרסום עם ID: $shortenedUserId (${userIdBytes.size} בתים)")

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            Log.d("BLE", "פתיחת Advertising עם מזהה משתמש: $shortenedUserId")
        } catch (e: Exception) {
            Log.e("BLE", "שגיאה בפתיחת פרסום: ${e.message}")
        }
    }

    fun startScanning(scanCallback: ScanCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "אין הרשאת BLUETOOTH_SCAN")
            return
        }

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(filters, settings, scanCallback)
            Log.d("BLE", "תחילת סריקה")
        } catch (e: Exception) {
            Log.e("BLE", "שגיאה בפתיחת סריקה: ${e.message}")
        }
    }

    fun stopAdvertising(advertiseCallback: AdvertiseCallback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e("BLE", "אין הרשאת BLUETOOTH_ADVERTISE לעצירת פרסום")
                return
            }
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: SecurityException) {
            Log.e("BLE", "SecurityException בעצירת פרסום: ${e.message}")
        } catch (e: Exception) {
            Log.e("BLE", "שגיאה בעצירת פרסום: ${e.message}")
        }
    }

    fun stopScanning(scanCallback: ScanCallback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e("BLE", "אין הרשאת BLUETOOTH_SCAN לעצירת סריקה")
                return
            }
            scanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e("BLE", "SecurityException בעצירת סריקה: ${e.message}")
        } catch (e: Exception) {
            Log.e("BLE", "שגיאה בעצירת סריקה: ${e.message}")
        }
    }
}