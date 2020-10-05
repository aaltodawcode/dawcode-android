package fi.aalto.roopepalomaki.distanceawarebarcode;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;

/**
 * @author: Shuai Zhu
 * @date: Sep. 2020
 */
public class ScanView extends View {

    private Rect mFrame;
    private Paint mLinePaint;
    private Paint mScanGridPaint;
    private Paint mScanPaint;

    private Path mPathLineBoundary;
    private Path mPathGrid;

    private LinearGradient mRadarLinearGradient;
    private LinearGradient mGridLinearGradient;

    private Matrix mGridMatrix;
    private ValueAnimator mValueAnimator;

    private int mGridColor;
    private float[] GRADIENT_POSITION = {0f, 0.85f, 0.98f, 1f};
    private float[] GRID_POSITION = {0, 0.5f, 0.99f, 1f};

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public ScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initScanView();
    }

    private void initScanView() {
        mScanGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScanGridPaint.setStyle(Paint.Style.STROKE);
        mScanGridPaint.setStrokeWidth(ConstantsKt.GRID_STROKE_WIDTH);

        mScanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScanPaint.setStyle(Paint.Style.FILL);

        mGridColor = ContextCompat.getColor(getContext(), R.color.scan_grid);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setStrokeWidth(ConstantsKt.BOUNDARY_STROKE_WIDTH);
        mLinePaint.setStyle(Paint.Style.STROKE);

        mGridMatrix = new Matrix();
        mGridMatrix.setTranslate(0, ConstantsKt.GRID_DENSITY);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int measuredHeight = this.getMeasuredHeight();
        int measuredWidth = this.getMeasuredWidth();
        int gridHeight =(int) ((measuredHeight + 0f) * ConstantsKt.GRID_SIZE);
        int gridWidth = (int) ((measuredWidth + 0f) * ConstantsKt.GRID_SIZE);

        int gridLen = gridHeight > gridWidth ? gridWidth : gridHeight;
        int gridLeft = (measuredWidth - gridLen) / 2;
        int gridTop = (measuredHeight - gridLen) / 2;

        mFrame = new Rect(gridLeft, gridTop,gridLeft + gridLen, gridTop + gridLen);
        initBoundaryAndAnimator();
    }

    private void initBoundaryAndAnimator() {
        if (mPathLineBoundary == null) {
            mPathLineBoundary = new Path();
            float cornerLineLen = mFrame.width() * ConstantsKt.RATIO_CORNER_LINE;
            float mFrameLeft = mFrame.left;
            float mFrameRight = mFrame.right;
            float mFrameTop = mFrame.top;
            float mFrameBottom = mFrame.bottom;
            addBoundary(ConstantsKt.LEFT_TOP, mPathLineBoundary, mFrameLeft, mFrameTop, cornerLineLen);
            addBoundary(ConstantsKt.RIGHT_TOP, mPathLineBoundary, mFrameRight, mFrameTop, cornerLineLen);
            addBoundary(ConstantsKt.RIGHT_BOTTOM, mPathLineBoundary, mFrameRight, mFrameBottom, cornerLineLen);
            addBoundary(ConstantsKt.LEFT_BOTTOM, mPathLineBoundary, mFrameLeft, mFrameBottom, cornerLineLen);
        }

        if (mValueAnimator == null) {
            initScanValueAnim(mFrame.height());
        }
    }

    private void addBoundary(int Position, Path targetPath, float x, float y, float cornerLineLen){
        switch(Position){
            case ConstantsKt.LEFT_TOP:
                targetPath.moveTo(x, y + cornerLineLen);
                targetPath.lineTo(x,y);
                targetPath.lineTo(x + cornerLineLen, y);
                break;
            case ConstantsKt.RIGHT_TOP:
                targetPath.moveTo(x - cornerLineLen, y);
                targetPath.lineTo(x, y);
                targetPath.lineTo(x, y + cornerLineLen);
                break;
            case ConstantsKt.LEFT_BOTTOM:
                targetPath.moveTo(x + cornerLineLen, y);
                targetPath.lineTo(x, y);
                targetPath.lineTo(x, y -cornerLineLen);
                break;
            case ConstantsKt.RIGHT_BOTTOM:
                targetPath.moveTo(x, y - cornerLineLen);
                targetPath.lineTo(x, y);
                targetPath.lineTo(x - cornerLineLen, y);
                break;
            default:
                break;
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (mFrame == null||mPathLineBoundary==null) {
            return;
        }
        canvas.drawPath(mPathLineBoundary, mLinePaint);
        initGridPathAndStyle();
        initRadarStyle();
        canvas.drawPath(mPathGrid, mScanGridPaint);
        canvas.drawRect(mFrame, mScanPaint);
    }

    private void initRadarStyle() {
        if (mRadarLinearGradient == null) {
            mRadarLinearGradient = new LinearGradient(0, mFrame.top,
                    0, mFrame.bottom + ConstantsKt.GRADIENT_BOTTOM * mFrame.height(),
                    new int[]{Color.TRANSPARENT, Color.TRANSPARENT, mGridColor, Color.TRANSPARENT},
                    GRADIENT_POSITION, LinearGradient.TileMode.CLAMP);
            mRadarLinearGradient.setLocalMatrix(mGridMatrix);
            mScanPaint.setShader(mRadarLinearGradient);
        }
    }

    private void initGridPathAndStyle() {
        if (mPathGrid == null) {
            mPathGrid = new Path();
            float width = mFrame.width() / (ConstantsKt.GRID_DENSITY + 0f);
            float height = mFrame.height() / (ConstantsKt.GRID_DENSITY + 0f);
            for (int i = 0; i <= ConstantsKt.GRID_DENSITY; i++) {
                mPathGrid.moveTo(mFrame.left + i * width, mFrame.top);
                mPathGrid.lineTo(mFrame.left + i * height, mFrame.bottom);
            }
            for (int i = 0; i <= ConstantsKt.GRID_DENSITY; i++) {
                mPathGrid.moveTo(mFrame.left, mFrame.top + i * height);
                mPathGrid.lineTo(mFrame.right, mFrame.top + i * height);
            }
        }
        if (mGridLinearGradient == null) {
            mGridLinearGradient = new LinearGradient(0, mFrame.top,
                    0, mFrame.bottom + ConstantsKt.GRADIENT_BOTTOM * mFrame.height(),
                    new int[]{Color.TRANSPARENT, Color.TRANSPARENT, mGridColor, Color.TRANSPARENT},
                    GRID_POSITION, LinearGradient.TileMode.CLAMP);
            mGridLinearGradient.setLocalMatrix(mGridMatrix);
            mScanGridPaint.setShader(mGridLinearGradient);

        }
    }

    public void initScanValueAnim(int height) {
        mValueAnimator = new ValueAnimator();
        mValueAnimator.setDuration(ConstantsKt.DURATION_ANIMATION);
        mValueAnimator.setFloatValues(-height, 0);
        mValueAnimator.setRepeatCount(Animation.INFINITE);
        mValueAnimator.setRepeatMode(ValueAnimator.RESTART);
        mValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mGridMatrix != null && mGridLinearGradient != null) {
                    float animatedValue = (float) animation.getAnimatedValue();
                    mGridMatrix.setTranslate(0, animatedValue);
                    mGridLinearGradient.setLocalMatrix(mGridMatrix);
                    mRadarLinearGradient.setLocalMatrix(mGridMatrix);
                    invalidate();
                }
            }
        });
        mValueAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mValueAnimator != null && mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }
}
