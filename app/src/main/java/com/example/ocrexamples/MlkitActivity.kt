package com.example.ocrexamples

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.android.odml.image.MlImage
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MlkitActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MlkitActivity"
    }

    private var previewView: PreviewView? = null
    private var textView: TextView? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var cameraSelector: CameraSelector? = null
    private var textRecognizer: TextRecognizer? = null
    private var analysisUseCase: ImageAnalysis? = null

    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    @ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mlkit)
        textView = findViewById(R.id.textView)
        previewView = findViewById(R.id.preview)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture?.addListener({
            cameraProvider = cameraProviderFuture!!.get()
            bindPreviewUseCase()
            bindAnalyzeUserCase()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreviewUseCase() {
        preview = Preview.Builder().build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        if (preview != null) {
            preview!!.setSurfaceProvider(previewView?.surfaceProvider)

            cameraProvider!!.bindToLifecycle(this as LifecycleOwner, cameraSelector!!, preview)
        }
    }

    @ExperimentalGetImage
    private fun bindAnalyzeUserCase() {
        textRecognizer?.close()
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

        analysisUseCase = ImageAnalysis.Builder().build()
        analysisUseCase!!.setAnalyzer(
            ContextCompat.getMainExecutor(this)
        ) { imageProxy: ImageProxy ->
            try {
                processImageProxy(imageProxy)
            } catch (e: MlKitException) {
                Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }

        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @ExperimentalGetImage
    fun processImageProxy(image: ImageProxy) {
        val mlImage = MediaMlImageBuilder(image.image!!).setRotation(image.imageInfo.rotationDegrees).build()
        requestDetectInImage(mlImage)
            .addOnCompleteListener { image.close() }

    }

    private fun requestDetectInImage(image: MlImage): Task<Text> {
        return setUpListener(textRecognizer!!.process(image))
    }

    private fun setUpListener(task: Task<Text>, ): Task<Text> {
        return task.addOnSuccessListener(executor) { results: Text ->
                val textResult: Text = results
                textView?.text = textResult.text
            }
    }
}