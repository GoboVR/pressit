package com.pressit.app

import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.pressit.app.compressors.AudioVideoCompressor
import com.pressit.app.compressors.ImageCompressor
import com.pressit.app.databinding.ActivityMainBinding
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedUri: Uri? = null
    private var selectedMime: String? = null
    private var resultFile: File? = null

    private val units = arrayOf("KB", "MB")

    private val pickFileLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedMime = contentResolver.getType(uri)
            binding.tvSelectedFile.text = uri.lastPathSegment ?: uri.toString()
            binding.btnCompress.isEnabled = true
            binding.btnShare.visibility = android.view.View.GONE
            binding.tvStatus.text = ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spinnerUnit.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, units
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerUnit.setSelection(1) // default MB

        binding.btnPickFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
        }

        binding.btnCompress.setOnClickListener { startCompression() }

        binding.btnShare.setOnClickListener { shareResult() }
    }

    private fun startCompression() {
        val uri = selectedUri ?: return
        val mime = selectedMime ?: ""
        val sizeText = binding.etTargetSize.text.toString()
        val sizeValue = sizeText.toDoubleOrNull()
        if (sizeValue == null || sizeValue <= 0) {
            Toast.makeText(this, "Enter a valid target size", Toast.LENGTH_SHORT).show()
            return
        }
        val unitIsMb = binding.spinnerUnit.selectedItem == "MB"
        val targetBytes = if (unitIsMb) (sizeValue * 1024 * 1024).toLong() else (sizeValue * 1024).toLong()

        binding.btnCompress.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.progressBar.progress = 5
        binding.tvStatus.text = "Compressing…"
        binding.btnShare.visibility = android.view.View.GONE

        val outDir = File(getExternalFilesDir(null), "output").apply { mkdirs() }

        thread {
            var ok = false
            var outFile: File? = null
            try {
                when {
                    mime.startsWith("image/") -> {
                        outFile = File(outDir, "compressed_${System.currentTimeMillis()}.jpg")
                        val orientation = readExifOrientation(uri)
                        contentResolver.openInputStream(uri)?.use { input ->
                            ok = ImageCompressor.compress(input, outFile, targetBytes, orientation)
                        }
                    }
                    mime.startsWith("video/") -> {
                        outFile = File(outDir, "compressed_${System.currentTimeMillis()}.mp4")
                        val localCopy = copyToCache(uri, "in_video")
                        ok = AudioVideoCompressor.compressVideo(
                            localCopy.absolutePath, outFile, targetBytes
                        ) { p -> runOnUiThread { binding.progressBar.progress = p } }
                        localCopy.delete()
                    }
                    mime.startsWith("audio/") -> {
                        outFile = File(outDir, "compressed_${System.currentTimeMillis()}.m4a")
                        val localCopy = copyToCache(uri, "in_audio")
                        ok = AudioVideoCompressor.compressAudio(
                            localCopy.absolutePath, outFile, targetBytes
                        ) { p -> runOnUiThread { binding.progressBar.progress = p } }
                        localCopy.delete()
                    }
                    else -> ok = false
                }
            } catch (e: Exception) {
                ok = false
            }

            runOnUiThread {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnCompress.isEnabled = true
                if (ok && outFile != null && outFile.exists()) {
                    resultFile = outFile
                    val kb = outFile.length() / 1024.0
                    binding.tvStatus.text = "Done — result is ${"%.1f".format(kb)} KB " +
                        "(target was ${"%.1f".format(targetBytes / 1024.0)} KB)"
                    binding.btnShare.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvStatus.text = "Compression failed. Try a larger target size or a different file."
                }
            }
        }
    }

    private fun readExifOrientation(uri: Uri): Int {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun copyToCache(uri: Uri, prefix: String): File {
        val ext = when {
            selectedMime?.contains("mp4") == true -> "mp4"
            else -> "tmp"
        }
        val file = File(cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    private fun shareResult() {
        val file = resultFile ?: return
        val uri = FileProvider.getUriForFile(this, "com.pressit.app.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Save or share compressed file"))
    }
}
