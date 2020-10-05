package fi.aalto.roopepalomaki.distanceawarebarcode;

import android.os.Environment;
import android.util.Log;

import com.google.common.primitives.Ints;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import mikera.vectorz.Vector2;

import static fi.aalto.roopepalomaki.distanceawarebarcode.BarcodeOneBit.getDitherBlockMidpoints;
import static fi.aalto.roopepalomaki.distanceawarebarcode.ConstantsKt.NEIGHBORS_X;
import static fi.aalto.roopepalomaki.distanceawarebarcode.ConstantsKt.NEIGHBORS_Y;
import static fi.aalto.roopepalomaki.distanceawarebarcode.ConstantsKt.NUMBER_OF_HUES;
import static fi.aalto.roopepalomaki.distanceawarebarcode.ConstantsKt.QUANTIZED_BLACK;
import static fi.aalto.roopepalomaki.distanceawarebarcode.ConstantsKt.QUANTIZED_COLOR;
import static fi.aalto.roopepalomaki.distanceawarebarcode.ConstantsKt.QUANTIZED_WHITE;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;
import static org.opencv.imgproc.Imgproc.COLOR_HSV2BGR;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_LIST;
import static org.opencv.imgproc.Imgproc.bilateralFilter;
import static org.opencv.imgproc.Imgproc.blur;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.putText;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.warpPerspective;

public class CVOneBit {

    static {
        System.loadLibrary("coder");
    }

    public static native byte[] cppDecodeRS255onebit(int[] ints);

    public static DecodeResult decode(String filename) {

        String TAG = "CVOneBit";

        long startTime = System.nanoTime();

        Log.d(TAG, "filename: " + filename);

        Mat src = imread(filename);

        /*
        Crop unnecessary top and bottom assuming code is in the middle
         */
        src = src.submat(new Rect(0, src.height() / 2 - src.width() / 2, src.width(), src.width()));

        /*
        Threshold for corners
         */

        Mat hsv = new Mat();

        cvtColor(src, hsv, COLOR_BGR2HSV);

        Mat thresh = new Mat();

        Core.inRange(hsv, new Scalar(50, 60, 60, 0), new Scalar(90, 255, 255, 0), thresh);

        int erosion_size = 2;
        Mat elem = getStructuringElement(MORPH_RECT, new Size(2*erosion_size+1, 2*erosion_size+1), new Point(erosion_size, erosion_size));
        erode(thresh, thresh, elem);
        //erode(thresh, thresh, elem);

        //int dilation_size = 2;
        //elem = getStructuringElement(MORPH_RECT, new Size(2*dilation_size+1, 2*dilation_size+1), new Point(dilation_size, dilation_size));
        //dilate(thresh, thresh, elem);

        imwrite(filename + ".thresh.png", thresh);

        hsv.release();

        /*
        Find corners
         */

        long image_area = thresh.cols() * thresh.rows();

        List<MatOfPoint> contours = new ArrayList<>();
        findContours(thresh, contours, new Mat(), RETR_LIST, CHAIN_APPROX_SIMPLE);

        thresh.release();

        Mat src_with_rects = src.clone();

        List<Rect> rects = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            double area = contourArea(contour);
            //Log.d(TAG, "contour area: " + area);
            double area_pc = area / image_area;
            // discard very small contours
            // Note: cannot check for squareness because from far away the corners are not squares
            if (area >= 5.0) { // absolute limit gives more distance with more probable false corners
                Rect rect = boundingRect(contour);
                rectangle(src_with_rects, rect, new Scalar(255, 0, 0, 0), 1, 8, 0);
                rects.add(rect);
            }
        }

        Collections.sort(rects, new Comparator<Rect>() {
            public int compare(Rect r1, Rect r2) {
                int result  = Integer.compare(r1.x+r1.width, r2.x+r2.width);
                if (result == 0) return -1;
                return result;
            }
        });

        List<Rect> left_corners = rects.subList(0, 2);
        List<Rect> right_corners = rects.subList(rects.size()-2, rects.size());

        Collections.sort(left_corners, new Comparator<Rect>() {
            public int compare(Rect r1, Rect r2) {
                return Integer.compare(r1.y+r1.height, r2.y+r2.height);
            }
        });

