package fi.aalto.roopepalomaki.distanceawarebarcode

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ToggleButton
import java.util.*

class TxActivity : AppCompatActivity() {

    class BarcodeView : View {
        private val TAG = "BarcodeView"

        var bitMode: BitMode? = null
        var stringNear: String? = null
        var stringFar: String? = null

        //
        constructor(context: Context?) : super(context)
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
        constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
        constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

        override fun onDraw(canvas: Canvas?) {
            if (canvas != null) {

                when (bitMode) {
                    BitMode.ONE -> {
                        BarcodeOneBit.drawDitherBarcode(canvas, stringNear!!, stringFar!!, this.measuredHeight, this.measuredWidth)
                        //BarcodeOneBit.drawEvaluationBarcode(canvas, height, width)
                    }
                }

            }
        }
    }

    private lateinit var barcodeView: BarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tx)

        if (!Settings.System.canWrite(this)) {
            allowWritePermission()
        }

        barcodeView = findViewById(R.id.barcodeView)

        barcodeView.bitMode = intent.extras["EXTRA_BITMODE"] as BitMode
        barcodeView.stringNear = intent.extras["EXTRA_INPUT_NEAR"] as String
        barcodeView.stringFar = intent.extras["EXTRA_INPUT_FAR"] as String
    }

    private fun Context.allowWritePermission(){
        val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

}
