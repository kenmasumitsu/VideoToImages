package com.test.videotoimages

import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = "MainActivity"
    }

    lateinit var videoToImages: VideoToImages
    lateinit var afd: AssetFileDescriptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        afd = assets.openFd("sample.mp4")

        // Create VideoToImages instance
        videoToImages = VideoToImages(this)
        videoToImages.onDecodeFrame = { bitmap: Bitmap, frameNo: Int ->
            saveBitmap(bitmap, frameNo)
            bitmap.recycle()
        }


        button_start.setOnClickListener{
            if (videoToImages.isRunning()) {
                Log.d(TAG, "Not start due to already running")
                return@setOnClickListener
            }

            // Start decoding
            videoToImages.run(afd)
        }

    }

    @Throws(IOException::class)
    fun saveBitmap(bitmap: Bitmap, frameNo: Int) {
        val dir = getExternalFilesDir(null)
        val file = File(dir, "test-${frameNo}.bmp")
        Log.d("TAG", "file: ${file.absolutePath}")
        val outStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.close()
    }
}