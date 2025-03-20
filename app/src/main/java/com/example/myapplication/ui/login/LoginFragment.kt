package com.example.myapplication.ui.login

import AuthResponse
import LoginRequest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            loginUser()
        }

        return view
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = LoginRequest(email, password)

        RetrofitClient.authService.loginUser(request)
            .enqueue(object : Callback<AuthResponse> {
                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    if (response.isSuccessful) {
                        val userId = response.body()?.userId ?: ""
                        val token = response.body()?.token ?: ""
                        val sessionManager = SessionManager(requireContext())
                        sessionManager.saveAuthData(userId, token)
                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        Log.d("SessionCheck", "Saved userId: ${sessionManager.getUserId()}, token: ${sessionManager.getToken()}")
                        findNavController().navigate(R.id.searchFragment)
                    } else {
                        Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Log.e("LoginError", "Retrofit failed: ${t.localizedMessage}", t)
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}
