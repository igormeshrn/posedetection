package ru.igormesharin.posedetection.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import ru.igormesharin.posedetection.databinding.FragmentPoseDetectionBinding
import ru.igormesharin.posedetection.utils.Draw

class PoseDetectionFragment : Fragment() {

    private lateinit var binding: FragmentPoseDetectionBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var safeContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPoseDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurePoseDetector()
    }

    private fun configurePoseDetector() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(safeContext))

        val customPoseDetectionOptions = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()

        poseDetector = PoseDetection.getClient(customPoseDetectionOptions)
    }

    @SuppressLint("UnsafeExperimentalUsageError", "ShowToast")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(binding.finder.createSurfaceProvider())

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(binding.finder.width, binding.finder.height))
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(safeContext)) { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image

//            Log.d("PoseDetectionFragment", "Image: image width: ${image?.width}, image height: ${image?.height}")
//            Log.d("PoseDetectionFragment", "Finder: width: ${binding.finder.width}, height: ${binding.finder.height}")

            if (image != null) {
                val processImage = InputImage.fromMediaImage(image, rotationDegrees)
                poseDetector
                    .process(processImage)
                    .addOnSuccessListener { pose ->
                        if (pose.allPoseLandmarks.isNotEmpty()) {
                            if (isHandAboveHead(pose)) {
                                binding.root.setBackgroundColor(Color.GREEN)
                            } else {
                                binding.root.setBackgroundColor(Color.WHITE)
                            }
                        }

                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        imageProxy.close()
                    }
            }
        }

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
    }

    private fun isHandAboveHead(pose: Pose): Boolean {
        return pose.getPoseLandmark(PoseLandmark.LEFT_WRIST).position.y <
        pose.getPoseLandmark(PoseLandmark.LEFT_EAR).position.y ||
        pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST).position.y <
        pose.getPoseLandmark(PoseLandmark.RIGHT_EAR).position.y
    }

}