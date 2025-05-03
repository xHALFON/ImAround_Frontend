package com.example.myapplication.ui.hobbies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.network.RetrofitClient
import kotlinx.coroutines.launch

/**
 * ViewModel for handling hobby selection
 * Usable in both registration flow and settings
 */
class HobbyViewModel(application: Application) : AndroidViewModel(application) {

    // Selected hobbies
    private val _selectedHobbies = MutableLiveData<List<String>>(emptyList())
    val selectedHobbies: LiveData<List<String>> = _selectedHobbies

    // Loading and error states
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Initialize with existing hobbies - for example when editing in settings
     */
    fun initializeWithExistingHobbies(hobbies: List<String>) {
        _selectedHobbies.value = hobbies
    }

    /**
     * Toggle hobby selection (add or remove)
     */
    fun toggleHobbySelection(hobby: String) {
        val currentSelection = _selectedHobbies.value ?: emptyList()

        if (currentSelection.contains(hobby)) {
            // Remove the hobby if already selected
            _selectedHobbies.value = currentSelection.filter { it != hobby }
        } else if (currentSelection.size < 5) {
            // Add the hobby if not already selected and less than 5 are selected
            _selectedHobbies.value = currentSelection + hobby
        }
    }

    /**
     * Save hobbies to user profile
     * This would be used when updating hobbies from settings
     */
    fun saveHobbies() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Example API call - implement according to your backend
                // RetrofitClient.userService.updateHobbies(_selectedHobbies.value ?: emptyList())

                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error updating hobbies"
                _isLoading.value = false
            }
        }
    }

    /**
     * Get currently selected hobbies
     * This can be used by other ViewModels that need the hobby data
     */
    fun getSelectedHobbies(): List<String> {
        return _selectedHobbies.value ?: emptyList()
    }
}