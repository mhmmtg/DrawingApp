package net.mguler.drawingapp

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mguler.drawingapp.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var btnCurrentColor: View

    private lateinit var brushDialog: Dialog
    private lateinit var binding: ActivityMainBinding

    private lateinit var permLauncher: ActivityResultLauncher<String>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerLauncher()

        binding.ibtnGallery.setOnClickListener(::selectFromGallery)
        binding.ibtnSave.setOnClickListener(::clickExportFile)

        //Select first
        binding.drawingView.setBrushSize(10f)
        binding.drawingView.setBrushColor(binding.ibtn1.tag.toString())
        btnCurrentColor = binding.llColors[0] as ImageButton
        btnCurrentColor.setBackgroundResource(R.drawable.rounded_bg_selected)

        binding.ibtnBrush.setOnClickListener { showBrushPicker() }
        binding.ibtnUndo.setOnClickListener { binding.drawingView.undo() }
        binding.ibtnDel.setOnClickListener { binding.drawingView.clear() }

        binding.ibtn1.setOnClickListener { setBrushColor(it) }
        binding.ibtn2.setOnClickListener { setBrushColor(it) }
        binding.ibtn3.setOnClickListener { setBrushColor(it) }
        binding.ibtn4.setOnClickListener { setBrushColor(it) }
        binding.ibtn5.setOnClickListener { setBrushColor(it) }
        binding.ibtn6.setOnClickListener { setBrushColor(it) }
    }

    private fun showBrushPicker() {
        brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size:")

        val btnSmallBrush: ImageButton = brushDialog.findViewById(R.id.ibSmallBrush)
        btnSmallBrush.setOnClickListener { setBrushSize(5F) }

        val btnMediumBrush: ImageButton = brushDialog.findViewById(R.id.ibMediumBrush)
        btnMediumBrush.setOnClickListener { setBrushSize(10F) }

        val btnBigBrush: ImageButton = brushDialog.findViewById(R.id.ibBigBrush)
        btnBigBrush.setOnClickListener { setBrushSize(20F) }

        brushDialog.show()

    }

    private fun setBrushSize(size: Float) {
        binding.drawingView.setBrushSize(size)
        brushDialog.dismiss()
    }

    private fun setBrushColor(view: View) {
        if (view !== btnCurrentColor) {
            val colorTag = view.tag.toString()
            binding.drawingView.setBrushColor(colorTag)

            //Set selected bg
            view.setBackgroundResource(R.drawable.rounded_bg_selected)
            btnCurrentColor.setBackgroundResource(R.drawable.rounded_bg)
            btnCurrentColor = view

            // TODO: color picker
        }

    }

    private fun registerLauncher() {
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) { binding.imageView.setImageURI(it.data?.data) }
            }

        permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show() }
            else { println("denied") }
        }
    }

    private fun selectFromGallery(view: View) {
        val perm_read = READ_EXTERNAL_STORAGE
        val GRANTED = PackageManager.PERMISSION_GRANTED

        when {
            ContextCompat.checkSelfPermission(this, perm_read) ==  GRANTED -> {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(galleryIntent)
            }

            shouldShowRequestPermissionRationale(perm_read) -> {
                Snackbar.make(view, "Permission needed!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give It!") { permLauncher.launch(perm_read) }
                    .show()
            }

            else -> { permLauncher.launch(perm_read) }
        }

    }

    private fun viewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        //Background
        val bg = view.background
        if (bg == null) { canvas.drawColor(Color.WHITE)}
        else { bg.draw(canvas) }

        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveBitmap(bitmap: Bitmap?): String {
        var result = ""

        withContext(Dispatchers.Main) {
            bitmap?.let {
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    //val file = File(filesDir.absolutePath + "/tmp.jpg")
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir.absolutePath + "/" + UUID.randomUUID() + ".png")

                    val fos = FileOutputStream(file)
                    fos.write(bytes.toByteArray())
                    fos.close()

                    result = file.absolutePath

                    runOnUiThread {
                        if (result.isNotEmpty()) {
                            clickShare(result)
                            Toast.makeText(this@MainActivity, "File saved to Downloads!", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(this@MainActivity, "File NOT saved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception) { println(e.localizedMessage) }
            }
        }

        return result
    }

    private fun clickExportFile(view: View) {
        val perm_write = WRITE_EXTERNAL_STORAGE
        val GRANTED = PackageManager.PERMISSION_GRANTED

        when {
            ContextCompat.checkSelfPermission(this, perm_write) ==  GRANTED -> {
                lifecycleScope.launch { saveBitmap(viewToBitmap(binding.flCanvas)) }
            }

            shouldShowRequestPermissionRationale(perm_write) -> {
                Snackbar.make(view, "Permission needed!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give It!") { permLauncher.launch(perm_write) }
                    .show()
            }

            else -> { permLauncher.launch(perm_write) }
        }

    }

    private fun clickShare(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) { path, uri ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
            }

            val shareIntent = Intent.createChooser(sendIntent, "Share")
            startActivity(shareIntent)
        }

    }


}