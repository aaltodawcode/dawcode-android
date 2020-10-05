package fi.aalto.roopepalomaki.distanceawarebarcode

import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

class RxResultActivity : AppCompatActivity() {

    private var bitMode: BitMode? = null

    private var imgIndex = 0
    private var imgNames = emptyArray<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rx_result)

        when (intent.extras["EXTRA_BITMODE"] as BitMode) {
            BitMode.ONE -> {
                imgNames = arrayOf(".quantized.png")
            }
        }

        val bitmap = BitmapFactory.decodeFile(intent.extras["EXTRA_FILENAME"] as String + imgNames[0])
        findViewById<ImageView>(R.id.codeImageView).setImageBitmap(bitmap)

        findViewById<ImageView>(R.id.codeImageView).setOnClickListener {
            imgIndex = (imgIndex + 1) % imgNames.size
            val bitmap = BitmapFactory.decodeFile(intent.extras["EXTRA_FILENAME"] as String + imgNames[imgIndex])
            (it as ImageView).setImageBitmap(bitmap)
        }

        if (intent.hasExtra("EXTRA_BYTES")) {
            findViewById<TextView>(R.id.textViewResult).text = String(intent.extras["EXTRA_BYTES"] as ByteArray)
        }
        if (intent.hasExtra("EXTRA_DIDPASSCHECKSUM")) {
            findViewById<TextView>(R.id.textViewChecksum).text =
                    (intent.extras["EXTRA_DIDPASSCHECKSUM"] as Boolean).toString() + " (" +
                    (intent.extras["EXTRA_NUMBEROFERRORSCORRECTED"] as Int).toString() + " errors corrected)"
            if (intent.extras["EXTRA_DIDPASSCHECKSUM"] as Boolean) {
                findViewById<TextView>(R.id.textViewChecksum).setBackgroundColor(Color.GREEN)
            } else {
                findViewById<TextView>(R.id.textViewChecksum).setBackgroundColor(Color.RED)
            }
        }

        if (intent.hasExtra("EXTRA_SIMILARITY_NEAR") && intent.hasExtra("EXTRA_SIMILARITY_FAR")) {
            findViewById<TextView>(R.id.textViewSimilarity).text =
                    "NEAR: " + (intent.extras["EXTRA_SIMILARITY_NEAR"] as Float).toString() +
                    " FAR: " + (intent.extras["EXTRA_SIMILARITY_FAR"] as Float).toString()
        }

        if (intent.hasExtra("EXTRA_STRINGOUTPUT")) {
            findViewById<TextView>(R.id.textViewResult).text = intent.extras["EXTRA_STRINGOUTPUT"] as String
        }
    }
}
