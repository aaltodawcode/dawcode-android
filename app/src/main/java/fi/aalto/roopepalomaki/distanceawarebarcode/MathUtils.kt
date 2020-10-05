package fi.aalto.roopepalomaki.distanceawarebarcode

import android.util.Log

class MathUtils {
    companion object {

        private const val TAG = "MathUtils"

        fun posMod(a: Int, b: Int): Int {
            /*
            Non-negative modulo (% returns remainder in Kotlin, can be negative)
             */
            var r = a.rem(b)
            if (r < 0) r += b
            return r
        }

        fun posMod(a: Double, b: Double): Double {
            /*
            Non-negative modulo (% returns remainder in Kotlin, can be negative)
             */
            var r = a.rem(b)
            if (r < 0) r += b
            return r
        }

        fun findInsertionPoint(arr: DoubleArray, item: Double, maxHue: Double): Int {
            for (i in 0 until arr.size) {
                val th0 = arr[i]
                var th1 = arr[posMod(i+1, arr.size)]

                if (item >= th0 && item < th1) {
                    return i
                }

                // check crossing over max hue
                if (th1 < th0) {
                    // fix floating point error next to zero with +.01
                    if ((item >= th0 && item < (th1 + maxHue + 0.01)) ||
                            (item >= (th0 - maxHue) && item < th1)) {
                        return i
                    }
                }
            }

            return -1
        }

        fun getHistogramPeaks(histogram: DoubleArray): IntArray {
            /*
            Returns peak indices based on a naive peak search (i-1 and i+1 lower than i)
             */
            var peaks = mutableListOf<Int>()

            Log.d(TAG, "histogram " + histogram.joinToString { "%.3f".format(it) })

            for (i in 0 until histogram.size) {
                val next = posMod(i + 1, histogram.size)
                val previous = posMod(i - 1, histogram.size)
                //Log.d("prev next", previous.toString() + " " + next.toString())
                if (histogram[i] > histogram[previous] && histogram[i] > histogram[next]) {
                    peaks.add(i)
                }
            }

            return peaks.toIntArray()
        }

    }
}