package fi.aalto.roopepalomaki.distanceawarebarcode

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View


class BitModeActivity : AppCompatActivity() {

    private var taskMode: TaskMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bit_choice)

        taskMode = intent.extras["EXTRA_TASKMODE"] as TaskMode
    }

    fun startOneBit(view: View) {
        when (taskMode) {
            TaskMode.TX -> {
                val intent = Intent(this, InputActivity::class.java)
                intent.putExtra("EXTRA_BITMODE", BitMode.ONE)
                startActivity(intent)
            }
            TaskMode.RX -> {
                val intent = Intent(this, RxActivity::class.java)
                intent.putExtra("EXTRA_BITMODE", BitMode.ONE)
                startActivity(intent)
            }
            TaskMode.RX_DIR -> {
                val intent = Intent(this, RxDirActivity::class.java)
                intent.putExtra("EXTRA_BITMODE", BitMode.ONE)
                startActivity(intent)
            }
        }
    }
}
