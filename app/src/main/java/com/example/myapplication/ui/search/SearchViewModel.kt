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
import com.example.myapplication.data.model.FindUsersRequest
import com.example.myapplication.data.model.LikeRequest
import com.example.myapplication.data.model.MatchCheckResponse
import com.example.myapplication.data.model.MatchStatus
import com.example.myapplication.data.model.UserMatch
import com.example.myapplication.data.model.UserResponse
import com.example.myapplication.data.network.SocketManager
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.network.SaveFCMTokenRequest
import com.example.myapplication.data.repository.BluetoothRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BluetoothRepository(application)
    private val sessionManager = SessionManager(application)
    private val backendApi = BackendApi()
    private val socketManager = SocketManager.getInstance()
    val newMatchId = MutableLiveData<String?>()
    // שימוש ב-RetrofitClient הקיים
    private val searchService = RetrofitClient.searchService
    private val matchingService = RetrofitClient.matchingService
    private val _socketConnected = MutableLiveData<Boolean>(false)
    val socketConnected: LiveData<Boolean> = _socketConnected
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
        socketManager.init("http://10.0.2.2:3000")
        setupSocketListeners()
        connectSocket()

        // Firebase initialization - הוסף את זה:
        if (currentUserId != "unknown") {
            initializeFCM()
        }

        viewModelScope.launch {
            while (true) {
                delay(10000L) // 10 שניות
                if (newRemoteUserIds.isNotEmpty()) {
                    val idsToSend = newRemoteUserIds.toList()
                    Log.d("BackendApi", "שליחת מזהים חדשים: $idsToSend")
                    try {
                        // שים לב לשינוי כאן - מעבירים גם את מזהה המשתמש הנוכחי
                        val request = FindUsersRequest(idsToSend, currentUserId)
                        val response = searchService.findUsers(request)
                        _usersResponse.postValue(response)
                    } catch (e: Exception) {
                        _errorMessage.postValue("שגיאה בקריאת backend: ${e.message}")
                    }
                    newRemoteUserIds.clear()
                }
            }
        }
        loadMatches()
    }

    // Firebase FCM initialization - הוסף את הפונקציות האלה:
    private fun initializeFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("SearchViewModel", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("SearchViewModel", "FCM Token: $token")

            // שלח את הטוקן לשרת שלך (אופציונלי לעכשיו)
            sendFCMTokenToServer(token)
        }
    }

    private fun sendFCMTokenToServer(token: String) {
        Log.d("SearchViewModel", "Sending FCM token to server: $token")

        viewModelScope.launch {
            try {
                val request = SaveFCMTokenRequest(
                    userId = currentUserId,
                    token = token
                )

                // 🔥 משתמש ב-matchingService במקום searchService
                matchingService.saveFCMToken(request)
                Log.d("SearchViewModel", "✅ FCM token sent successfully to server")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "❌ Failed to send FCM token to server", e)
                // לא נציג שגיאה למשתמש - זה לא קריטי
            }
        }
    }

    private fun setupSocketListeners() {
        // Handle new match events
        socketManager.setOnMatchListener { matchData ->
            Log.d("SocketManager", "Received match: ${matchData._id}")

            // Check if this is a match with both likes (confirmed match)
            if (matchData.liked.size >= 2) {
                // Find the other user ID (not current user)
                val otherUserId = matchData.participants.find { it != currentUserId } ?: return@setOnMatchListener
                removeUserFromRadar(otherUserId)
                // Find user data from discovered users
                val userData = _usersResponse.value?.find { it._id == otherUserId }

                if (userData != null) {
                    // This is a confirmed match - show notification REGARDLESS of who liked last
                    val newMatch = UserMatch(
                        id = matchData._id,
                        user = userData,
                        status = MatchStatus.CONFIRMED
                    )

                    // Update confirmed matches list
                    val currentMatches = _matches.value?.toMutableList() ?: mutableListOf()
                    if (currentMatches.none { it.id == matchData._id }) {
                        currentMatches.add(newMatch)
                        _matches.postValue(currentMatches)
                    }

                    // Clean up other lists
                    val pendingMatches = _pendingMatches.value?.toMutableList() ?: mutableListOf()
                    pendingMatches.removeIf { it.user._id == otherUserId }
                    _pendingMatches.postValue(pendingMatches)

                    val receivedMatches = _receivedMatches.value?.toMutableList() ?: mutableListOf()
                    receivedMatches.removeIf { it.user._id == otherUserId }
                    _receivedMatches.postValue(receivedMatches)

                    // IMPORTANT: Always show match notification for confirmed matches
                    Log.d("SocketManager", "Showing match notification for: ${userData.firstName}")
                    _newMatchUser.postValue(userData)
                    newMatchId.postValue(matchData._id)
                    _hasNewMatch.postValue(true)
                } else {

                    Log.d("SocketManager", "Fetching user data for ID: $otherUserId")

                    viewModelScope.launch {
                        try {
                            // Call the API to get user data
                            val userFromServer = RetrofitClient.authService.getUserById(otherUserId)

                            if (userFromServer != null) {
                                // Create match with the fetched user data
                                val newMatch = UserMatch(
                                    id = matchData._id,
                                    user = userFromServer,
                                    status = MatchStatus.CONFIRMED
                                )

                                // Update confirmed matches list
                                val currentMatches = _matches.value?.toMutableList() ?: mutableListOf()
                                if (currentMatches.none { it.id == matchData._id }) {
                                    currentMatches.add(newMatch)
                                    _matches.postValue(currentMatches)
                                }

                                // Clean up other lists
                                val pendingMatches = _pendingMatches.value?.toMutableList() ?: mutableListOf()
                                pendingMatches.removeIf { it.user._id == otherUserId }
                                _pendingMatches.postValue(pendingMatches)

                                val receivedMatches = _receivedMatches.value?.toMutableList() ?: mutableListOf()
                                receivedMatches.removeIf { it.user._id == otherUserId }
                                _receivedMatches.postValue(receivedMatches)

                                // Show match notification with the fetched user data
                                Log.d("SocketManager", "Showing match notification for fetched user: ${userFromServer.firstName}")
                                _newMatchUser.postValue(userFromServer)

                                _hasNewMatch.postValue(true)
                            } else {
                                Log.e("SocketManager", "User not found on server for ID: $otherUserId")
                            }
                        } catch (e: Exception) {
                            Log.e("SocketManager", "Error fetching user data: ${e.message}", e)
                            _errorMessage.postValue("Failed to load match: ${e.message}")
                        }
                    }
                }
            } else if (matchData.liked.size == 1) {
                // Handle single like case (pending or received)
                // [Original logic for pending/received matches]
            }
        }



        socketManager.setOnMatchSeenListener { matchId ->
            // Update match seen status in your lists
            val confirmedMatches = _matches.value?.toMutableList() ?: mutableListOf()
            confirmedMatches.find { it.id == matchId }?.let { match ->
                // Here you would update the match object if needed
                // For now we just log it
                Log.d("SocketManager", "Match ${match.id} was seen by the other user")
            }
        }

        // Connection status listeners
        socketManager.setOnConnectListener {
            _socketConnected.postValue(true)
        }

        socketManager.setOnDisconnectListener {
            _socketConnected.postValue(false)
        }
    }



    fun loadMatches() {
        viewModelScope.launch {
            try {

                Log.d("SearchViewModel", "מאצ'ים נטענו בהצלחה")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "שגיאה בטעינת נתוני מאצ' התחלתיים", e)
                _errorMessage.postValue("טעינת מאצ'ים נכשלה: ${e.message}")
            }
        }
    }



    private fun removeUserFromRadar(userId: String) {
        val currentUsers = _usersResponse.value?.toMutableList() ?: mutableListOf()
        currentUsers.removeIf { it._id == userId }
        _usersResponse.postValue(currentUsers)
    }
    // פונקציה ללייק משתמש - נקראת בעת החלקה ימינה על כרטיס משתמש
    fun likeUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                val request = LikeRequest(currentUserId, targetUserId)
                val response = matchingService.likeUser(request)

                // Remove user from radar once liked
                val currentUsers = _usersResponse.value?.toMutableList() ?: mutableListOf()
                currentUsers.removeIf { it._id == targetUserId }
                _usersResponse.postValue(currentUsers)

                // Handle match response
                if (!response.isMatch) {
                    // Add to pending matches if not a match
                    val userData = _usersResponse.value?.find { it._id == targetUserId }
                    userData?.let {
                        val currentPendingMatches = _pendingMatches.value?.toMutableList() ?: mutableListOf()
                        if (currentPendingMatches.none { it.user._id == targetUserId }) {
                            val newPendingMatch = UserMatch(
                                id = "pending_${System.currentTimeMillis()}",
                                user = userData,
                                status = MatchStatus.PENDING
                            )
                            currentPendingMatches.add(newPendingMatch)
                            _pendingMatches.postValue(currentPendingMatches)
                        }
                    }
                }
                // Socket will handle the case of it being a match
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error liking user", e)
                _errorMessage.postValue("Error liking user: ${e.message}")
            }
        }
    }

    fun dislikeUser(targetUserId: String) {

        removeUserFromRadar(targetUserId)

        viewModelScope.launch {
            try {
                // שליחת בקשת דיסלייק לשרת
                val request = LikeRequest(currentUserId, targetUserId)
                matchingService.dislikeUser(request)
                Log.d("SearchViewModel", "משתמש $currentUserId לא אהב את משתמש $targetUserId - נשלח לשרת")
            } catch (e: Exception) {
                // אם נכשל, נרשום לוג אבל לא נודיע למשתמש כי זה לא קריטי
                Log.e("SearchViewModel", "שגיאה בשליחת dislike לשרת", e)
            }
        }
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
                    newMatchId.postValue(response.matchDetails._id) //
                    _hasNewMatch.postValue(true)
                    removeUserFromRadar(targetUserId)
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "שגיאה באישור מאצ'", e)
                _errorMessage.postValue("שגיאה באישור מאצ': ${e.message}")
            }
        }
    }

    // פונקציה להסרת מאצ' עם משתמש
