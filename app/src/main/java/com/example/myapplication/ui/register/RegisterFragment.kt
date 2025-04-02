package com.example.myapplication.ui.register

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import java.util.*

class RegisterFragment : Fragment() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etDateOfBirth: EditText
    private lateinit var btnUploadImage: Button
    private lateinit var btnRegister: Button

    private lateinit var viewModel: RegisterViewModel
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        etDateOfBirth = view.findViewById(R.id.etDateOfBirth)
        btnUploadImage = view.findViewById(R.id.btnUploadImage)
        btnRegister = view.findViewById(R.id.btnRegister)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        etDateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, y, m, d ->
                etDateOfBirth.setText("$d/${m + 1}/$y")
            }, year, month, day).show()
        }

        btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        btnRegister.setOnClickListener {
            registerUser()
        }

        viewModel.authResponse.observe(viewLifecycleOwner, Observer { response ->
            Toast.makeText(requireContext(), "Registered successfully!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_registerFragment_to_welcomeFragment)
        })

        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            Toast.makeText(requireContext(), "Image selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerUser() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val dob = etDateOfBirth.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() ||
            email.isEmpty() || password.isEmpty() || dob.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.registerUser(firstName, lastName, email, password, dob, selectedImageUri)
    }
}
