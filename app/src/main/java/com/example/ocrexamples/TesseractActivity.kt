package com.example.ocrexamples

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.googlecode.leptonica.android.ReadFile
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class TesseractActivity : AppCompatActivity() {

    companion object {
        const val TAG = "TesseractActivity"
    }

    private var tessBaseAPI: TessBaseAPI? = null
    private var button: Button? = null
    private var previewView: PreviewView? = null
    private var textView: TextView? = null

    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture : ImageCapture
    private lateinit var cameraProvider : ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var preview : Preview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tesseract)
        initUI()
    }

    private fun initUI() {
        textView = findViewById(R.id.textView)
        previewView = findViewById(R.id.preview)

        bindPreview()

        button = findViewById(R.id.button)
        button?.setOnClickListener { capture() }
        tessBaseAPI = TessBaseAPI()
        val dir = "$filesDir/tesseract"
        if (checkLanguageFile("$dir/tessdata")) tessBaseAPI!!.init(dir, "eng")
    }

    fun bindPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                .build()

            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview.setSurfaceProvider(previewView?.surfaceProvider)

            cameraProvider.bindToLifecycle(this as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkLanguageFile(dir: String): Boolean {
        val file = File(dir)
        if (!file.exists() && file.mkdirs()) createFiles(dir) else if (file.exists()) {
            val filePath = "$dir/eng.traineddata"
            val langDataFile = File(filePath)
            if (!langDataFile.exists()) createFiles(dir)
        }
        return true
    }

    private fun createFiles(dir: String) {
        val assetMgr = this.assets
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = assetMgr.open("eng.traineddata")
            val destFile = "$dir/eng.traineddata"
            outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            inputStream.close()
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun capture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            imageCapture = display?.let {
                ImageCapture.Builder()
                    .setTargetRotation(it.rotation)
                    .build()
            }!!
        }

        val name = SimpleDateFormat("yyyyMMdd", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture);

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    button?.text = "텍스트 인식 중..."
                    CoroutineScope(Dispatchers.IO).launch {
                        val resultDeferred : Deferred<String> = async {
                            val bitmap = output.savedUri?.let { getBitmap(it) }
                            if (bitmap != null) {
                                tessBaseAPI?.setImage(ReadFile.readBitmap(bitmap))
                            }
                            tessBaseAPI?.utF8Text ?: ""
                        }

                        withContext(Dispatchers.Main) {
                            button?.text = "텍스트 인식 완료!"
                            textView?.text = resultDeferred.await()
                        }

                    }
                    Log.d(TAG, msg)
                    bindPreview()
                }
            }
        )
    }

    private fun getARGBBitmap(img: Bitmap): Bitmap? {
        return img.copy(Bitmap.Config.ARGB_8888, true)
    }


    private fun getBitmap(uri: Uri) : Bitmap? {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getARGBBitmap(ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)))
            } else {
                getARGBBitmap(MediaStore.Images.Media.getBitmap(contentResolver, uri))
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
        return null
    }
}