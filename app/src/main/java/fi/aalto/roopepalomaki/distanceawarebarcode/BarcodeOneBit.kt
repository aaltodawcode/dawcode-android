package fi.aalto.roopepalomaki.distanceawarebarcode

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.graphics.ColorUtils.colorToHSL
import android.util.Log
import kotlin.math.floor
import kotlin.math.min
import java.util.*


class BarcodeOneBit {

    companion object {
        private const val TAG = "BarcodeOneBit"

        @JvmStatic private external fun cppEncodeRS255onebit(str: String): IntArray

        init {
            System.loadLibrary("coder")
        }

        private const val paddingPercentage = .1f

        private val ditherColorChoices = arrayOf(
                Color.rgb(0, 0, 0), // black
                Color.rgb(0, 0, 0),
                Color.rgb(0, 200, 200), // cyan
                Color.rgb(0, 200, 200),
                Color.rgb(255, 0, 0), // red + green = yellow
                Color.rgb(0, 255, 0),
                Color.rgb(255, 0, 0), // red + blue = purple
                Color.rgb(0, 0, 255)
        )

        private val colorChoices = arrayOf(
                Color.rgb(0, 0, 0),    //black
                Color.rgb(220, 40, 40),  //red
                Color.rgb(40, 220, 40),  //green
                Color.rgb(40, 40, 220),  //blue
                Color.rgb(220, 140, 0),//orange
                //Color.rgb(440, 2240, 140),//teal
                Color.rgb(140, 40, 220),//purple
                Color.rgb(220, 40, 140),//pink
                Color.rgb(40, 220, 140) //light blue
        )

        private fun printColor(color: Int) {
            //val A = color shr 24 and 0xff
            val r = color shr 16 and 0xff
            val g = color shr 8 and 0xff
            val b = color and 0xff

            val hsl = FloatArray(3)
            colorToHSL(color, hsl)

            Log.d("color " + color.toString() + ", ","R: $r, G: $g, B: $b, H: ${hsl[0]}, S: ${hsl[1]}, L: ${hsl[2]}")
        }

        /*
        Distance barcode
         */

        fun drawDitherBarcode(canvas: Canvas, stringNear: String, stringFar: String, height: Int, width: Int) {
            val encodedNear = cppEncodeRS255onebit(stringNear)
            val encodedFar = cppEncodeRS255onebit(stringFar)

            Log.d(TAG, "encode - bits near: " + encodedNear.joinToString(""))
            Log.d(TAG, "encode - bits far: " + encodedFar.joinToString(""))

            val colorIndices = IntArray(CODE_HEIGHT * CODE_WIDTH)
            for (i in 0 until CODE_HEIGHT * CODE_WIDTH) {
                if (encodedNear[i] == 0 && encodedFar[i] == 0) {
                    colorIndices[i] = CHOICE_00
                } else if (encodedNear[i] == 1 && encodedFar[i] == 0) {
                    colorIndices[i] = CHOICE_10
                } else if (encodedNear[i] == 0 && encodedFar[i] == 1) {
                    colorIndices[i] = CHOICE_01
                } else {
                    colorIndices[i] = CHOICE_11
                }
            }

            for ((i, colorIndex) in colorIndices.withIndex()) {
                drawDitherBlock(canvas, colorIndex, i, height, width)
            }

            drawCorners(canvas, height, width)

        }

        fun drawEvaluationBarcode(canvas: Canvas, height: Int, width: Int) {
            // draw all possible blocks in order for evaluation, repeat until code full

            val colorIndices = IntArray(CODE_HEIGHT * CODE_WIDTH)
            for (i in 0 until CODE_HEIGHT * CODE_WIDTH) {
                colorIndices[i] = i % UNIQUE_CODE_VALUES_ONEBIT
            }

            for ((i, colorIndex) in colorIndices.withIndex()) {
                drawDitherBlock(canvas, colorIndex, i, height, width)
            }

            drawCorners(canvas, height, width)
        }

        private fun drawCorners(canvas: Canvas, height: Int, width: Int) {
            val size = min(height, width)
            val paddingSize = size * paddingPercentage
            val halfPaddingSize = paddingSize / 2f
            val radius = paddingSize / 2f

            //val offset = paddingSize * 0.1f // gap for separation from possible green corners in the actual code

            val greenPaint = Paint()
            greenPaint.color = Color.argb(255, 10, 255, 10)
            greenPaint.style = Paint.Style.FILL

            //canvas.drawRect(0f+offset, 0f+offset, paddingSize-offset, paddingSize-offset, greenPaint) //tl
            //canvas.drawRect(0f+offset, size - paddingSize+offset, paddingSize-offset, size.toFloat()-offset, greenPaint) //bl
            //canvas.drawRect(size - paddingSize+offset, size - paddingSize+offset, size.toFloat()-offset, size.toFloat()-offset, greenPaint) //br
            //canvas.drawRect(size - paddingSize+offset, 0f+offset, size.toFloat()-offset, paddingSize-offset, greenPaint) //tr

            canvas.drawCircle(0f + halfPaddingSize, 0f + halfPaddingSize, radius, greenPaint) // tl
            canvas.drawCircle(0f + halfPaddingSize, size - halfPaddingSize, radius, greenPaint) // bl
            canvas.drawCircle(size - halfPaddingSize, size - halfPaddingSize, radius, greenPaint) // br
            canvas.drawCircle(size - halfPaddingSize, 0f + halfPaddingSize, radius, greenPaint) // tr
        }

        private fun drawDitherBlock(canvas: Canvas, colorIndex: Int, blockIndex: Int, height: Int, width: Int) {
            val originX = width * paddingPercentage
            val originY = height * paddingPercentage
            val w = (width - width * paddingPercentage * 2) / CODE_WIDTH
            val h = (height - height * paddingPercentage * 2) / CODE_HEIGHT

            val x = originX + w * (blockIndex % CODE_WIDTH)
            val y = originY + h * floor((blockIndex / CODE_HEIGHT).toFloat())

            val offsetX = w * SEPARATOR_OFFSET_PC // creates black grid between modules
            val offsetY = h * SEPARATOR_OFFSET_PC

            Log.d(TAG, "module w $w height $height width $width actual w ${(x + w - offsetX)-(x+offsetX)}")

            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = ditherColorChoices[colorIndex * 2]
            canvas.drawRect(x + offsetX, y + offsetY, x + w / 2f, y + h / 2f, paint)
            canvas.drawRect(x + w / 2f, y + h / 2f, x + w - offsetX, y + h - offsetY, paint)
            paint.color = ditherColorChoices[colorIndex * 2 + 1]
            canvas.drawRect(x + w / 2f, y + offsetY, x + w - offsetX, y + h / 2f, paint)
            canvas.drawRect(x + offsetX, y + h / 2f, x + w / 2f, y + h - offsetY, paint)
        }

        private fun getDitherBlockMidpointPercentage(blockIndex: Int): Array<Float> {
            val correction = 0.005f

            val originX = paddingPercentage / 2f + correction// actual code origin, excludes corners
            val originY = paddingPercentage / 2f + correction
            val width = 1f - paddingPercentage - 2f * correction
            val height = 1f - paddingPercentage - 2f * correction
            val w = width / CODE_WIDTH
            val h = height / CODE_HEIGHT
            val x = originX + w * (blockIndex % CODE_WIDTH)
            val y = originY + h * floor((blockIndex / CODE_HEIGHT).toFloat())

            return floatArrayOf(x + w / 2f, y + h / 2f).toTypedArray()
        }

        @JvmStatic fun getDitherBlockMidpoints(): Array<Array<Float>> {
            val midpoints = ArrayList<Array<Float>>()
            for (i in 0 until (CODE_HEIGHT * CODE_WIDTH)) {
                midpoints.add(getDitherBlockMidpointPercentage(i))
            }
            return midpoints.toTypedArray()
        }
    }

}