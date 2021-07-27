package ru.igormesharin.posedetection.fragments.face

import android.Manifest
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import com.google.android.gms.tasks.OnSuccessListener
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import ru.igormesharin.posedetection.databinding.FragmentFaceRecognitionBinding
import ru.igormesharin.posedetection.utils.FaceNetModel
import ru.igormesharin.posedetection.utils.FrameAnalyser
import java.io.File

class FaceRecognition : Fragment() {

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE )
    private lateinit var cameraTextureView : TextureView
    private lateinit var frameAnalyser  : FrameAnalyser

    // Use Firebase MLKit to crop faces from images present in "/images" folder
    private val cropWithBBoxes : Boolean = false

    // Initialize MLKit Face Detector
    private val realTimeOpts = FaceDetectorOptions.Builder()
        .enableTracking()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

    // Create an empty ( String , FloatArray ) Hashmap for storing the data.
    private var imageData = ArrayList<Pair<String,FloatArray>>()
    private var imageLabelPairs = ArrayList<Pair<Bitmap,String>>()

    // Declare the FaceNet model variable.
    private var model : FaceNetModel? = null

    // To show the number of images processed.
    private var progressDialog : ProgressDialog? = null

    // Number of images in which no faces were detected.
    private var numImagesWithNoFaces = 0

    // View binding
    private lateinit var binding: FragmentFaceRecognitionBinding

    // CameraX provider
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFaceRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraTextureView = binding.cameraTextureView
        val boundingBoxOverlay = binding.bboxOverlay
        frameAnalyser = FrameAnalyser(requireContext(), boundingBoxOverlay)
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Loading images...")
        progressDialog?.setCancelable(false)
        model = FaceNetModel(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            startCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))

        cameraTextureView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        // Necessary to keep the Overlay above the TextureView so that the boxes are visible.
        boundingBoxOverlay.setWillNotDraw(false)
        boundingBoxOverlay.setZOrderOnTop(true)

//        if ( ActivityCompat.checkSelfPermission( requireContext() , Manifest.permission.WRITE_EXTERNAL_STORAGE ) ==
//            PackageManager.PERMISSION_GRANTED ){
            // Read image data
            scanStorageForImages(File(requireActivity().getExternalFilesDir(null).toString() + "/images"))
//        }

    }

    private fun scanStorageForImages(imagesDir : File) {
        Log.d("FaceRecognition", "scanStorage")
        progressDialog?.show()
        val imageSubDirs = imagesDir.listFiles()
        if (imageSubDirs == null) {
            progressDialog?.dismiss()
        } else {
//            for (image in imagesDir.listFiles()) {
//                Log.d("FaceRecognition", "Reading file -> ${image.name}")
//                imageLabelPairs.add(Pair(BitmapFactory.decodeFile(image.absolutePath), imagesDir.name))
//            }
            // List all the images in the "images" dir. Create a Hashmap of <Path,Bitmap> from them.
            for (imageSubDir in imagesDir.listFiles()) {
                Log.d( "FaceRecognition", "Reading directory -> ${imageSubDir.name}" )
                if (imageSubDir.listFiles() != null) {
                    for (image in imageSubDir.listFiles()) {
                        Log.d( "FaceRecognition", "Reading file --> ${image.name}" )
                        imageLabelPairs.add(Pair(BitmapFactory.decodeFile(image.absolutePath), imageSubDir.name))
                    }
                }
            }
            // Initiate the loop
            if (imageLabelPairs.isNotEmpty()){
                Log.d("FaceRecognition", "Elon in a loop")
                scanImage(0)
            }
            else {
                progressDialog?.dismiss()
                Toast.makeText(
                    requireContext() ,
                    "Found ${imageSubDirs.size} directories with no image(s)."
                    , Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun scanImage( counter : Int ) {
        Log.d("FaceRecognition", "scanImage")
        // Get the Bitmap
        val sample = imageLabelPairs[ counter ]
        val inputImage = InputImage.fromByteArray(bitmapToNV21(sample.first),
            sample.first.width,
            sample.first.height,
            0,
            InputImage.IMAGE_FORMAT_NV21
        )
        val successListener = OnSuccessListener<List<Face?>> { faces ->
            Log.d("FaceRecognition", "Faces size on success listener ${faces.size}")
            if (faces.isNotEmpty()) {
                // Append face embeddings to imageData
                imageData.add(
                    Pair(sample.second,
                        if (cropWithBBoxes) {
                            model!!.getFaceEmbedding(
                                sample.first,
                                faces[0]!!.boundingBox,
                                false,
                                true
                            )
                        // We are not checking whether the rear camera is really on/off
                        // We pass 'true' here so as to avoid the "180 degrees" flip transform.
                        }
                        else {
                            model!!.getFaceEmbeddingWithoutBBox(sample.first)
                        }
                    )
                )
            }
            else {
                numImagesWithNoFaces += 1
            }
            // Check if all images have been processed.
            if (counter + 1  == imageLabelPairs.size) {
                Toast.makeText(
                    requireContext(),
                    "Processing completed. Found ${imageData.size} image(s). " +
                            "Faces could not be detected in $numImagesWithNoFaces images.",
                    Toast.LENGTH_LONG
                ).show()
                // Dismiss the progressDialog
                progressDialog?.dismiss()
                frameAnalyser.faceList = imageData
            }
            else {
                // Else, update the message of the ProgressDialog
                progressDialog?.setMessage( "Processed ${counter+1} image(s)" )
                // Rerun this message with the updated counter.
                scanImage( counter + 1 )
            }
        }
        // addOnSuccessListener for face detection.
        detector.process(inputImage)
            .addOnSuccessListener(successListener)
            .addOnFailureListener {
                Log.d("FaceRecognition", it.localizedMessage)
            }
    }


    // Start the camera preview once the permissions are granted, also with the
    // given LensFacing ( FRONT or BACK ).
    private fun startCamera(cameraProvider: ProcessCameraProvider) {

        val preview = Preview.Builder()
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(binding.finder.createSurfaceProvider())

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(640, 480))
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), frameAnalyser)

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)

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

    private fun bitmapToNV21(bitmap: Bitmap): ByteArray {
        val argb = IntArray(bitmap.width * bitmap.height )
        bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val yuv = ByteArray(bitmap.height * bitmap.width + 2 * Math.ceil(bitmap.height / 2.0).toInt()
                * Math.ceil(bitmap.width / 2.0).toInt())
        encodeYUV420SP( yuv, argb, bitmap.width, bitmap.height)
        return yuv
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var R: Int
        var G: Int
        var B: Int
        var Y: Int
        var U: Int
        var V: Int
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                R = argb[index] and 0xff0000 shr 16
                G = argb[index] and 0xff00 shr 8
                B = argb[index] and 0xff shr 0
                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
                yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                    yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                }
                index++
            }
        }
    }

}