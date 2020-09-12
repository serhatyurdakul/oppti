package com.serhatyurdakul.todo.util.helper

import android.text.TextUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.serhatyurdakul.todo.R

/*
* Image loader using Glide.
*/
class ImageLoader {
    companion object {
        fun load(location: Int, imageView: ImageView) {
            val requestOptions = RequestOptions()
                .timeout(20000)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)

            Glide.with(imageView.context)
                .load(if (location > 0) location else R.drawable.ic_avatar)
                .apply(requestOptions)
                .into(imageView)
        }

        fun load(location: String?, imageView: ImageView) {
            val requestOptions = RequestOptions()
                .timeout(20000)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)

            Glide.with(imageView.context)
                .load(if (TextUtils.isEmpty(location)) R.drawable.ic_avatar else location)
                .apply(requestOptions)
                .into(imageView)
        }

        fun load(location: String, imageView: ImageView, placeholder: Int) {
            val requestOptions = RequestOptions()
                .timeout(20000)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(placeholder)
                .error(placeholder)

            Glide.with(imageView.context)
                .load(if (TextUtils.isEmpty(location)) placeholder else location)
                .apply(requestOptions)
                .into(imageView)
        }
    }
}