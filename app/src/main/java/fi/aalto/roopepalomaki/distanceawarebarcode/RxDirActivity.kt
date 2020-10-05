package fi.aalto.roopepalomaki.distanceawarebarcode

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import java.io.File


class RxDirActivity : AppCompatActivity() {

    private var bitMode: BitMode? = null

    private var TAG = "RxDirActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rx_dir)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        bitMode = intent.extras["EXTRA_BITMODE"] as BitMode
    }

    fun pickFolder(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        startActivityForResult(Intent.createChooser(intent, "Choose directory"), 9999)
    }

    private fun processFolder(path: String) {
        /*
        NOTE: correct file order in the log file is not guaranteed,
        must sort according to the first column.
         */
        val dir = File(path)
        val files = dir.listFiles()
        for ((i, file) in files.iterator().withIndex()) {
            /*
            Ignore possible debug images from CV processing in case the same folder
            in case the same folder is processed more than once
            */
            val debugImageNames = arrayOf("quantized", "thresh", "rect", "warp", "smooth",
                    "midpoints", "nearcorrectness", "farcorrectness", "histogram")
            if (debugImageNames.none { file.path.contains(it) }) {
                Log.d(TAG, "Processing file ${i+1} of ${files.size} at: " + file.path)
                when (bitMode) {
                    BitMode.ONE -> CVOneBit.decode(file.path)

                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            9999 -> {
                val uri = data?.data
                val fullPath = FileUtils.getFullPathFromTreeUri(this, uri)!!
                Log.d(TAG, fullPath)
                processFolder(fullPath)
            }
        }
    }
}
