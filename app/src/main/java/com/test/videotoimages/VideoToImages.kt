package com.test.videotoimages

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log

class VideoToImages(context: Context) {
    companion object {
        val TAG = "VideoToImages"
        val TIMEOUT_US = 10000L
    }

    private val yuvToRgbConverter = YuvToRgbConverter(context)
    private var thread: Thread = Thread()

    // It is called a frame is decoded during decoding.
    var onDecodeFrame: ((bitmap: Bitmap, frameNo: Int) -> Unit)? = null

    fun isRunning(): Boolean {
        return thread.isAlive
    }

    // This sample takes AssetFileDescriptor object for a video file in assets.
    // You can easily modify to take a video file in File or URL.
    fun run(videoAsset: AssetFileDescriptor) {
        if (isRunning()) {
            Log.d(TAG, "Failed. already running")
            return
        }

        thread = Thread {

            var decoder: MediaCodec? = null
            val extractor = MediaExtractor()
            extractor.setDataSource(videoAsset)

            for (i in 0..extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)

                if (mime!=null && mime.startsWith("video/")) {
                    extractor.selectTrack(i)
                    decoder = MediaCodec.createDecoderByType(mime)
                    format.setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        CodecCapabilities.COLOR_FormatYUV420Flexible
                    )
                    decoder.configure(format, null, null, 0)
                    break
                }
            }

            if (decoder == null) {
                Log.e(TAG, "Failed to open the video file")
                return@Thread
            }

            decoder.start()

            val info = MediaCodec.BufferInfo()
            var isEOS = false
            var frameNo = 0

            while (!Thread.interrupted()) {
                if (!isEOS) {
                    val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)
                        val sampleSize = extractor.readSampleData(buffer!!, 0)
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM")
                            decoder.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isEOS = true
                        } else {
                            decoder.queueInputBuffer(
                                inIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = decoder.getOutputFormat()
                        Log.d("DecodeActivity", "New format " + format);
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                    }
                    else -> {
                        val image = decoder.getOutputImage(outIndex)
                        if (image != null) {
                            val bmp = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                            yuvToRgbConverter.yuvToRgb(image, bmp)
                            onDecodeFrame?.let {
                                it(bmp, frameNo)
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        frameNo ++
                    }
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                    break
                }
            }
            decoder.stop()
            decoder.release()
            extractor.release()

        }
        thread.start()
    }
}