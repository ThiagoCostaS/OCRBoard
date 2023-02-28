package com.example.ocrboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import java.io.IOException

class MainActivity : AppCompatActivity() {
    var cameraView: SurfaceView? = null
    var textView: TextView? = null
    var cameraSource: CameraSource? = null
    var button: Button? = null
    var button_close: Button? = null
    val RequestCameraPermission = 1001

    val mercosulRegex = Regex("[A-Z]{3}[0-9][A-Z][0-9]{2}")
    val normalRegex = Regex("[A-Z]{3}[0-9]{4}")

    // 3
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RequestCameraPermission -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    try {
                        cameraSource!!.start(cameraView!!.holder)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView = findViewById(R.id.surface_view)
        textView = findViewById(R.id.text_view)
        button = findViewById<Button?>(R.id.button_camera).apply {
            setOnClickListener {
                openCamera()
            }
        }

        // 1
        val textRecognizer = TextRecognizer.Builder(applicationContext).build()
        if (!textRecognizer.isOperational) {
            Log.w("MainActivity", "Detected dependence are not found ")
        } else {
            cameraSource = CameraSource.Builder(applicationContext, textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(2.0f)
                .setAutoFocusEnabled(true)
                .build()
            cameraView?.holder?.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    openCamera()
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    cameraSource?.stop()
                }
            })

            // 4
            textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
                override fun release() {}
                override fun receiveDetections(detections: Detections<TextBlock>) {
                    val items = detections.detectedItems
                    if (items.size() != 0) {
                        textView?.post {
                            val stringBuilder = StringBuilder()
                            for (i in 0 until items.size()) {
                                val item = items.valueAt(i)
                                stringBuilder.append(item.value)
                                stringBuilder.append("\n")
                            }

                            val text = stringBuilder.toString()

                            val mercosulResult = mercosulRegex.find(text)
                            val normalResult = normalRegex.find(text)

                            if (mercosulResult != null) {
                                textView?.setTextColor(Color.GREEN)
                                cameraSource?.stop()
                                textView?.text = mercosulResult.value
                            } else if (normalResult != null) {
                                textView?.setTextColor(Color.BLUE)
                                cameraSource?.stop()
                                textView?.text = normalResult.value
                            } else {
                                textView?.setTextColor(Color.RED)
                                textView?.text = text
                            }

                        }
                    }
                }
            })
        }
    }

    private fun openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(Manifest.permission.CAMERA),
                    RequestCameraPermission
                )
            }
            cameraSource?.let {
                it.start(cameraView?.holder)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}