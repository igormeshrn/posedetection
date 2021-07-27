package ru.igormesharin.posedetection.fragments.face

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.google.mlkit.vision.face.FaceDetection
import ru.igormesharin.posedetection.databinding.CameraFragmentBinding
import ru.igormesharin.posedetection.utils.FaceBoundsView

class FaceDetection : Fragment() {

    private lateinit var binding: CameraFragmentBinding
    private lateinit var faceDetector: FaceDetector
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
        configureFaceDetector()
    }

    private fun configureFaceDetector() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(safeContext))

        val customFaceDetectionOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .enableTracking()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        faceDetector = FaceDetection.getClient(customFaceDetectionOptions)
    }

    @SuppressLint("UnsafeExperimentalUsageError", "ShowToast")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
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
                val views: MutableList<View> = mutableListOf()
                faceDetector
                    .process(processImage)
                    .addOnSuccessListener { faces ->
                        if(binding.finder.childCount > 1){
                            binding.finder.removeViews(1, binding.finder.childCount - 1)
                        }
                        if (faces.isNotEmpty()) {
                            faces.forEach { face ->
                                val element: FaceBoundsView
                                if (face.leftEyeOpenProbability != null && face.rightEyeOpenProbability != null) {
                                    if (face.leftEyeOpenProbability < 0.1f || face.rightEyeOpenProbability < 0.1f) {
                                        element = FaceBoundsView(safeContext, face.boundingBox, false)
                                    } else {
                                        element = FaceBoundsView(safeContext, face.boundingBox, true)
                                    }
                                } else {
                                    element = FaceBoundsView(safeContext, face.boundingBox, true)
                                }
                                binding.finder.addView(element)
                                views.add(element)

                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.d("FaceDetection", e.localizedMessage)
                        imageProxy.close()
                    }
            }
        }

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
    }

}