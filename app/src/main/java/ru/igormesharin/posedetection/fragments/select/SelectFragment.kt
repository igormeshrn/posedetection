package ru.igormesharin.posedetection.fragments.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.igormesharin.posedetection.databinding.FragmentSelectBinding

class SelectFragment : Fragment() {

    private lateinit var binding: FragmentSelectBinding

    private var adapter: SelectAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }

    private fun configureRecyclerView() {
        adapter = SelectAdapter()
        binding.rvChoose.adapter = adapter
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvChoose.layoutManager = layoutManager
        adapter?.addData()
    }

}