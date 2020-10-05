package fi.aalto.roopepalomaki.distanceawarebarcode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import java.util.*
import android.support.design.widget.FloatingActionButton
import android.util.SparseIntArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat


class RxActivity : AppCompatActivity() {

    companion object {
        public lateinit var context: Context
    }

    private var TAG = "RxActivity"

    private var bitMode: BitMode? = null

    /*
     * Adapted from https://github.com/googlesamples/android-Camera2Basic/
     */

    private val STATE_PREVIEW = 0
    private val STATE_WAITING_LOCK = 1
    private val STATE_PICTURE_TAKEN = 4

    private var state = STATE_PREVIEW

    private val ORIENTATIONS = SparseIntArray()
    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    private var orientation = 0

    private lateinit var cameraId: String
    private lateinit var previewRequest: CaptureRequest
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewSize: Size
    private lateinit var previewTextureView: TextureView
    private var backgroundHandler: Handler? = null
    private var cameraDevice: CameraDevice? = null
    private val cameraFacing = CameraCharacteristics.LENS_FACING_BACK
    private var captureSession: CameraCaptureSession? = null
    private var handlerThread: HandlerThread? = null
    private var imageReader: ImageReader? = null

    private var imageDir: File? = null
    private var imageId: String? = null
    private var imageFileName: String? = null

