package ru.igormesharin.posedetection.fragments.pose

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import ru.igormesharin.posedetection.databinding.CameraFragmentBinding
import ru.igormesharin.posedetection.utils.PoseLinesView

class RealtimePoseDetectionFragment : Fragment() {

    private lateinit var binding: CameraFragmentBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var safeContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = CameraFragmentBinding.inflate(inflater, container, false)
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

    @SuppressLint("UnsafeExperimentalUsageError")
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
//            .setTargetResolution(Size(480, 640))
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(safeContext)) { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image

//            Log.d("PoseDetectionFragment", "Image: image width: ${image?.width}, image height: ${image?.height}")
//            Log.d("PoseDetectionFragment", "Finder: width: ${binding.finder.width}, height: ${binding.finder.height}")

            if (image != null) {
                val processImage = InputImage.fromMediaImage(image, rotationDegrees)
//                val factor = binding.finder.width / image.width
                poseDetector
                    .process(processImage)
                    .addOnSuccessListener { pose ->
                        if(binding.finder.childCount > 1){
                            binding.finder.removeViewAt(1)
                        }
                        if(pose.allPoseLandmarks.isNotEmpty()){
                            if(binding.finder.childCount > 1){
                                binding.finder.removeViewAt(1)
                            }

                            val element = PoseLinesView(safeContext, pose, factor = 1)
                            binding.finder.addView(element)
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

}