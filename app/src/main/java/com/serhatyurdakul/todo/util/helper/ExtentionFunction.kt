package com.serhatyurdakul.todo.util.helper

import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView

/*
* This class provides for extension function for loading image and performing animation
* with less code
*/

fun ImageView.load(res: Int) {
    ImageLoader.load(res, this)
}

fun ImageView.load(url: String?) {
    ImageLoader.load(url, this)
}

var isAnimating = false
fun View.slideUp(duration: Int = 500) {
    visibility = View.VISIBLE
    val animate = TranslateAnimation(0f, 0f, this.height.toFloat(), 0f)
    animate.duration = duration.toLong()
    animate.fillAfter = true
    animate.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {}

        override fun onAnimationEnd(animation: Animation?) {
            isAnimating = false
        }

        override fun onAnimationStart(animation: Animation?) {
            isAnimating = true
        }
    })
    this.startAnimation(animate)
}

fun View.slideDown(duration: Int = 500) {
    visibility = View.VISIBLE
    val animate = TranslateAnimation(0f, 0f, 0f, this.height.toFloat())
    animate.duration = duration.toLong()
    animate.fillAfter = true
    animate.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {}

        override fun onAnimationEnd(animation: Animation?) {
            isAnimating = false
        }

        override fun onAnimationStart(animation: Animation?) {
            isAnimating = true
        }
    })
    this.startAnimation(animate)
}