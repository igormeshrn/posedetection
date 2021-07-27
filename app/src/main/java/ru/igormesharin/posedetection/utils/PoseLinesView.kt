package ru.igormesharin.posedetection.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

@SuppressLint("ViewConstructor")
class PoseLinesView(context: Context?, var pose: Pose, var factor: Int) : View(context) {

    private var boundaryPaint: Paint
    private var leftPaint: Paint
    private var rightPaint: Paint

    init {
        boundaryPaint = Paint()
        boundaryPaint.color = Color.WHITE
        boundaryPaint.strokeWidth = 6f
        boundaryPaint.style = Paint.Style.STROKE

        leftPaint = Paint()
        leftPaint.strokeWidth = 6f
        leftPaint.color = Color.GREEN
        rightPaint = Paint()
        rightPaint.strokeWidth = 6f
        rightPaint.color = Color.YELLOW
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val landmarks = pose.allPoseLandmarks

        for (landmark in landmarks) {
            canvas?.drawCircle(translateX(landmark.position.x),landmark.position.y,8.0f,boundaryPaint)
        }

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
        val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
        val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
        val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
        val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
        val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
        val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
        val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)

        canvas?.drawLine(translateX(leftShoulder.position.x),leftShoulder.position.y,translateX(rightShoulder.position.x),rightShoulder.position.y,boundaryPaint)
        canvas?.drawLine(translateX(leftHip.position.x),leftHip.position.y,translateX(rightHip.position.x),rightHip.position.y,boundaryPaint)

        //Left body
        canvas?.drawLine(translateX(leftShoulder.position.x * factor), leftShoulder.position.y * factor, translateX(leftElbow.position.x * factor), leftElbow.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftElbow.position.x  * factor), leftElbow.position.y * factor, translateX(leftWrist.position.x * factor), leftWrist.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftShoulder.position.x * factor), leftShoulder.position.y * factor, translateX(leftHip.position.x * factor), leftHip.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftHip.position.x * factor), leftHip.position.y * factor, translateX(leftKnee.position.x * factor), leftKnee.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftKnee.position.x * factor), leftKnee.position.y * factor, translateX(leftAnkle.position.x * factor),leftAnkle.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftWrist.position.x * factor), leftWrist.position.y * factor, translateX(leftThumb.position.x * factor), leftThumb.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftWrist.position.x * factor), leftWrist.position.y * factor, translateX(leftPinky.position.x * factor), leftPinky.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftWrist.position.x * factor), leftWrist.position.y * factor, translateX(leftIndex.position.x * factor), leftIndex.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftIndex.position.x * factor), leftIndex.position.y * factor, translateX(leftPinky.position.x * factor), leftPinky.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftAnkle.position.x * factor), leftAnkle.position.y * factor, translateX(leftHeel.position.x * factor), leftHeel.position.y * factor, leftPaint)
        canvas?.drawLine(translateX(leftHeel.position.x * factor), leftHeel.position.y * factor, translateX(leftFootIndex.position.x * factor), leftFootIndex.position.y * factor, leftPaint)

        //Right body
        canvas?.drawLine(translateX(rightShoulder.position.x * factor), rightShoulder.position.y * factor, translateX(rightElbow.position.x * factor), rightElbow.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightElbow.position.x * factor), rightElbow.position.y * factor, translateX(rightWrist.position.x * factor), rightWrist.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightShoulder.position.x * factor), rightShoulder.position.y * factor, translateX(rightHip.position.x * factor), rightHip.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightHip.position.x * factor), rightHip.position.y * factor, translateX(rightKnee.position.x * factor), rightKnee.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightKnee.position.x * factor), rightKnee.position.y * factor, translateX(rightAnkle.position.x * factor), rightAnkle.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightWrist.position.x * factor), rightWrist.position.y * factor, translateX(rightThumb.position.x * factor), rightThumb.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightWrist.position.x * factor), rightWrist.position.y * factor, translateX(rightPinky.position.x * factor), rightPinky.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightWrist.position.x * factor), rightWrist.position.y * factor, translateX(rightIndex.position.x * factor), rightIndex.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightIndex.position.x * factor), rightIndex.position.y * factor, translateX(rightPinky.position.x * factor), rightPinky.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightAnkle.position.x * factor), rightAnkle.position.y * factor, translateX(rightHeel.position.x * factor), rightHeel.position.y * factor, rightPaint)
        canvas?.drawLine(translateX(rightHeel.position.x * factor), rightHeel.position.y * factor, translateX(rightFootIndex.position.x * factor), rightFootIndex.position.y * factor, rightPaint)
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun translateX(x: Float): Float {

        // you will need this for the inverted image in case of using front camera
        // return context.display?.width?.minus(x)!!

        return x;
    }


}