    private var focusRectangle: MeteringRectangle? = null

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            when (state) {
                STATE_PREVIEW -> Unit
                STATE_WAITING_LOCK -> capturePicture(result)
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                state = STATE_PICTURE_TAKEN
                captureStillPicture()
            }
        }

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            process(result)
        }

    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(p0: CameraDevice?) {
            this@RxActivity.cameraDevice = p0
            createCameraPreviewSession()
        }

        override fun onDisconnected(p0: CameraDevice?) {
            p0?.close()
            this@RxActivity.cameraDevice = null
        }

        override fun onError(p0: CameraDevice?, p1: Int) {
            p0?.close()
            this@RxActivity.cameraDevice = null
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            setUpCamera()
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
        }
    }

    private val onImageAvailableListener = object : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader?) {
            var image: Image? = null
            try {
                image = reader?.acquireLatestImage()

                val buffer = image!!.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                var output: FileOutputStream? = null
                try {
                    output = FileOutputStream(createImageFile(imageDir!!)).apply {
                        write(bytes)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, e.toString())
                } finally {
                    image.close()
                    output?.let {
                        try {
                            it.close()
                        } catch (e: IOException) {
                            Log.e(TAG, e.toString())
                        }
                    }

                    // decode result
                    processCapturedImage()
                }
            } catch (e: Exception) {

            } finally {
                image?.close()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rx)

        bitMode = intent.extras["EXTRA_BITMODE"] as BitMode

        previewTextureView = findViewById(R.id.previewTextureView)
        context = this
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            createImageDir()
        }

        val captureButton = findViewById<FloatingActionButton>(R.id.captureButton)
        captureButton.setOnClickListener {
            onCaptureButtonClicked(0)
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (previewTextureView.isAvailable) {
            setUpCamera()
            openCamera()
        } else {
            previewTextureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onStop() {
        super.onStop()
        closeCamera()
        stopBackgroundThread()
    }

    private fun createImageDir() {
        val storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        imageDir = File(storageDirectory, resources.getString(R.string.app_name))
        if (imageDir != null && !imageDir!!.exists()) {
            val wasCreated = imageDir!!.mkdirs()
            if (!wasCreated) Log.e(TAG, "Failed to create image directory")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(galleryFolder: File): File {
        val imageFileNamePrefix = "image_" + imageId + "_"
        val imageFile = File.createTempFile(imageFileNamePrefix, ".jpg", galleryFolder)
        imageFileName = imageFile.absolutePath
        Log.d(TAG, "Set imageFileName to $imageFileName")
        return imageFile
    }

    private fun onCaptureButtonClicked(imageIdPostfix: Int) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        imageId = timeStamp + "_" + imageIdPostfix.toString()

        try {
            lockFocus() // starts capture
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processCapturedImage() {
        Log.d(TAG, "Done capturing, image filename: $imageFileName")
        val startTime = System.nanoTime()

        var result: DecodeResult? = null
        when (bitMode) {
            BitMode.ONE -> result = CVOneBit.decode(imageFileName)

        }

        val endTime = System.nanoTime()
        Log.d(TAG, "Processing took ${endTime-startTime} ns")

        startRxResult(result!!)
    }

    private fun setUpCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (id in cameraManager.cameraIdList) {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)

                val sensorArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val midX = (sensorArraySize.width() / 2f).toInt()
                val midY = (sensorArraySize.height() / 2f).toInt()
                focusRectangle = MeteringRectangle(Point(midX, midY), Size(200, 200), MeteringRectangle.METERING_WEIGHT_MAX - 1)

                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                    orientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                    val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizes = streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java)
                    val largest = sizes[0]

                    imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 1).apply {
                        setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                    }
                    //previewSize = sizes.find { it.height == 1080 && it.width == 1080 }!!
                    previewSize = largest

                    Log.d(TAG, "preview size ${previewSize.height} ${previewSize.width}")

                    cameraId = id
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        if (captureSession != null) {
            captureSession?.close()
            captureSession = null
        }

        if (cameraDevice != null) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private fun lockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START)
            state = STATE_WAITING_LOCK
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun captureStillPicture() {
        try {

            val captureBuilder = cameraDevice?.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                addTarget(imageReader?.surface!!)

                // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
                // We have to take that into account and rotate JPEG properly.
                // For devices with orientation of 90, we return our mapping from ORIENTATIONS.
                // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
                set(CaptureRequest.JPEG_ORIENTATION,
                        (ORIENTATIONS.get(windowManager.defaultDisplay.rotation) + orientation + 270) % 360)

                // set manual settings
                setCaptureSettings(this)

                // Use the same AF mode as the preview.
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                result: TotalCaptureResult) {
                    unlockFocus()
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureBuilder?.build(), captureCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler)
            state = STATE_PREVIEW

            captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun startBackgroundThread() {
        handlerThread = HandlerThread("camera_thread")
        handlerThread?.start()
        backgroundHandler = Handler(handlerThread?.looper)
    }

    private fun stopBackgroundThread() {
        if (backgroundHandler != null) {
            handlerThread?.quitSafely()
            handlerThread = null
            backgroundHandler = null
        }
    }

    private fun setCaptureSettings(builder: CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT)
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (1000000000/60).toLong())
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, 160)
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = previewTextureView.surfaceTexture
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)

            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(
                    Arrays.asList(surface, imageReader?.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession?) {
                            if (cameraDevice == null) return // camera is closed

                            captureSession = session
                            try {

                                // EXPOSURE

                                // auto on the center
                                //previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(focusRectangle))
                                //previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusRectangle))

                                // manual settings
                                setCaptureSettings(previewRequestBuilder)

                                // FOCUS
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                                previewRequest = previewRequestBuilder.build()
                                captureSession?.setRepeatingRequest(previewRequest, null, backgroundHandler)
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession?) {
                            Log.e(TAG, "Creating preview session failed")
                        }
                    },
                    null // createCaptureSession handler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startRxResult(result: DecodeResult) {
        val intent = Intent(this, RxResultActivity::class.java)
        intent.putExtra("EXTRA_BITMODE", bitMode)
        intent.putExtra("EXTRA_BYTES", result.bytes)
        intent.putExtra("EXTRA_DIDPASSCHECKSUM", result.didPassChecksum)
        intent.putExtra("EXTRA_NUMBEROFERRORSCORRECTED", result.numberOfErrorsCorrected)
        intent.putExtra("EXTRA_FILENAME", imageFileName)
        startActivity(intent)
    }
}