        Collections.sort(right_corners, new Comparator<Rect>() {
            public int compare(Rect r1, Rect r2) {
                return Integer.compare(r1.y+r1.height, r2.y+r2.height);
            }
        });

        Rect tl_corner = left_corners.get(0);
        Rect bl_corner = left_corners.get(1);
        Rect tr_corner = right_corners.get(0);
        Rect br_corner = right_corners.get(1);

        Point tl = new Point(tl_corner.x + (tl_corner.width/2), tl_corner.y + (tl_corner.height/2));
        Point tr = new Point(tr_corner.x + (tr_corner.width/2), tr_corner.y + (tr_corner.height/2));
        Point br = new Point(br_corner.x + (br_corner.width/2), br_corner.y + (br_corner.height/2));
        Point bl = new Point(bl_corner.x + (bl_corner.width/2), bl_corner.y + (bl_corner.height/2));

        //Scalar red = new Scalar(0, 0, 255, 0);
        //line(src_with_rects, tl, tr, red, 1, 8, 0);
        //line(src_with_rects, tr, br, red, 1, 8, 0);
        //line(src_with_rects, br, bl, red, 1, 8, 0);
        //line(src_with_rects, bl, tl, red, 1, 8, 0);

        //imwrite(filename + ".rects.jpg", src_with_rects);

        src_with_rects.release();

        long cornerTime = System.nanoTime();

        /*
        Perspective transform
         */

        int warped_size = 1000;

        List<Point> recognized_corners = new ArrayList<>();
        recognized_corners.add(tl);
        recognized_corners.add(tr);
        recognized_corners.add(br);
        recognized_corners.add(bl);

        List<Point> target_corners = new ArrayList<>();
        target_corners.add(new Point(0, 0));
        target_corners.add(new Point(warped_size, 0));
        target_corners.add(new Point(warped_size, warped_size));
        target_corners.add(new Point(0, warped_size));

        Mat recognized = Converters.vector_Point2f_to_Mat(recognized_corners);
        Mat target = Converters.vector_Point2f_to_Mat(target_corners);
        Mat M = getPerspectiveTransform(recognized, target);
        Mat warped = new Mat();
        warpPerspective(src, warped, M, new Size(warped_size, warped_size), INTER_LINEAR);

        //imwrite(filename + ".warp.jpg", warped);

        src.release();

        long perspectiveTime = System.nanoTime();

        /*
        Smooth for reading
         */

        /*
        Mat warpedSmoothed = new Mat();

        blur(warped, warpedSmoothed, new Size(3, 3), new Point(-1,-1), Core.BORDER_DEFAULT);
        //bilateralFilter(warped, warpedSmoothed, 7, 150, 150);

        imwrite(filename + ".smooth.jpg", warpedSmoothed);

        warped = warpedSmoothed.clone();

        warpedSmoothed.release();
        //*/

        /*
        Find midpoints
         */

        // redundant warning because the following 2 lines after this are commented
        Vector2 alongTopLeftToTopRight = new Vector2(warped_size, 0);
        Vector2 alongTopLeftToBottomLeft = new Vector2(0, warped_size);

        //line(src, topLeft, new Point(topLeft.x()+ ((int) alongTopLeftToTopRight.x), topLeft.y()+ ((int) alongTopLeftToTopRight.y)), new Scalar(0, 0, 0, 0), 3, 8, 0);
        //line(src, topLeft, new Point(topLeft.x()+ ((int) alongTopLeftToBottomLeft.x), topLeft.y()+ ((int) alongTopLeftToBottomLeft.y)), new Scalar(0, 0, 0, 0), 3, 8, 0);

        Float[][] percentageMidpoints = getDitherBlockMidpoints();

        List<Point> midpoints = new ArrayList<>();
        for (int i = 0; i < percentageMidpoints.length; i++) {
            Float[] percentageMidpoint = percentageMidpoints[i];

            alongTopLeftToTopRight = new Vector2(warped_size, 0);
            alongTopLeftToBottomLeft = new Vector2(0, warped_size);

            alongTopLeftToTopRight.multiply(percentageMidpoint[0]); // along x "axis" until midpoint
            alongTopLeftToBottomLeft.multiply(percentageMidpoint[1]); // along y

            alongTopLeftToTopRight.add(alongTopLeftToBottomLeft);

            // NOTE: alongTopLeftToTopRight now contains the vector to the midpoint from topLeft

            double midpointX = Math.round(alongTopLeftToTopRight.x);
            double midpointY = Math.round(alongTopLeftToTopRight.y);

            midpoints.add(new Point((int)midpointX, (int)midpointY));
        }

