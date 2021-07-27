package ru.igormesharin.posedetection.fragments.face

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.*
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
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import ru.igormesharin.posedetection.databinding.FragmentFaceRecognitionBinding
import ru.igormesharin.posedetection.utils.FaceNetModel
import ru.igormesharin.posedetection.utils.FrameAnalyser
import ru.igormesharin.posedetection.utils.bitmapToNV21
import java.io.File

class FaceRecognitionFragment : Fragment() {

    private lateinit var binding: FragmentFaceRecognitionBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var safeContext: Context
    private lateinit var frameAnalyser: FrameAnalyser
    private lateinit var cameraTextureView : TextureView

    // Initialize MLKit Face Detector
    private val customFaceDetectionOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .enableTracking()
        .build()
    private val faceDetector = FaceDetection.getClient(customFaceDetectionOptions)

    // Create an empty (String, FloatArray) Hashmap for storing the data.
    private var imageData = ArrayList<Pair<String,FloatArray>>()
    private var imageLabelPairs = ArrayList<Pair<Bitmap,String>>()

    // Declare the FaceNet model variable.
    private var model : FaceNetModel? = null

    // To show the number of images processed.
    private var progressDialog : ProgressDialog? = null

    // Number of images in which no faces were detected.
    private var numImagesWithNoFaces = 0

    override fun onAttach(context: Context) {
        safeContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFaceRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        cameraTextureView = binding.cameraTextureView
        val boundingBoxOverlay = binding.bboxOverlay
        frameAnalyser = FrameAnalyser( safeContext, boundingBoxOverlay)
        progressDialog = ProgressDialog(safeContext)
        progressDialog?.setMessage("Loading images ...")
        progressDialog?.setCancelable(false)
        model = FaceNetModel(safeContext)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(safeContext))

        cameraTextureView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        // Necessary to keep the Overlay above the TextureView so that the boxes are visible.
        boundingBoxOverlay.setWillNotDraw(false)
        boundingBoxOverlay.setZOrderOnTop(true)

        scanStorageForImages(
            File(requireActivity().getExternalFilesDir(null).toString() + "/images")
        )
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
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), frameAnalyser)

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
    }

    private fun scanStorageForImages(imagesDir: File) {
        progressDialog?.show()
        val imageSubDirs = imagesDir.listFiles()
        if ( imageSubDirs == null ) {
            progressDialog?.dismiss()
        } else {
            // List all the images in the "images" dir. Create a Hashmap of <Path,Bitmap> from them.
            for (imageSubDir in imagesDir.listFiles()) {
                Log.i("Image Processing", "Reading directory --> ${imageSubDir.name}")
                for (image in imageSubDir.listFiles()) {
                    Log.i("Image Processing", "Reading file --> ${image.name}")
                    imageLabelPairs.add(
                        Pair(BitmapFactory.decodeFile(image.absolutePath), imageSubDir.name)
                    )
                }
            }
            // Initiate the loop
            if (imageLabelPairs.isNotEmpty()){
                scanImage(0)
            } else {
                progressDialog?.dismiss()
                Toast.makeText(
                    safeContext,
                    "Found ${imageSubDirs.size} directories with no image(s).",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun scanImage(counter: Int) {
        // Get the Bitmap
        val sample = imageLabelPairs[counter]

        val inputImage = InputImage.fromByteArray(
            bitmapToNV21(sample.first),
            sample.first.width,
            sample.first.height,
            0,
            InputImage.IMAGE_FORMAT_NV21
        )

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    imageData.add(
                        Pair(sample.second, model!!.getFaceEmbeddingWithoutBBox(sample.first))
                    )
                } else {
                    numImagesWithNoFaces += 1
                }

                // Check if all images have been processed.
                if (counter + 1  == imageLabelPairs.size) {
                    Toast.makeText(
                        safeContext,
                        "Processing completed. Found ${imageData.size} image(s). " +
                                "Faces could not be detected in $numImagesWithNoFaces images."
                        ,Toast.LENGTH_LONG
                    ).show()

                    progressDialog?.dismiss()
                    frameAnalyser.faceList = imageData
                } else {
                    progressDialog?.setMessage( "Processed ${counter + 1} image(s)" )
                    scanImage(counter + 1)
                }
            }
    }

    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = cameraTextureView.width.div(2f)
        val centerY = cameraTextureView.height.div(2f)
        val rotationDegrees = when(cameraTextureView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX , centerY )
        cameraTextureView.setTransform(matrix)
    }

}