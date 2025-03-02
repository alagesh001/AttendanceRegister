package com.oxad.attendanceregister

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.oxad.attendanceregister.ui.theme.AttendanceRegisterTheme
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendanceRegisterTheme {
                checkCameraPermission()
                QRScannerWithButton()
                /*QRCodeScreen(
                        text = "4353453433",
                    )*/

            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)

        }
    }
    @Composable
    fun QRScannerWithButton() {
        var isScanning by remember { mutableStateOf(false) }
        var scannedText by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            if (isScanning) {
                QRCodeScanner { result ->
                    Log.e("adgadgadf","Yes $result")
                    scannedText = result
                    isScanning = false // Stop scanning after detection
                    valueIs = result
                    apicallmarkattendance()
                }
            }

            Button(onClick = { isScanning = true }) {
                Text("Scan QR Code")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (scannedText.isNotEmpty()) {
                Text("Scanned Result: $scannedText")
            }
        }
    }

    @Composable
    fun QRCodeScanner(onQRCodeScanned: (String) -> Unit) {
        val context = LocalContext.current
        val lifecycleOwner = context as LifecycleOwner
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val executorService: ExecutorService = remember { Executors.newSingleThreadExecutor() }
        Log.e("adgadgadf", "scanned A")

        AndroidView(
            factory = { ctx ->
                val previewView = androidx.camera.view.PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                cameraProviderFuture.addListener({
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                    Log.e("adgadgadf", "scanned B")

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    Log.e("adgadgadf", "scanned C")

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    Log.e("adgadgadf", "scanned D")

                    imageAnalysis.setAnalyzer(executorService) { imageProxy ->
                        Log.e("adgadgadf", "imageProxy "+imageProxy)

                        val result = scanQRCode(imageProxy)
                        result?.let {
                            onQRCodeScanned(it)
                            Log.e("adgadgadf", "scanned "+it)

                            imageProxy.close()

                        }
                    }
                    Log.e("adgadgadf", "scanned E")

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {

                        Log.e("adgadgadf", "Use case binding failed"+ exc)
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    private var valueIs:String ?=null
private fun apicallmarkattendance(){
    lifecycleScope.launch {
        Log.e("jadfadsf","B")

        val result= valueIs?.let { ApiRepository().markMyAttendance( it.toLong(),52196542) }
        Log.e("jadfadsf","C "+Gson().toJson(result))

        result?.onSuccess { response ->
            Log.e("jadfadsf","Check result "+Gson().toJson(response))
            if(response.success){
                Utils.showToast(this@MainActivity, response.message)


            }else{
                Utils.showToast(this@MainActivity, response.message)

            }
        }

        result?.onFailure { exception ->
            exception.message ?: "Something went wrong!"
        }            }
}
    private fun scanQRCode(imageProxy: ImageProxy): String? {
        val width = imageProxy.width
        val height = imageProxy.height

        // Convert YUV_420_888 to NV21
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)  // Swap V and U planes
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Use the correct size for PlanarYUVLuminanceSource
        val source = PlanarYUVLuminanceSource(
            nv21, width, height, 0, 0, width, height, false
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        return try {
            val result = MultiFormatReader().decode(binaryBitmap)
            Log.e("QRCodeScanner", "QR Code decoding Result: ${result.text}")
            Log.e("jadfadsf","A")
            result.text


        } catch (e: Exception) {
            Log.e("QRCodeScanner", "QR Code decoding failed: ${e.message}")
            null
        } finally {
            imageProxy.close() // Close imageProxy after processing
        }
    }
    @Composable
    fun QRCodeScreen(text: String) {
        val qrBitmap = remember { generateQRCode(text) }

        qrBitmap?.let {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp)
                )
            }
        }
    }


    @androidx.compose.ui.tooling.preview.Preview
    @Composable
    fun PreviewQRCode() {
       // QRCodeScreen("userid")
    }
}
