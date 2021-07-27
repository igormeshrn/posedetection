package ru.igormesharin.posedetection.fragments.select

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import ru.igormesharin.posedetection.R
import ru.igormesharin.posedetection.databinding.LiChooseBinding

class SelectAdapter : RecyclerView.Adapter<SelectAdapter.SelectViewHolder>() {

    private val items: MutableList<SelectItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiChooseBinding.inflate(inflater, parent, false)
        return SelectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun addData() {
        items.add(SelectItem("Realtime pose detection", "Detect poses in realtime with ML Kit"))
        items.add(SelectItem("Pose detection", "Do something on specific pose"))
        items.add(SelectItem("Face detection", "Detect face position in realtime with ML Kit"))
        items.add(SelectItem("Realtime pose detection accurate", "High accuracy but low FPS"))
        items.add(SelectItem("Face recognition", "description..."))
        items.add(SelectItem("Pose detection with repeat", "Repeat counter for exercises"))
    }

    inner class SelectViewHolder(var binding: LiChooseBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelectItem) {
            binding.title.text = item.title
            binding.description.text = item.description

            binding.root.setOnClickListener {
                when (item.title) {
                    "Realtime pose detection" -> it.findNavController().navigate(R.id.fragment_select_to_fragment_realtime_pose_detection)
                    "Pose detection" -> it.findNavController().navigate(R.id.fragment_select_to_fragment_pose_detection)
                    "Face detection" -> it.findNavController().navigate(R.id.fragment_face_detection)
                    "Realtime pose detection accurate" -> it.findNavController().navigate(R.id.fragment_select_to_fragment_accurate)
                    "Face recognition" -> it.findNavController().navigate(R.id.fragment_select_to_fragment_face_recognition)
                    "Pose detection with repeat" -> it.findNavController().navigate(R.id.fragment_select_to_fragment_pose_detection_repeat)
                }
            }
        }

    }

}