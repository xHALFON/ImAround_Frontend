
package com.example.myapplication.ui.search

import android.Manifest
import android.app.Application
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.data.repository.BluetoothRepository

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BluetoothRepository(application)

    // רשימת התקנים שנתגלו
    private val _discoveredDevices = MutableLiveData<MutableList<ScanResult>>(mutableListOf())
    val discoveredDevices: LiveData<MutableList<ScanResult>> = _discoveredDevices

    // הודעות שגיאה
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Callback לסריקה
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                val btDevice = scanResult.device
                // בדיקה האם יש הרשאת BLUETOOTH_CONNECT במכשירים עם Android 12 ומעלה
                val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                        btDevice.name ?: "אלמוני"
                    } else {
                        "אלמוני (אין הרשאת CONNECT)"
                    }
                } else {
                    btDevice.name ?: "אלמוני"
                }
                Log.d("BLE", "נמצא התקן: $deviceName - ${btDevice.address}")
                val list = _discoveredDevices.value ?: mutableListOf()
                list.add(scanResult)
                _discoveredDevices.postValue(list)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _errorMessage.postValue("הסריקה נכשלה, קוד שגיאה: $errorCode")
        }
    }


    // Callback לפרסום (אם נרצה לטפל בהודעה חוזרת)
    private val advertiseCallback = object : android.bluetooth.le.AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: android.bluetooth.le.AdvertiseSettings?) {
            Log.d("BLE", "הפרסום התחיל בהצלחה")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE", "הפרסום נכשל, קוד שגיאה: $errorCode")
        }
    }

    fun startSearch() {
        // אתחול מחדש של הרשימה (למקרה של חיפוש חוזר)
        _discoveredDevices.postValue(mutableListOf())

        repository.startAdvertising(advertiseCallback)
        repository.startScanning(scanCallback)
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopAdvertising(advertiseCallback)
        repository.stopScanning(scanCallback)
    }
}
