package ru.igormesharin.posedetection.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ru.igormesharin.posedetection.R
import ru.igormesharin.posedetection.databinding.FragmentSelectBinding

class SelectFragment : Fragment() {

    private lateinit var binding: FragmentSelectBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureButtons()
    }

    private fun configureButtons() {
        binding.btnRealtimePoseDetection.setOnClickListener {
            findNavController().navigate(R.id.fragment_select_to_fragment_realtime_pose_detection)
        }
        binding.btnPoseDetection.setOnClickListener {
            findNavController().navigate(R.id.fragment_select_to_fragment_pose_detection)
        }
    }

}