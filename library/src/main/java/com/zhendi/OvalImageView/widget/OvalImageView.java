package com.zhendi.OvalImageView.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

import com.zhendi.OvalImageView.R;


/**
 * Created by zze on 28/12/17.
 */

@SuppressLint("AppCompatCustomView")
public class OvalImageView extends ImageView {
    private static final String TAG = OvalImageView.class.getSimpleName();
    public static final int NORMAL = 0;
    public static final int ROUND = 1;
    public static final int CIRCLE = 2;

    private int mDrawShape;

    private RectF mRectF = new RectF();
    RectF srcRect = new RectF();

    private Paint paint;
    private Xfermode xfermode;
    private Bitmap markBitmap;
    private float oldW, oldH;

    //数据带把着圆角 左上角半径xy值 右上角半径xy值 右下角半径xy值 左下角半径xy值
    private float[] radius = new float[8];

    private int mBorderColor = 0xFF0080FF;
    private Paint mPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mBorderWidth;

    public OvalImageView(Context context) {
        super(context);
        initData();
    }

    public OvalImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OvalImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initData();
    }

    private void initData() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OvalImageView);
        mDrawShape = a.getInteger(R.styleable.OvalImageView_drawShape, NORMAL);

        if (mDrawShape == ROUND) {
            float radius = a.getDimension(R.styleable.OvalImageView_roundRadius, 0);
            if (radius > 0) {
                setRadius(radius);
            } else {
                float roundLeftTopRadius = a.getDimension(R.styleable.OvalImageView_roundLeftTopRadius, 0f);
                float roundRightTopRadius = a.getDimension(R.styleable.OvalImageView_roundRightTopRadius, 0f);
                float roundLeftBottomRadius = a.getDimension(R.styleable.OvalImageView_roundLeftBottomRadius, 0f);
                float roundRightBottomRadius = a.getDimension(R.styleable.OvalImageView_roundRightBottomRadius, 0f);
                setRadius(roundLeftTopRadius, roundRightTopRadius, roundLeftBottomRadius, roundRightBottomRadius);
            }
        }

        mBorderWidth = a.getDimension(R.styleable.OvalImageView_borderWidth, 0f);
        mBorderColor = a.getColor(R.styleable.OvalImageView_borderColor, Color.TRANSPARENT);
        a.recycle();
    }

    public void setRadius(float radius) {
        setRadius(radius, radius, radius, radius);
    }

    public void setRadius(float roundLeftTopRadius, float roundRightTopRadius,
                          float roundLeftBottomRadius, float roundRightBottomRadius) {
        //左上角半径xy值
        radius[0] = radius[1] = roundLeftTopRadius;
        //右上角半径xy值
        radius[2] = radius[3] = roundRightTopRadius;
        //右下角半径xy值
        radius[4] = radius[5] = roundRightBottomRadius;
        //左下角半径xy值
        radius[6] = radius[7] = roundLeftBottomRadius;

        invalidate();
    }

    public void setDrawShape(int drawShape) {
        mDrawShape = drawShape;
        invalidate();
    }

    public void setBorderWidth(float width) {
        mBorderWidth = dip2px(width);
        invalidate();
    }

    public void setBorderColor(@ColorInt int color) {
        mBorderWidth = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mRectF.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {

        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        if (drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
            return;
        }

        Matrix matrix = getImageMatrix();
        if (matrix == null && getPaddingTop() == 0 && getPaddingBottom() == 0) {
            drawable.draw(canvas);
        } else {
            if (mDrawShape == NORMAL) {
                super.onDraw(canvas);
            } else {
                Path path = null;
                if (mDrawShape == CIRCLE) {
                    path = getCirclePath(mRectF);
                } else if (mDrawShape == ROUND) {
                    path = getRoundPath(mRectF);
                }

                drawBorderBackground(canvas, path);

                //制作需要显示的目标图形
                Bitmap bitmapContent = drawableToBitmap(drawable, matrix);
                srcRect.set(0, 0, bitmapContent.getWidth(), bitmapContent.getHeight());
                //制作框架原型图
                createMarkBitmap(path, mRectF);
                //绘制圆角图形
                RectF rectFSrc = new RectF(srcRect);
                RectF rectFMark = new RectF(mRectF);
                if (mBorderWidth > 0) {
                    adjustRectF(rectFSrc);
                    adjustRectF(rectFMark);
                }
                final int saveCount = canvas.saveLayer(rectFSrc, paint, Canvas.ALL_SAVE_FLAG);
                setCanvasMatrix(canvas, mRectF);
                canvas.drawBitmap(markBitmap, null, rectFMark, paint);
                paint.setXfermode(xfermode);
                canvas.drawBitmap(bitmapContent, null, rectFSrc, paint);
                paint.setXfermode(null);
                canvas.restoreToCount(saveCount);
            }

        }
    }

    private Path getRoundPath(RectF rectF) {
        Path path = new Path();
        path.addRoundRect(rectF, radius, Path.Direction.CW);
        return path;
    }

    private Path getCirclePath(RectF rectF) {
        Path path = new Path();
        final float width = rectF.width();
        final float height = rectF.height();
        float radius = width > height ? height : width;
        path.addCircle(width / 2, height / 2, radius / 2, Path.Direction.CW);
        return path;
    }

    private Bitmap drawableToBitmap(Drawable drawable, Matrix matrix) {
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), config);
        Canvas canvas = new Canvas(bitmap);

        if (getCropToPadding()) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.clipRect(scrollX + getPaddingLeft(), scrollY + getPaddingTop(),
                    scrollX + getRight() - getLeft() - getPaddingRight(),
                    scrollY + getBottom() - getTop() - getPaddingBottom());
        }

        canvas.translate(getPaddingLeft(), getPaddingTop());

        if (matrix != null) {
            canvas.concat(matrix);
        }

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void createMarkBitmap(Path path, RectF rectF) {
        if (oldW != rectF.width() || oldH != rectF.height()) {
            oldW = rectF.width();
            oldH = rectF.height();
            markBitmap = Bitmap.createBitmap((int) rectF.width(), (int) rectF.height(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(markBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        canvas.drawPath(path, paint);
    }

    private void drawBorderBackground(Canvas canvas, Path path) {
        if (mBorderWidth > 0) {
            int saveCount = canvas.getSaveCount();
            canvas.save();
            mPaintBorder.setColor(mBorderColor);
            canvas.drawPath(path, mPaintBorder);
            canvas.restoreToCount(saveCount);
        }
    }

    private void setCanvasMatrix(Canvas canvas, RectF rectF) {
        if (mBorderWidth > 0) {
            Matrix matrix = new Matrix();
            float scaleWidth = (rectF.width() - mBorderWidth * 2) / rectF.width();
            float scaleHeight = (rectF.height() - mBorderWidth * 2) / rectF.height();
            matrix.setScale(scaleWidth, scaleHeight);
            canvas.setMatrix(matrix);
        }
    }

    private float dip2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void adjustRectF(RectF rectF) {
        rectF.set(rectF.left + mBorderWidth,
                rectF.top + mBorderWidth,
                rectF.right - mBorderWidth,
                rectF.bottom - mBorderWidth);
    }

}
