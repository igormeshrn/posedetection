package ru.igormesharin.posedetection.fragments.face

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import ru.igormesharin.posedetection.databinding.FragmentFaceMatchingBinding
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.pow
import kotlin.math.sqrt

class FaceMatchingFragment : Fragment() {

    private lateinit var binding: FragmentFaceMatchingBinding
    private lateinit var safeContext: Context
    private lateinit var originalBitmap: Bitmap
    private lateinit var testBitmap: Bitmap
    private lateinit var cropped: Bitmap

    // TFLite model for face recognition (.tflite file in assets directory)
    private var model: Interpreter? = null

    // Image embeddings
    private var originalEmbedding = Array(1) { FloatArray(128) }
    private var testEmbedding = Array(1) { FloatArray(128) }

    private var imageSizeX: Int = 0
    private var imageSizeY: Int = 0

    companion object {
        private const val TAG = "FaceMatchingFragment"
        private const val MODEL_FILE_NAME = "Qfacenet.tflite"
        private const val IMAGE_MEAN = 0.0f
        private const val IMAGE_STD = 1.0f
    }


    /**
        CALLBACK FUNCTIONS
    **/

    override fun onAttach(context: Context) {
        safeContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFaceMatchingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initModel()
        configureButtons()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                12 -> {
                    val imageUri = data.data
                    try {
                        originalBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                        binding.imgOriginal.setImageBitmap(originalBitmap)
                        binding.btnOriginal.visibility = View.GONE
                        detectFace(originalBitmap, "original")
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                13 -> {
                    val imageUri = data.data
                    try {
                        testBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                        binding.imgTest.setImageBitmap(testBitmap)
                        binding.btnTest.visibility = View.GONE
                        detectFace(testBitmap, "test")
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    /**
        PRIVATE FUNCTIONS
    **/

    private fun initModel() {
        try {
            model = Interpreter(loadModelFile())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configureButtons() {
        // Loading an original image
        binding.btnOriginal.setOnClickListener {
            Intent().apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(this, "Select picture"), 12)
            }
        }

        // Loading an image to match with the original image
        binding.btnTest.setOnClickListener {
            Intent().apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(this, "Select picture"), 13)
            }
        }

        // Start model and get result
        binding.btnVerify.setOnClickListener {
            val distance = calculateDistance(originalEmbedding, testEmbedding)

            if (distance < 6.0) {
                binding.txtResult.text = "Result: Same Faces"
            } else {
                binding.txtResult.text = "Result: Different Faces"
            }

        }

        // Clear images
        binding.btnClear.setOnClickListener {
            binding.btnTest.visibility = View.VISIBLE
            binding.btnOriginal.visibility = View.VISIBLE
            binding.imgOriginal.setImageBitmap(null)
            binding.imgTest.setImageBitmap(null)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = requireActivity().assets.openFd(MODEL_FILE_NAME)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun calculateDistance(originalEmbedding: Array<FloatArray>, testEmbedding: Array<FloatArray>): Double {
        var sum = 0.0
        for (i in 0..127) {
            sum += (originalEmbedding[0][i] - testEmbedding[0][i]).toDouble().pow(2.0)
        }
        return sqrt(sum)
    }

    private fun detectFace(bitmap: Bitmap, imageType: String) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val detector = FaceDetection.getClient()
        detector.process(image)
            .addOnSuccessListener { faces -> // Task completed successfully
                for (face in faces) {
                    val bounds = face.boundingBox
                    cropped = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
                    getEmbeddings(cropped, imageType)
                }
            }
            .addOnFailureListener { e -> // Task failed with an exception
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                Log.e(TAG, e.localizedMessage)
            }
    }

    private fun getEmbeddings(bitmap: Bitmap, imageType: String) {
        var inputImageBuffer: TensorImage
        val embedding = Array(1) { FloatArray(128) }

        val imageTensorIndex = 0
        val imageShape: IntArray = model?.getInputTensor(imageTensorIndex)!!.shape() // {1, height, width, 3}
        imageSizeY = imageShape[1]
        imageSizeX = imageShape[2]
        val imageDataType: DataType = model?.getInputTensor(imageTensorIndex)!!.dataType()

        inputImageBuffer = TensorImage(imageDataType)

        inputImageBuffer = loadImage(bitmap, inputImageBuffer)

        model?.run(inputImageBuffer.buffer, embedding)

        when (imageType) {
            "original" -> originalEmbedding = embedding
            "test" -> testEmbedding = embedding
        }
    }

    private fun loadImage(bitmap: Bitmap, inputImageBuffer: TensorImage): TensorImage {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap)

        // Creates processor for the TensorImage.
        val cropSize = bitmap.width.coerceAtMost(bitmap.height)

        val imageProcessor: ImageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(getPreprocessNormalizeOp())
            .build()
        return imageProcessor.process(inputImageBuffer)
    }

    private fun getPreprocessNormalizeOp(): TensorOperator {
        return NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    }

}