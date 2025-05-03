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
import com.example.myapplication.data.model.LikeRequest
import com.example.myapplication.data.model.MatchCheckResponse
import com.example.myapplication.data.model.MatchStatus
import com.example.myapplication.data.model.UserMatch
import com.example.myapplication.data.model.UserResponse

import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.repository.BluetoothRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BluetoothRepository(application)
    private val sessionManager = SessionManager(application)
    private val backendApi = BackendApi()

    // שימוש ב-RetrofitClient הקיים
    private val searchService = RetrofitClient.searchService
    private val matchingService = RetrofitClient.matchingService

    // ID של המשתמש הנוכחי מ-SessionManager
    private val currentUserId: String
        get() = sessionManager.getUserId() ?: "unknown"

    // HashSet לשמירת כתובות התקנים שנמצאו (מניעת כפילויות)
    private val discoveredDeviceAddresses = HashSet<String>()
    // Set לשמירת מזהי משתמש מרוחקים חדשים (מה-Manufacturer Data)
    private val newRemoteUserIds = mutableSetOf<String>()

    // LiveData להתקנים שהתגלו (מה-BLE)
    private val _discoveredDevices = MutableLiveData<MutableList<ScanResult>>(mutableListOf())
    val discoveredDevices: LiveData<MutableList<ScanResult>> = _discoveredDevices

    // LiveData למשתמשים שנמצאו בקרבת מקום
    private val _usersResponse = MutableLiveData<List<UserResponse>>()
    val usersResponse: LiveData<List<UserResponse>> = _usersResponse

    // LiveData למאצ'ים מאושרים
    private val _matches = MutableLiveData<List<UserMatch>>(emptyList())
    val matches: LiveData<List<UserMatch>> = _matches

    // LiveData למאצ'ים ממתינים (אתה לייקת אותם)
    private val _pendingMatches = MutableLiveData<List<UserMatch>>(emptyList())
    val pendingMatches: LiveData<List<UserMatch>> = _pendingMatches

    // LiveData למאצ'ים שהתקבלו (הם לייקו אותך)
    private val _receivedMatches = MutableLiveData<List<UserMatch>>(emptyList())
    val receivedMatches: LiveData<List<UserMatch>> = _receivedMatches

    // דגל להתראת מאצ' חדש
    private val _hasNewMatch = MutableLiveData<Boolean>(false)
    val hasNewMatch: LiveData<Boolean> = _hasNewMatch

    // משתמש שהפעיל את המאצ' החדש
    private val _newMatchUser = MutableLiveData<UserResponse?>(null)
    val newMatchUser: LiveData<UserResponse?> = _newMatchUser

    // LiveData להודעות שגיאה
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Callback לסריקה - כאשר נמצא מכשיר, נחלץ את ה-ID (אם קיים)
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
                            btDevice.name ?: "לא ידוע"
                        } else {
                            "לא ידוע (אין הרשאת CONNECT)"
                        }
                    } else {
                        btDevice.name ?: "לא ידוע"
                    }

                    // ניסיון לפענח את מזהה המשתמש מהנתונים (Manufacturer Data)
                    var remoteUserId = "מזהה לא ידוע"
                    scanResult.scanRecord?.manufacturerSpecificData?.let { msd ->
                        if (msd.size() > 0) {
                            val manufacturerId = 0xFF // אותו Manufacturer ID שהוגדר בפרסום
                            msd.get(manufacturerId)?.let { data ->
                                remoteUserId = String(data, Charsets.UTF_8)
                                Log.d("BLE", "נמצא מזהה: $remoteUserId")
                                if (remoteUserId != "מזהה לא ידוע") {
                                    newRemoteUserIds.add(remoteUserId)
                                }
                            }
                        }
                    }

                    Log.d("BLE", "נמצא מכשיר חדש: $deviceName - $deviceAddress - ID: $remoteUserId")
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
            Log.d("BLE", "הפרסום התחיל בהצלחה עם מזהה: $currentUserId")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE", "הפרסום נכשל, קוד שגיאה: $errorCode")
        }
    }

    init {
        // Coroutine ששולח את רשימת המזהים כל 10 שניות אם ישנם חדשים
        // ובודק מאצ'ים
        viewModelScope.launch {
            while (true) {
                delay(10000L) // 10 שניות
                if (newRemoteUserIds.isNotEmpty()) {
                    val idsToSend = newRemoteUserIds.toList()
                    Log.d("BackendApi", "שליחת מזהים חדשים: $idsToSend")
                    try {
                        val request = com.example.myapplication.data.model.FindUsersRequest(idsToSend)
                        val response = searchService.findUsers(request)
                        _usersResponse.postValue(response)
                    } catch (e: Exception) {
                        _errorMessage.postValue("שגיאה בקריאת backend: ${e.message}")
                    }
                    newRemoteUserIds.clear()
                }

                // בדיקת מאצ'ים חדשים כל 10 שניות
                checkForMatches()
            }
        }

        // טעינת נתוני מאצ' התחלתיים
        loadMatches()
    }

    // פונקציה ציבורית לטעינת המאצ'ים (נקראת מהאקטיביטי בהתחברות)
    fun loadMatches() {
        viewModelScope.launch {
            try {
                checkForMatches()
                Log.d("SearchViewModel", "מאצ'ים נטענו בהצלחה")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "שגיאה בטעינת נתוני מאצ' התחלתיים", e)
                _errorMessage.postValue("טעינת מאצ'ים נכשלה: ${e.message}")
            }
        }
    }

    // פולינג לשרת עבור מאצ'ים
    private suspend fun checkForMatches() {
        try {
            val userId = currentUserId
            val response = matchingService.checkMatches(userId)
             // עיבוד המאצ'ים
            Log.d("SearchViewModel", "מעקב מזהה: $userId")
            processMatchResponse(response)
        } catch (e: Exception) {
            Log.e("SearchViewModel", "שגיאה בבדיקת מאצ'ים", e)
        }
    }

    // עיבוד תשובת מאצ' ועדכון ממשק המשתמש בהתאם
    private fun processMatchResponse(response: MatchCheckResponse) {
        val userId = currentUserId

        // יצירת רשימות לסטטוסים שונים של מאצ'
        val confirmedMatches = mutableListOf<UserMatch>()
        val pendingMatches = mutableListOf<UserMatch>()
        val receivedMatches = mutableListOf<UserMatch>()

        // מעקב אחר מאצ'ים חדשים שלא נראו
        var hasNewUnseen = false
        var newMatchUserData: UserResponse? = null

        // עיבוד כל מאצ'
        response.matchs.forEach { match ->
            // חיפוש המשתמש האחר במשתתפים
            val otherUserId = match.participants.find { it != userId } ?: return@forEach

            // חיפוש נתוני משתמש מרשימת המשתמשים שהתגלו
            val userData = _usersResponse.value?.find { it._id == otherUserId }
                ?: return@forEach // דילוג אם אין לנו נתוני משתמש

            // בדיקת סטטוס מאצ'
            val userHasLiked = match.liked.contains(userId)
            val otherUserHasLiked = match.liked.contains(otherUserId)

            val matchObj = when {
                userHasLiked && otherUserHasLiked -> {
                    // זהו מאצ' מאושר
                    val userMatch = UserMatch(match._id, userData, MatchStatus.CONFIRMED)

                    // בדיקה אם זהו מאצ' חדש שלא נראה
                    if (!match.seen) {
                        hasNewUnseen = true
                        newMatchUserData = userData
                    }

                    userMatch
                }
                userHasLiked -> {
                    // אתה לייקת אותם, מחכה לתגובה שלהם
                    UserMatch(match._id, userData, MatchStatus.PENDING)
                }
                otherUserHasLiked -> {
                    // הם לייקו אותך, מחכים לתגובה שלך
                    UserMatch(match._id, userData, MatchStatus.RECEIVED)
                }
                else -> null // לא אמור לקרות
            }

            matchObj?.let {
                when (it.status) {
                    MatchStatus.CONFIRMED -> confirmedMatches.add(it)
                    MatchStatus.PENDING -> pendingMatches.add(it)
                    MatchStatus.RECEIVED -> receivedMatches.add(it)
                }
            }
        }

        // עדכון ה-LiveData
        _matches.postValue(confirmedMatches)
        _pendingMatches.postValue(pendingMatches)
        _receivedMatches.postValue(receivedMatches)

        // הצגת התראת מאצ' רק עבור מאצ'ים שלא נראו
        if (hasNewUnseen && newMatchUserData != null) {
            _newMatchUser.postValue(newMatchUserData)
            _hasNewMatch.postValue(true)
        }
    }

    // פונקציה ללייק משתמש - נקראת בעת החלקה ימינה על כרטיס משתמש
    fun likeUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                val request = LikeRequest(currentUserId, targetUserId)
                val response = matchingService.likeUser(request)

                // הוספה למאצ'ים ממתינים אם עדיין אין מאצ'
                if (!response.isMatch) {
                    val userData = _usersResponse.value?.find { it._id == targetUserId } ?: return@launch

                    val currentPendingMatches = _pendingMatches.value?.toMutableList() ?: mutableListOf()
                    // הוספה רק אם לא כבר ברשימה
                    if (currentPendingMatches.none { it.user._id == targetUserId }) {
                        val newPendingMatch = UserMatch(
                            id = "pending_${System.currentTimeMillis()}",
                            user = userData,
                            status = MatchStatus.PENDING
                        )
                        currentPendingMatches.add(newPendingMatch)
                        _pendingMatches.postValue(currentPendingMatches)
                    }
                } else {
                    // טיפול בתשובת מאצ' מיידית (מקרה של משתמש 2)
                    response.matchDetails?.let { matchDetails ->
                        // חיפוש המשתמש האחר במשתתפים
                        val otherUserId = matchDetails.participants.find { it != currentUserId } ?: return@let

                        // חיפוש נתוני משתמש מרשימת המשתמשים שהתגלו
                        val userData = _usersResponse.value?.find { it._id == otherUserId }
                            ?: return@let // דילוג אם אין לנו נתוני משתמש

                        // יצירת אובייקט מאצ'
                        val newMatch = UserMatch(
                            id = matchDetails._id,
                            user = userData,
                            status = MatchStatus.CONFIRMED
                        )

                        // הוספה למאצ'ים מאושרים
                        val currentConfirmedMatches = _matches.value?.toMutableList() ?: mutableListOf()
                        currentConfirmedMatches.add(newMatch)
                        _matches.postValue(currentConfirmedMatches)

                        // הסרה מרשימת הממתינים אם היה שם
                        val currentPendingMatches = _pendingMatches.value?.toMutableList() ?: mutableListOf()
                        currentPendingMatches.removeIf { it.user._id == otherUserId }
                        _pendingMatches.postValue(currentPendingMatches)

                        // הצגת התראת מאצ' מיידית
                        _newMatchUser.postValue(userData)
                        _hasNewMatch.postValue(true)
                    }
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "שגיאה בלייק משתמש", e)
                _errorMessage.postValue("שגיאה בלייק משתמש: ${e.message}")
            }
        }
    }

    // פונקציה לדיסלייק משתמש - נקראת בעת החלקה שמאלה על כרטיס משתמש
    fun dislikeUser(targetUserId: String) {
        // ביישום אמיתי, ייתכן שתרצה לשלוח זאת לשרת
        Log.d("SearchViewModel", "משתמש $currentUserId לא אהב את משתמש $targetUserId")
    }

    // פונקציה לאישור מאצ' שהתקבל
    fun approveMatch(targetUserId: String) {
        viewModelScope.launch {
            try {
                // שליחת בקשת לייק
                val request = LikeRequest(currentUserId, targetUserId)
                val response = matchingService.likeUser(request)

                // מובטח להיות מאצ' כיוון שהמשתמש האחר כבר לייק אותך
                if (response.isMatch && response.matchDetails != null) {
                    // חיפוש נתוני משתמש מרשימת המשתמשים שהתגלו
                    val userData = _usersResponse.value?.find { it._id == targetUserId }
                        ?: return@launch // דילוג אם אין לנו נתוני משתמש

                    // הוספה למאצ'ים מאושרים
                    val currentConfirmedMatches = _matches.value?.toMutableList() ?: mutableListOf()
                    currentConfirmedMatches.add(
                        UserMatch(
                            id = response.matchDetails._id,
                            user = userData,
                            status = MatchStatus.CONFIRMED
                        )
                    )
                    _matches.postValue(currentConfirmedMatches)

                    // הסרה ממאצ'ים שהתקבלו
                    val currentReceivedMatches = _receivedMatches.value?.toMutableList() ?: mutableListOf()
                    currentReceivedMatches.removeIf { it.user._id == targetUserId }
                    _receivedMatches.postValue(currentReceivedMatches)

                    // הפעלת התראת מאצ'
                    _newMatchUser.postValue(userData)
                    _hasNewMatch.postValue(true)
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "שגיאה באישור מאצ'", e)
                _errorMessage.postValue("שגיאה באישור מאצ': ${e.message}")
            }
        }
    }

    // פונקציה להסרת מאצ' עם משתמש
    fun unmatchUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                // הסרה מכל רשימות המאצ'
                val currentConfirmedMatches = _matches.value?.toMutableList() ?: mutableListOf()
                currentConfirmedMatches.removeIf { it.user._id == targetUserId }
                _matches.postValue(currentConfirmedMatches)

                val currentPendingMatches = _pendingMatches.value?.toMutableList() ?: mutableListOf()
                currentPendingMatches.removeIf { it.user._id == targetUserId }
                _pendingMatches.postValue(currentPendingMatches)

                val currentReceivedMatches = _receivedMatches.value?.toMutableList() ?: mutableListOf()
                currentReceivedMatches.removeIf { it.user._id == targetUserId }
                _receivedMatches.postValue(currentReceivedMatches)

                // בדרך כלל היינו גם קוראים ל-API להסרת המאצ' בשרת
                // ביישום עתידי
            } catch (e: Exception) {
                Log.e("SearchViewModel", "שגיאה בהסרת מאצ'", e)
                _errorMessage.postValue("שגיאה בהסרת מאצ': ${e.message}")
            }
        }
    }

    // ניקוי הדגל של מאצ' חדש כאשר המשתמש צופה במאצ'ים
    fun clearNewMatchFlag() {
        _hasNewMatch.postValue(false)
        _newMatchUser.postValue(null)
    }

    // התחלת חיפוש משתמשים בקרבת מקום
    fun startSearch() {
        _discoveredDevices.postValue(mutableListOf())
        discoveredDeviceAddresses.clear()

        repository.startAdvertisingWithUserId(currentUserId, advertiseCallback)
        repository.startScanning(scanCallback)
    }

    // הפסקת חיפוש משתמשים בקרבת מקום
    fun stopSearch() {
        repository.stopAdvertising(advertiseCallback)
        repository.stopScanning(scanCallback)
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopAdvertising(advertiseCallback)
        repository.stopScanning(scanCallback)
    }
}