//    fun unmatchUser(targetUserId: String) {
//        viewModelScope.launch {
//            try {
//                // הסרה מכל רשימות המאצ'
//                val currentConfirmedMatches = _matches.value?.toMutableList() ?: mutableListOf()
//                currentConfirmedMatches.removeIf { it.user._id == targetUserId }
//                _matches.postValue(currentConfirmedMatches)
//
//                val currentPendingMatches = _pendingMatches.value?.toMutableList() ?: mutableListOf()
//                currentPendingMatches.removeIf { it.user._id == targetUserId }
//                _pendingMatches.postValue(currentPendingMatches)
//
//                val currentReceivedMatches = _receivedMatches.value?.toMutableList() ?: mutableListOf()
//                currentReceivedMatches.removeIf { it.user._id == targetUserId }
//                _receivedMatches.postValue(receivedMatches)
//
//                // בדרך כלל היינו גם קוראים ל-API להסרת המאצ' בשרת
//                // ביישום עתידי
//            } catch (e: Exception) {
//                Log.e("SearchViewModel", "שגיאה בהסרת מאצ'", e)
//                _errorMessage.postValue("שגיאה בהסרת מאצ': ${e.message}")
//            }
//        }
//    }

    // ניקוי הדגל של מאצ' חדש כאשר המשתמש צופה במאצ'ים
    fun clearNewMatchFlag() {
        _hasNewMatch.postValue(false)
        _newMatchUser.postValue(null)
        newMatchId.postValue(null)
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
        socketManager.disconnect()
    }
    // הוסף את הפונקציה הזו ל-SearchViewModel בסוף הקלאס:

    // Helper function to ensure socket connection
    fun ensureSocketConnection() {
        Log.d("SearchViewModel", "🔥 Ensuring socket connection for user: $currentUserId")

        if (currentUserId != "unknown" && !socketManager.isConnected()) {
            Log.d("SearchViewModel", "🔥 Socket not connected, reconnecting...")
            connectSocket()
        } else {
            Log.d("SearchViewModel", "🔥 Socket already connected or user unknown")
        }
    }

    // Function to reconnect socket when app comes back to foreground
    fun reconnectSocketIfNeeded() {
        Log.d("SearchViewModel", "🔥 SearchViewModel checking socket connection for user: $currentUserId")

        if (currentUserId != "unknown") {
            viewModelScope.launch {
                if (!socketManager.isConnected()) {
                    Log.d("SearchViewModel", "🔥 SearchViewModel reconnecting socket...")
                    connectSocket()
                }
            }
        }
    }

    // עדכן את הפונקציה connectSocket:
    private fun connectSocket() {
        Log.d("SearchViewModel", "🔥 Connecting socket for user: $currentUserId")
        socketManager.connect(currentUserId)
    }
}