package com.anish.owee

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

import com.anish.owee.data.local.PreferenceManager

class OweeApp : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: OweeApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        PreferenceManager.getInstance(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}