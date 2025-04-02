package com.example.myapplication.ui.mainscreen

import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R

class MainScreenFragment : Fragment() {

    private lateinit var btnSearchUsers: Button
    private lateinit var btnViewProfile: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_main_screen, container, false)
        btnSearchUsers = view.findViewById(R.id.btnSearchUsers)
        btnViewProfile = view.findViewById(R.id.btnViewProfile)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnSearchUsers.setOnClickListener {
            findNavController().navigate(R.id.action_mainScreenFragment_to_searchFragment)
        }

        btnViewProfile.setOnClickListener {
            findNavController().navigate(R.id.action_mainScreenFragment_to_profileFragment)
        }
    }
}
