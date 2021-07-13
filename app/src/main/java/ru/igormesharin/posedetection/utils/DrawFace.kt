package ru.igormesharin.posedetection.utils

import android.content.Context
import android.graphics.*
import android.media.FaceDetector
import android.view.View

class DrawFace(context: Context, var rect: Rect, var isEyeOpen: Boolean) : View(context) {

    private var boundaryPaint = Paint().apply {
        if (isEyeOpen) {
            color = Color.parseColor("#FFCA28")
        } else {
            color = Color.parseColor("#b71c1c")
        }
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawRoundRect(
            rect.left.toFloat(), rect.top.toFloat(),
            rect.right.toFloat(), rect.bottom.toFloat(),
            16F, 16F,
            boundaryPaint)
    }

}