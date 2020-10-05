package fi.aalto.roopepalomaki.distanceawarebarcode

// bits per code block (1 for dither code)
const val BITS = 1

// width and height should be divisible by multiplier+1
const val CODE_WIDTH = 24
const val CODE_HEIGHT = 24
const val LOW_DEFINITION_SIZE_MULTIPLIER = 4

// leave black from both sides of a module
const val SEPARATOR_OFFSET_PC = 0.15f

// neighbors in each direction for averaging recognized color (10 for 20by20, 7 for 30by30)
const val NEIGHBORS_X = 9
const val NEIGHBORS_Y = 9

// subchannel flags within the additional third channel to determine
// whether even or odd values are used when decoding
const val CH3_NEAR = 0 // 0b00000000
const val CH3_FAR = 16 // 0b00010000

// color indices
const val BLACK = 0
const val RED = 1
const val GREEN = 2
const val BLUE = 3
const val ORANGE = 4
const val PURPLE = 5
const val PINK = 6
const val LIGHTBLUE = 7

// for order shuffling to redistribute data
const val SEED = 1234

const val DISTANCE_THRESHOLD = 600

// quantization
const val QUANTIZED_BLACK = 0
const val QUANTIZED_WHITE = 1
const val QUANTIZED_COLOR = 2
const val QUANTIZED_GRAY = 3

const val NUMBER_OF_HUES = 6
const val NUMBER_OF_LIGHTNESS_LEVELS = 16
const val ROTATE_HUES_BY_STEPS = 0
const val ROTATE_HUES_BY_DEGREES = 0.0 // double
const val UNIQUE_CODE_VALUES_ONEBIT = 4


/*
    DITHER CODE
 */

const val SIZE_OF_SIZE = 1 // bytes
const val SIZE_OF_CRC = 4

const val BIT_BLACK = 0
const val BIT_WHITE = 1
const val BIT_CYAN = 1
const val BIT_REDGREEN = 1
const val BIT_REDBLUE = 0
const val BIT_YELLOW = 0
const val BIT_PURPLE = 1

const val CHOICE_00 = 0 // black
const val CHOICE_01 = 3 // red blue | purple
const val CHOICE_10 = 2 // red green | yellow
const val CHOICE_11 = 1 // white

/**
 * ScanView
 * **/
const val LEFT_TOP = 0
const val RIGHT_TOP = 1
const val LEFT_BOTTOM = 2
const val RIGHT_BOTTOM = 3
const val RATIO_CORNER_LINE = 0.08f
const val DURATION_ANIMATION = 2000
const val GRADIENT_BOTTOM = 0.02f
const val BOUNDARY_STROKE_WIDTH = 8f
const val GRID_STROKE_WIDTH = 2f
const val GRID_DENSITY = 30
const val GRID_SIZE = 0.85f