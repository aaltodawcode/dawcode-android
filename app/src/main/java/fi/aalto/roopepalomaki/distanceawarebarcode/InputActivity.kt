package fi.aalto.roopepalomaki.distanceawarebarcode

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText

class InputActivity : AppCompatActivity() {

    companion object {
        var defaultNear = "1n49shIEw1pN0/0:4Pg:je3PwoQCG.yTKVVNunxAadM8mKNZGaIFyypc0i4XT0AAjyCRKM:hICQpNytzYcwdw/:uMnn.D/l//9khhNAUaKHsgOZ/SLzBtMc8"
        var defaultFar = "DPKttlh5tAEj/RQwo/pwoZwfeNIkumzMoChHHbvVpwYYngi0Gic/iFBn9HKdldGlsNJmqcN145rhDAm6WUYwypYN7jJvxvFo8Tfz0PS5rqJRAj3L.s2vAdvc"
    }

    private var inputNear: EditText? = null
    private var inputFar: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        inputNear = findViewById(R.id.edittext_near)
        inputFar = findViewById(R.id.edittext_far)

        // default random input
        inputNear?.setText(defaultNear)
        inputFar?.setText(defaultFar)
    }

    fun startTx(view: View) {
        val bitMode = intent.extras["EXTRA_BITMODE"] as BitMode // pass the same bitmode forward
        val intent = Intent(this, TxActivity::class.java)
        intent.putExtra("EXTRA_BITMODE", bitMode)
        intent.putExtra("EXTRA_INPUT_NEAR", inputNear?.text.toString())
        intent.putExtra("EXTRA_INPUT_FAR", inputFar?.text.toString())
        startActivity(intent)
    }
}
