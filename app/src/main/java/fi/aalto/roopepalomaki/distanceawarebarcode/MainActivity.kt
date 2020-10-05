package fi.aalto.roopepalomaki.distanceawarebarcode

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


class MainActivity : AppCompatActivity() {

    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> Log.d("Main loaderCallback", "OpenCV loaded")
                else -> super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            Log.d("Main onResume", "OpenCV already loaded")
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            Log.d("Main onResume", "OpenCV not loaded")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, loaderCallback)
        }
    }



    fun startTx(view: View) {
        val intent = Intent(this, BitModeActivity::class.java)
        intent.putExtra("EXTRA_TASKMODE", TaskMode.TX)
        startActivity(intent)
    }

    fun startRx(view: View) {
        val intent = Intent(this, RxActivity::class.java)
        intent.putExtra("EXTRA_BITMODE", BitMode.ONE)
        startActivity(intent)
    }


}
