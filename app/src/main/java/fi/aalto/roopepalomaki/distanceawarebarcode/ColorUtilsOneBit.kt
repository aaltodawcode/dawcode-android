package fi.aalto.roopepalomaki.distanceawarebarcode

class ColorUtilsOneBit {

    companion object {

        private const val MAJORITY = .75
        private const val MINORITY= .35
        private const val EXISTENCE = .05
        private const val BLACK_THRESHOLD = 0.2

        // indices in the order of the hue circle beginning from red
        private const val BLACK = 0
        private const val WHITE = 1
        private const val RED = 2
        private const val YELLOW = 3
        private const val GREEN = 4
        private const val CYAN = 5
        private const val BLUE = 6
        private const val PURPLE = 7

        @JvmStatic fun getHueThresholds(): DoubleArray {
            val hueThresholds = DoubleArray(NUMBER_OF_HUES)
            for (i in 0 until NUMBER_OF_HUES) {
                hueThresholds[i] = (180 / NUMBER_OF_HUES * i + 90 / NUMBER_OF_HUES).toDouble()
            }
            return hueThresholds
        }

        @JvmStatic fun quantizeHsv(hsv: DoubleArray, hueThresholds: DoubleArray): IntArray {
            val blackValue1 = 0.3 * 255
            val blackValue2 = 0.5 * 255
            val blackSaturation = 0.5 * 255
            val whiteSaturation = 0.6 * 255

            if (hsv[2] < blackValue1 || (hsv[2] < blackValue2 && hsv[1] < blackSaturation)) {
                return intArrayOf(QUANTIZED_BLACK)
            /* Improved version: do not consider white, uses cyan instead
            } else if (hsv[2] >= blackValue1 && hsv[1] < whiteSaturation) {
                return intArrayOf(QUANTIZED_WHITE)
            */
            } else {
                val invertedInsertionPoint = hueThresholds.binarySearch(hsv[0])
                var insertionPoint = if (invertedInsertionPoint < 0) (-invertedInsertionPoint - 1) else invertedInsertionPoint
                if (insertionPoint == NUMBER_OF_HUES) insertionPoint = 0
                //Log.d("hue pos", hsv[0].toString() + " " + insertionPoint.toString())
                return intArrayOf(QUANTIZED_COLOR, insertionPoint)
            }
        }

        @JvmStatic fun getFrequencyPercentages(list: List<IntArray>): DoubleArray {
            /*
            Returns frequencies in format [# of blacks, # of whites, # of reds, ...]
             */
            var frequencies = DoubleArray(NUMBER_OF_HUES + 2) { 0.0 }

            for (color in list) {
                when (color[0]) {
                    QUANTIZED_BLACK -> frequencies[0]++
                    QUANTIZED_WHITE -> frequencies[1]++
                    QUANTIZED_COLOR -> frequencies[color[1] + 2]++
                }
            }

            frequencies = frequencies.map { it / (list.size) } .toDoubleArray()

//            print("FREQS: ")
//            for (f in frequencies) print(f.toString() + " ")
//            println()

            return frequencies
        }

        @JvmStatic fun centerIsBlack(list: List<IntArray>): Boolean {
            /*
            Returns true when the center pixel of the sample area is black
             */
            val centerIndex = list.size / 2
            return (list[centerIndex][0] == QUANTIZED_BLACK)
        }

        private fun accountForAdjacentBleed(frequencies: DoubleArray): DoubleArray {
            /*
            Some bleeding can be accounted for by replacing the offending color into
            the assumed correct color since single primary colors should never be dominant
            if the counterpart does not exist (e.g. if a lot of red but no green or vice versa)
             */

            var output = frequencies

            // purple bleed turns yellow into red
            if (frequencies[GREEN] < EXISTENCE && frequencies[RED] > MINORITY) {
                output[YELLOW] += frequencies[RED]
                output[RED] = 0.0
            }
            // cyan bleed turns purple into blue and yellow into green
            if (frequencies[RED] < EXISTENCE && frequencies[GREEN] > MINORITY) {
                output[YELLOW] += frequencies[GREEN]
                output[GREEN] = 0.0
            }
            if (frequencies[RED] < EXISTENCE && frequencies[BLUE] > MINORITY) {
                output[PURPLE] += frequencies[BLUE]
                output[BLUE] = 0.0
            }

            return output
        }

        @JvmStatic fun getBitFromHistogram(frequencies: DoubleArray, centerIsBlack: Boolean): Int {
            /*
            Returns the bit based on the colors
             */

            //val f = accountForAdjacentBleed(frequencies)
            val f = frequencies

            // if center is not black, ignore black completely
            if (!centerIsBlack) {
                for (i in 0 until f.size) {
                    f[i] = f[i] / (1.0 - f[0])
                }
            }

            //Log.d("after black check", (f.joinToString()))

            val noRedOrGreen = f[RED] < EXISTENCE || f[GREEN] < EXISTENCE
            val noRedOrBlue = f[RED] < EXISTENCE || f[BLUE] < EXISTENCE

            // NOTE: black tends to get suppressed while white tends to bleed to adjacent blocks

            // obvious cases
            if (f[BLACK] > MINORITY) return BIT_BLACK
            if (f[CYAN] > MAJORITY) return BIT_CYAN
            if (f[YELLOW] > MAJORITY) return BIT_YELLOW
            if (f[PURPLE] > MAJORITY) return BIT_PURPLE

            // recognizable red-greens and red-blues (i.e. when both colors clearly exist)
            if (f[RED] + f[GREEN] > MINORITY && !noRedOrGreen) return BIT_REDGREEN
            if (f[RED] + f[BLUE] > MINORITY && !noRedOrBlue) return BIT_REDBLUE

            // yellows and purples that have blended to nearby blocks in far cases
            //if (yellow > half && noRedOrGreen) return BIT_YELLOW
            //if (purple > half && noRedOrBlue) return BIT_PURPLE
            //if (f[YELLOW] > EXISTENCE && (f[RED] > MAJORITY || f[GREEN] > MAJORITY)) return BIT_YELLOW
            //if (f[PURPLE] > EXISTENCE && (f[RED] > MAJORITY || f[BLUE] > MAJORITY)) return BIT_PURPLE

            // yellows and purples that tend to get dominated by white but are not actually white
            //if (f[YELLOW] > EXISTENCE && f[YELLOW] > (f[RED] + f[GREEN])) return BIT_YELLOW
            //if (f[PURPLE] > EXISTENCE && f[PURPLE] > (f[RED] + f[BLUE])) return BIT_PURPLE

            // otherwise decide based on the highest value
            when (frequencies.indexOf(frequencies.max()!!)) {
                BLACK -> return BIT_BLACK
                WHITE -> return BIT_CYAN // should be no whites, but white and cyan are the same bit
                RED -> return BIT_YELLOW // yellow has probably blended with adjacent purples
                YELLOW -> return BIT_YELLOW
                GREEN -> return BIT_REDGREEN // yellow has probably blended with adjacent cyans
                CYAN -> return BIT_CYAN
                BLUE -> return BIT_PURPLE // purple has probably blended with adjacent cyans
                PURPLE -> return BIT_PURPLE
            }

            // if all else fails, return black
            return BIT_BLACK
        }

    }

}