        //for (Point p : midpoints) {
        //    rectangle(warped, new Point(p.x - NEIGHBORS_X, p.y - NEIGHBORS_Y), new Point(p.x + NEIGHBORS_X, p.y + NEIGHBORS_Y), new Scalar(0, 0, 0, 0), 1, 8, 0);
        //}

        //imwrite(filename + ".midpoints.jpg", warped);

        /*
        Get color values from midpoint neighborhood
         */

        Mat warpedHsv = new Mat();
        cvtColor(warped, warpedHsv, COLOR_BGR2HSV);
        warped.release();

        List<Integer> bits = new ArrayList<>();

        double[] hueThresholds = ColorUtilsOneBit.getHueThresholds();

        for (Point p : midpoints) {
            List<int[]> blockColors = new ArrayList<>();
            for (int x = -NEIGHBORS_X; x < NEIGHBORS_X + 1; x++) {
                for (int y = -NEIGHBORS_Y; y < NEIGHBORS_Y + 1; y++) {
                    double[] pixelHsv = warpedHsv.get((int)p.y + y, (int)p.x + x);

                    int[] q = ColorUtilsOneBit.quantizeHsv(pixelHsv, hueThresholds);

                    // for visual inspection
                    if (q[0] == QUANTIZED_BLACK) {
                        pixelHsv[1] = 0;
                        pixelHsv[2] = 0;
                    } else if (q[0] == QUANTIZED_WHITE) {
                        pixelHsv[1] = 0;
                        pixelHsv[2] = 255;
                    } else if (q[0] == QUANTIZED_COLOR){
                        pixelHsv[0] = q[1] * (180 / (NUMBER_OF_HUES));
                        pixelHsv[1] = 255;
                        pixelHsv[2] = 200;
                    }
                    warpedHsv.put((int)p.y + y, (int)p.x + x, pixelHsv);

                    blockColors.add(q);
                }
            }

            int bit = ColorUtilsOneBit.getBitFromHistogram(
                    ColorUtilsOneBit.getFrequencyPercentages(blockColors),
                    ColorUtilsOneBit.centerIsBlack(blockColors)
            );
            bits.add(bit);

            // overlay interpreted bit
            putText(warpedHsv, Integer.toString(bit), p, 0, 1, new Scalar(0, 255, 0));
        }

        warped = new Mat();

        cvtColor(warpedHsv, warped, COLOR_HSV2BGR);

        warpedHsv.release();

        imwrite(filename + ".quantized.png", warped);

        long bitsTime = System.nanoTime();

        //System.out.print("BITS CVOneBit  ");
        //for (int b : bits) System.out.print(b + " ");
        //System.out.println();

        /*
        Cleanup
         */

        warped.release();

        /*
        Decode
         */

        byte[] decoded = cppDecodeRS255onebit(Ints.toArray(bits));

        long decodedTime = System.nanoTime();

        /*
        Write bits to file
         */
        File logf2 = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "bits_log.txt"
        );
        if (!logf2.exists()) {
            try {
                logf2.createNewFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        Date now = Calendar.getInstance().getTime();
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String nowStr = filename + sdf.format(now);
        try {
            String str = nowStr + ", " + Boolean.toString(decoded[0] == 0) + ", " + decoded[1] + ", " +
                    StringUtils.integerListToString(bits) +
                    System.getProperty("line.separator");
            Files.write(logf2.toPath(), str.getBytes(), StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        Return result
         */

        Log.d(TAG, "Finding corners took " + Long.toString(cornerTime-startTime));
        Log.d(TAG, "Perspective transform took " + Long.toString(perspectiveTime-cornerTime));
        Log.d(TAG, "Bits took " + Long.toString(bitsTime-perspectiveTime));
        Log.d(TAG, "Decoding took " + Long.toString(decodedTime-bitsTime));
        Log.d(TAG, "In total " + Long.toString(System.nanoTime()-startTime));

        return new DecodeResult(
                Arrays.copyOfRange(decoded, 2, decoded.length),
                decoded[0] == 0,
                decoded[1]
        );

    }

}
