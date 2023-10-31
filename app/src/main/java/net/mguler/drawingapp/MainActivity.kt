package net.mguler.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.DISPLAY_NAME
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.provider.MediaStore.Images.Media.HEIGHT
import android.provider.MediaStore.Images.Media.MIME_TYPE
import android.provider.MediaStore.Images.Media.WIDTH
import android.provider.MediaStore.Images.Media.getContentUri
import android.view.View
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mguler.drawingapp.databinding.ActivityMainBinding
import net.mguler.drawingapp.databinding.DialogBrushSizeBinding
import net.mguler.drawingapp.databinding.HiddenButtonsBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private lateinit var btnCurrentColor: View
    private lateinit var binding: ActivityMainBinding

    private lateinit var permLauncher2: ActivityResultLauncher<String>

    private var writePermGranted = false

    private var isBgAdded = false
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerLauncher()
        registerPickMedia()

        //Select first
        binding.drawingView.setBrushSize(10f)
        binding.drawingView.setBrushColor(binding.ibtn1.tag.toString())
        btnCurrentColor = binding.llColors[0]
        btnCurrentColor.setBackgroundResource(R.drawable.rounded_bg_selected)

        //Click events
        binding.fabDel.setOnClickListener { binding.drawingView.clear() }
        binding.fabDel.setOnLongClickListener { showHiddenIcons(it); true }

        binding.ibtn1.setOnClickListener { setBrushColor(it) }
        binding.ibtn2.setOnClickListener { setBrushColor(it) }
        binding.ibtn3.setOnClickListener { setBrushColor(it) }
        binding.ibtn4.setOnClickListener { setBrushColor(it) }
        binding.ibtn5.setOnClickListener { setBrushColor(it) }
        binding.ibtn6.setOnClickListener { setBrushColor(it) }
    }

    private fun setBrushSize(size: Float) {
        binding.drawingView.setBrushSize(size)
    }

    private fun setBrushColor(view: View) {
        if (view !== btnCurrentColor) {
            val colorTag = view.tag.toString()
            binding.drawingView.setBrushColor(colorTag)

            //Set selected bg
            view.setBackgroundResource(R.drawable.rounded_bg_selected)
            btnCurrentColor.setBackgroundResource(R.drawable.rounded_bg)
            btnCurrentColor = view

            // TODO: color picker hidden menu
        }

    }



    private fun registerPickMedia(){
        pickMedia = registerForActivityResult(PickVisualMedia()) { uri ->
            uri?.let {
                binding.imageView.setImageURI(it)
                isBgAdded = true
            }
        }
    }

    private fun selectFromGallery() {
        if (isBgAdded) {
            binding.imageView.setImageURI(null)
            isBgAdded = false }
        else {
            pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
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
                            //clickShare(result)
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

    private fun registerLauncher() {
        permLauncher2 = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            writePermGranted = it

            if (it) {
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show() }
            else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun askWritePermission() {
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val permWrite = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val permGranted = PackageManager.PERMISSION_GRANTED
        val hasWritePerm = ContextCompat.checkSelfPermission(this, permWrite) ==  permGranted

        writePermGranted = hasWritePerm || minSdk29

        when {
            //Granted
            writePermGranted -> {
                saveToExternal()
            }
            //Rationale
            shouldShowRequestPermissionRationale(permWrite) -> {
                Snackbar.make(binding.root, "Permission needed to save image!", Snackbar.LENGTH_LONG)
                    .setAction("Give It!") { permLauncher2.launch(permWrite) }
                    .show()
            }
            //Denied-Not asked
            else -> { saveToExternal() }
        }

    }

    private fun saveToInternal(){
        val bmp = viewToBitmap(binding.flCanvas)

        val filename = System.currentTimeMillis().toString() + ".png"
        openFileOutput(filename, MODE_PRIVATE).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 90, it)
        }
    }

    private fun saveToExternal() {
        val bmp = viewToBitmap(binding.flCanvas)
        val name = System.currentTimeMillis().toString() + ".png"
        val up29Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            else { EXTERNAL_CONTENT_URI }


        val contentValues = ContentValues().apply {
            put(DISPLAY_NAME, name)
            put(MIME_TYPE, "image/png")
            put(WIDTH, bmp.width)
            put(HEIGHT, bmp.height)
        }

        try {
            contentResolver.insert(up29Uri, contentValues).also { uri->
                contentResolver.openOutputStream(uri!!).use { outputStream->
                    try {
                        bmp.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                    }
                    catch (e: Exception) {
                        Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                    Toast.makeText(this, "Saved to Pictures", Toast.LENGTH_SHORT).show()
                }
            }
        }
        catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
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


    private fun showBrushPopup(v: View, p: PopupWindow){
        val popup = DialogBrushSizeBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(popup.root, WRAP_CONTENT, WRAP_CONTENT, true)

        popup.ibSmallBrush.setOnClickListener { setBrushSize(5F); popupWindow.dismiss(); p.dismiss() }
        popup.ibMediumBrush.setOnClickListener { setBrushSize(10F); popupWindow.dismiss(); p.dismiss() }
        popup.ibBigBrush.setOnClickListener { setBrushSize(20F); popupWindow.dismiss(); p.dismiss() }

        popupWindow.showAsDropDown(v,-120,40)
    }

    private fun showInfoDialog() {
        //val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar)
        dialog.setContentView(R.layout.dialog_info)
        dialog.show()
    }

    private fun showHiddenIcons(v: View) {
        val popup = HiddenButtonsBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(popup.root, WRAP_CONTENT, WRAP_CONTENT, true)

        popup.fabBrush.setOnClickListener { showBrushPopup(v, popupWindow) }
        popup.fabUndo.setOnClickListener { binding.drawingView.undo() }

        popup.fabGallery.setOnClickListener{ selectFromGallery() }
        //popup.ibtnSave.setOnClickListener{ clickExportFile() }
        popup.fabSave.setOnClickListener{ askWritePermission() }

        popup.fabInfo.setOnClickListener { showInfoDialog() }

        popupWindow.showAsDropDown(v,0,10)
    }

}


// TODO: icon alignment

/*
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

 if (!writePermGranted) {
            permLauncher2.launch(permWrite)
        }
        else { saveToExternal() }


 */