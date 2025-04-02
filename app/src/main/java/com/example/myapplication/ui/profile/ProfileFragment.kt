package com.example.myapplication.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.myapplication.R


class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        viewModel.loadUserProfile()

        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            view.findViewById<TextView>(R.id.tvFullName).text = "${user.firstName} ${user.lastName}"
            view.findViewById<TextView>(R.id.tvEmail).text = user.email
            view.findViewById<TextView>(R.id.tvBirthDate).text = user.birthDate

            Glide.with(this)
                .load(user.avatar)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .into(view.findViewById(R.id.imgAvatar))
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }
}

