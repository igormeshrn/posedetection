package ru.igormesharin.posedetection.utils

import android.graphics.Rect

data class Prediction(var bbox : Rect, var label : String )