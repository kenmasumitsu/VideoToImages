# VideoToImages

Sample program that decoding a video file to bitmap images.
Android MediaCodec is used for the decoder.

The rough sequences are
1. MediaCodec decodes a video file to Image objects
2. [YuvToRgbConverter](https://github.com/android/camera-samples/blob/3730442b49189f76a1083a98f3acf3f5f09222a3/CameraUtils/lib/src/main/java/com/example/android/camera/utils/YuvToRgbConverter.kt) converts the Image objects to Bitmap objects.




