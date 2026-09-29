package com.aerostudio.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aerostudio.mobile.databinding.ActivityMainBinding
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.common.ConnectChecker

class MainActivity : AppCompatActivity(), ConnectChecker {

    private lateinit var binding: ActivityMainBinding
    private var rtmpCamera: RtmpCamera2? = null

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rtmpCamera = RtmpCamera2(binding.liveGlView, this)

        if (allPermissionsGranted()) {
            startCameraPreview()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, 1001)
        }

        binding.btnStream.setOnClickListener {
            val rtmpUrl = binding.etRtmpUrl.text.toString().trim()
            if (rtmpUrl.isEmpty()) {
                Toast.makeText(this, "Masukkan URL RTMP terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            rtmpCamera?.let { camera ->
                if (!camera.isStreaming) {
                    if (camera.prepareAudio() && camera.prepareVideo(1280, 720, 30, 2500 * 1024, 0)) {
                        camera.startStream(rtmpUrl)
                        binding.btnStream.text = "STOP"
                        binding.btnStream.setBackgroundColor(0xFF444444.toInt())
                    } else {
                        Toast.makeText(this, "Gagal menyiapkan encoder", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    camera.stopStream()
                    binding.btnStream.text = "GO LIVE"
                    binding.btnStream.setBackgroundColor(0xFFD50000.toInt())
                }
            }
        }
    }

    private fun startCameraPreview() {
        rtmpCamera?.startPreview(1280, 720)
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && allPermissionsGranted()) {
            startCameraPreview()
        } else {
            Toast.makeText(this, "Izin Kamera & Audio diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionStarted(url: String) {}
    override fun onConnectionSuccess() {
        runOnUiThread { Toast.makeText(this, "Terkoneksi ke Server!", Toast.LENGTH_SHORT).show() }
    }
    override fun onConnectionFailed(reason: String) {
        runOnUiThread { 
            Toast.makeText(this, "Koneksi Gagal: $reason", Toast.LENGTH_SHORT).show() 
            rtmpCamera?.stopStream()
            binding.btnStream.text = "GO LIVE"
        }
    }
    override fun onNewBitrate(bitrate: Long) {}
    override fun onDisconnect() {
        runOnUiThread { Toast.makeText(this, "Terputus dari Server", Toast.LENGTH_SHORT).show() }
    }
    override fun onAuthError() {
        runOnUiThread { Toast.makeText(this, "Auth Error RTMP", Toast.LENGTH_SHORT).show() }
    }
    override fun onAuthSuccess() {}
}
