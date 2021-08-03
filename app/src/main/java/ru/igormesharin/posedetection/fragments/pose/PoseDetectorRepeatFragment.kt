package ru.igormesharin.posedetection.fragments.pose

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import ru.igormesharin.posedetection.databinding.FragmentPoseRecognitionBinding
import ru.igormesharin.posedetection.utils.GraphicOverlay
import ru.igormesharin.posedetection.utils.PoseDetectorProcessor
import ru.igormesharin.posedetection.utils.VisionImageProcessor
import java.lang.Exception

class PoseDetectorRepeatFragment : Fragment() {

    private lateinit var binding: FragmentPoseRecognitionBinding
    private lateinit var safeContext: Context
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var imageProcessor: VisionImageProcessor? = null

    companion object {
        private const val TAG = "PoseDetectorRepeatFragment"
    }


    /**
     *  Callback functions
     */

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPoseRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCamera()
    }


    /**
     *  Private functions
     */

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview.setSurfaceProvider(binding.preview.createSurfaceProvider())

            val poseDetectorOptions = PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()

            imageProcessor = PoseDetectorProcessor(
                safeContext, poseDetectorOptions, true, true, true,
                true, true
            )

            val imageAnalysis = ImageAnalysis.Builder()
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(safeContext)) { imageProxy ->
                try {
                    imageProcessor!!.processImageProxy(imageProxy, binding.graphicOverlay)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(
                        safeContext,
                        e.localizedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
        }, ContextCompat.getMainExecutor(safeContext))
    }

}