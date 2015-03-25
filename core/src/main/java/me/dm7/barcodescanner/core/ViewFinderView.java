package me.dm7.barcodescanner.core;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class ViewFinderView extends View {
    private static final String TAG = "ViewFinderView";

    private Rect mFramingRect;

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;

    private static final float LANDSCAPE_WIDTH_RATIO = 5f/8;
    private static final float LANDSCAPE_HEIGHT_RATIO = 5f/8;
    private static final int LANDSCAPE_MAX_FRAME_WIDTH = (int) (1920 * LANDSCAPE_WIDTH_RATIO); // = 5/8 * 1920
    private static final int LANDSCAPE_MAX_FRAME_HEIGHT = (int) (1080 * LANDSCAPE_HEIGHT_RATIO); // = 5/8 * 1080

    private static final float PORTRAIT_WIDTH_RATIO = 7f/8;
    private static final float PORTRAIT_HEIGHT_RATIO = 3f/8;
    private static final int PORTRAIT_MAX_FRAME_WIDTH = (int) (1080 * PORTRAIT_WIDTH_RATIO); // = 7/8 * 1080
    private static final int PORTRAIT_MAX_FRAME_HEIGHT = (int) (1920 * PORTRAIT_HEIGHT_RATIO); // = 3/8 * 1920

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private int scannerAlpha;
    private static final int POINT_SIZE = 10;
    private static final long ANIMATION_DELAY = 80l;

    public ViewFinderView(Context context) {
        super(context);
    }

    public ViewFinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setupViewFinder() {
        updateFramingRect();
        invalidate();
    }

    public Rect getFramingRect() {
        return mFramingRect;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(mFramingRect == null) {
            return;
        }

        drawViewFinderBorder(canvas);
    }

    public void drawViewFinderMask(Canvas canvas) {
        Paint paint = new Paint();
        Resources resources = getResources();
        paint.setColor(resources.getColor(R.color.viewfinder_mask));

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawRect(0, 0, width, mFramingRect.top, paint);
        canvas.drawRect(0, mFramingRect.top, mFramingRect.left, mFramingRect.bottom + 1, paint);
        canvas.drawRect(mFramingRect.right + 1, mFramingRect.top, width, mFramingRect.bottom + 1, paint);
        canvas.drawRect(0, mFramingRect.bottom + 1, width, height, paint);
    }

    public void drawViewFinderBorder(Canvas canvas) {
        Resources resources = getResources();
        Paint paint = preparePaint(resources);
        int lineLength = resources.getDimensionPixelSize(R.dimen.lineLength);

        Path path = new Path();

        path.moveTo(mFramingRect.left + lineLength, mFramingRect.top);
        path.lineTo(mFramingRect.left , mFramingRect.top );
        path.lineTo(mFramingRect.left, mFramingRect.top + lineLength);

        path.moveTo(mFramingRect.right - lineLength, mFramingRect.top);
        path.lineTo(mFramingRect.right , mFramingRect.top );
        path.lineTo(mFramingRect.right, mFramingRect.top + lineLength);

        path.moveTo(mFramingRect.left + lineLength, mFramingRect.bottom);
        path.lineTo(mFramingRect.left , mFramingRect.bottom );
        path.lineTo(mFramingRect.left, mFramingRect.bottom - lineLength);


        path.moveTo(mFramingRect.right - lineLength, mFramingRect.bottom);
        path.lineTo(mFramingRect.right , mFramingRect.bottom );
        path.lineTo(mFramingRect.right, mFramingRect.bottom - lineLength);

        canvas.drawPath(path, paint);
    }

    private Paint preparePaint(Resources resources) {
        Paint paint = new Paint();
        paint.setColor(resources.getColor(R.color.bg_blue));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.rectangleWidth));
        paint.setPathEffect(new CornerPathEffect(resources.getDimensionPixelSize(R.dimen.rectangleWidth)) );
        paint.setDither(true);                    // set the dither to true
        paint.setStyle(Paint.Style.STROKE);       // set to STOKE
        paint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        paint.setStrokeCap(Paint.Cap.ROUND);
        return paint;
    }

    public void drawLaser(Canvas canvas) {
        Paint paint = new Paint();
        Resources resources = getResources();
        // Draw a red "laser scanner" line through the middle to show decoding is active
        paint.setColor(resources.getColor(R.color.viewfinder_laser));
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        paint.setStyle(Paint.Style.FILL);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        int middle = mFramingRect.height() / 2 + mFramingRect.top;
        canvas.drawRect(mFramingRect.left + 2, middle - 1, mFramingRect.right - 1, middle + 2, paint);

        postInvalidateDelayed(ANIMATION_DELAY,
                mFramingRect.left - POINT_SIZE,
                mFramingRect.top - POINT_SIZE,
                mFramingRect.right + POINT_SIZE,
                mFramingRect.bottom + POINT_SIZE);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    public synchronized void updateFramingRect() {
        Point viewResolution = new Point(getWidth(), getHeight());
        if (viewResolution == null) {
            return;
        }
        int width;
        int height;
        int orientation = DisplayUtils.getScreenOrientation(getContext());

        if(orientation != Configuration.ORIENTATION_PORTRAIT) {
            width = findDesiredDimensionInRange(LANDSCAPE_WIDTH_RATIO, viewResolution.x, MIN_FRAME_WIDTH, LANDSCAPE_MAX_FRAME_WIDTH);
            height = findDesiredDimensionInRange(LANDSCAPE_HEIGHT_RATIO, viewResolution.y, MIN_FRAME_HEIGHT, LANDSCAPE_MAX_FRAME_HEIGHT);
        } else {
            width = findDesiredDimensionInRange(PORTRAIT_WIDTH_RATIO, viewResolution.x, MIN_FRAME_WIDTH, PORTRAIT_MAX_FRAME_WIDTH);
            height = width;//findDesiredDimensionInRange(PORTRAIT_HEIGHT_RATIO, viewResolution.y, MIN_FRAME_HEIGHT, PORTRAIT_MAX_FRAME_HEIGHT);
        }

        int leftOffset = (viewResolution.x - width) / 2;
        int topOffset = (viewResolution.y - height) / 2;
        mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }

    private static int findDesiredDimensionInRange(float ratio, int resolution, int hardMin, int hardMax) {
        int dim = (int) (ratio * resolution);
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

}
