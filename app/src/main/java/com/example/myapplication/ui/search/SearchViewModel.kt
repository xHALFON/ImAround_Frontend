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
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.BackendApi
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.model.UserResponse
import com.example.myapplication.data.repository.BluetoothRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BluetoothRepository(application)
    private val sessionManager = SessionManager(application)
    private val backendApi = BackendApi()

    // משתמש – נקבל את ה-ID המעודכן מ־SessionManager
    private var sampleUserId = "12345678"

    // HashSet לשמירת כתובות מכשירים שנמצאו (מניע כפילויות)
    private val discoveredDeviceAddresses = HashSet<String>()
    // Set לשמירת מזהי משתמש חדשים (מה Manufacturer Data)
    private val newRemoteUserIds = mutableSetOf<String>()

    // LiveData להצגת התקנים שנתגלו (מה BLE)
    private val _discoveredDevices = MutableLiveData<MutableList<ScanResult>>(mutableListOf())
    val discoveredDevices: LiveData<MutableList<ScanResult>> = _discoveredDevices

    // LiveData להצגת רשימת המשתמשים מהבקר
    private val _usersResponse = MutableLiveData<List<UserResponse>>()
    val usersResponse: LiveData<List<UserResponse>> = _usersResponse

    // LiveData להודעות שגיאה
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Callback לסריקה – כאשר נמצא מכשיר, נחלץ את ה־ID (אם קיים)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                val btDevice = scanResult.device
                val deviceAddress = btDevice.address

                if (!discoveredDeviceAddresses.contains(deviceAddress)) {
                    discoveredDeviceAddresses.add(deviceAddress)

                    val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(
                                getApplication(),
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            btDevice.name ?: "אלמוני"
                        } else {
                            "אלמוני (אין הרשאת CONNECT)"
                        }
                    } else {
                        btDevice.name ?: "אלמוני"
                    }

                    // ניסיון לפענח את מזהה המשתמש מהנתונים (Manufacturer Data)
                    var remoteUserId = "לא זוהה ID"
                    scanResult.scanRecord?.manufacturerSpecificData?.let { msd ->
                        if (msd.size() > 0) {
                            val manufacturerId = 0xFF // אותו Manufacturer ID שהוגדר בפרסום
                            msd.get(manufacturerId)?.let { data ->
                                remoteUserId = String(data, Charsets.UTF_8)
                                Log.d("BLE", "נמצא ID: $remoteUserId")
                                if (remoteUserId != "לא זוהה ID") {
                                    newRemoteUserIds.add(remoteUserId)
                                }
                            }
                        }
                    }

                    Log.d("BLE", "נמצא התקן חדש: $deviceName - $deviceAddress - ID: $remoteUserId")
                    val list = _discoveredDevices.value ?: mutableListOf()
                    list.add(scanResult)
                    _discoveredDevices.postValue(list)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _errorMessage.postValue("הסריקה נכשלה, קוד שגיאה: $errorCode")
        }
    }

    // Callback לפרסום
    private val advertiseCallback = object : android.bluetooth.le.AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: android.bluetooth.le.AdvertiseSettings?) {
            Log.d("BLE", "הפרסום התחיל בהצלחה עם ID: $sampleUserId")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE", "הפרסום נכשל, קוד שגיאה: $errorCode")
        }
    }

    init {
        // Coroutine ששולח את רשימת המזהים כל 10 שניות אם קיימים חדשים
        viewModelScope.launch {
            while (true) {
                delay(10000L) // 10 שניות
                if (newRemoteUserIds.isNotEmpty()) {
                    val idsToSend = newRemoteUserIds.toList()
                    Log.d("BackendApi", "שליחת מזהים חדשים: $idsToSend")
                    try {
                        val response = backendApi.findUsers(idsToSend)
                        _usersResponse.postValue(response)
                    } catch (e: Exception) {
                        _errorMessage.postValue("שגיאה בקריאת backend: ${e.message}")
                    }
                    newRemoteUserIds.clear()
                }
            }
        }
    }

    fun startSearch() {
        _discoveredDevices.postValue(mutableListOf())
        discoveredDeviceAddresses.clear()

        sampleUserId = sessionManager.getUserId() ?: "123456"
        repository.startAdvertisingWithUserId(sampleUserId, advertiseCallback)
        repository.startScanning(scanCallback)
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopAdvertising(advertiseCallback)
        repository.stopScanning(scanCallback)
    }
}
