package fi.aalto.roopepalomaki.distanceawarebarcode

class StringUtils {
    companion object {
        @JvmStatic fun floatArrayToString(arr: FloatArray): String {
            return arr.joinToString()
        }
        @JvmStatic fun integerListToString(list: List<Int>): String {
            return list.joinToString()
        }

    }
}
