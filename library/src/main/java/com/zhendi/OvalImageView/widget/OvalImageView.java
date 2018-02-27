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
import android.util.Log;
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
    //绘制的形状
    private int mDrawShape;
    //目标 bitmap 矩形，即目标形状的 bitmap
    private RectF mDstRectF = new RectF();
    //原图 bitmap 矩形，即 drawable 转换来的 bitmap
    private RectF mSrcRect = new RectF();
    //合成目标bitmap用的 paint
    private Paint mSrcPaint;
    // Xfermode 的过渡模式使用 scr_in
    private Xfermode xfermode;
    //目标形状的 bitmap
    private Bitmap mDstBitmap;
    //记录当前的正在使用 mDstRectF 的宽高，若宽高改变就刷新 mDstBitmap 的大小
    private float oldWidth, oldHeight;

    //数据带把着圆角 左上角半径xy值 右上角半径xy值 右下角半径xy值 左下角半径xy值
    private float[] radius = new float[8];
    //边框的颜色
    private int mBorderColor = 0x00000000;
    //绘制边框最终图形所需用的 paint
    private Paint mPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    //边框的宽度
    private float mBorderWidth;
    //Xfermode 的过渡模式使用 SRC_OVER
    private Xfermode xfermodeBorder;

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
        mSrcPaint = new Paint();
        mSrcPaint.setAntiAlias(true);
        mSrcPaint.setDither(true);
        xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        xfermodeBorder = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
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
        mBorderColor = a.getColor(R.styleable.OvalImageView_borderColor, Color.WHITE);
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

    /**
     * 设置绘制的形状
     */
    public void setDrawShape(int drawShape) {
        mDrawShape = drawShape;
        invalidate();
    }

    /**
     * 设置边框宽度
     */
    public void setBorderWidth(float width) {
        mBorderWidth = dip2px(width);
        invalidate();
    }

    /**
     * 设置边框颜色
     */
    public void setBorderColor(@ColorInt int color) {
        mBorderWidth = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mDstRectF.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
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
                    path = buildCirclePath(mDstRectF);
                } else if (mDrawShape == ROUND) {
                    path = buildRoundPath(mDstRectF);
                }

                //创建原图 bitmap
                Bitmap srcBitmap = drawableToBitmap(drawable, matrix);
                mSrcRect.set(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
                //创建目标形状 bitmap
                createMarkBitmap(path, mDstRectF);
                //创建目标形状原图 bitmap
                Log.i(TAG, "mDstRectF width:" + mDstRectF.width() + "/height:" + mDstRectF.height());
                Bitmap targetBitmap = createTargetBitmap(mDstBitmap, srcBitmap, mDstRectF, mSrcRect);
                RectF targetRectF = new RectF();
                targetRectF.set(0, 0, targetBitmap.getWidth(), targetBitmap.getHeight());
                //判断是否有设置边框，若有则调整 target 的绘制区域的矩形
                if (mBorderWidth > 0) {
                    adjustRectF(targetRectF);
                }
                //创建边框 bitmap
                Bitmap borderBitmap = createBorderBackground(mDstRectF, path);
                final int saveCount = canvas.saveLayer(mDstRectF, mSrcPaint, Canvas.ALL_SAVE_FLAG);
                if (mBorderWidth > 0) {
                    canvas.drawBitmap(borderBitmap, null, mDstRectF, mSrcPaint);
                }
                mSrcPaint.setXfermode(xfermodeBorder);
                canvas.drawBitmap(targetBitmap, null, targetRectF, mSrcPaint);
                mSrcPaint.setXfermode(null);
                canvas.restoreToCount(saveCount);

            }

        }
    }

    //构造圆角矩形路径
    private Path buildRoundPath(RectF rectF) {
        Path path = new Path();
        path.addRoundRect(rectF, radius, Path.Direction.CW);
        return path;
    }

    //构造圆形路径
    private Path buildCirclePath(RectF rectF) {
        Path path = new Path();
        final float width = rectF.width();
        final float height = rectF.height();
        float radius = width > height ? height : width;
        path.addCircle(width / 2, height / 2, radius / 2, Path.Direction.CW);
        return path;
    }

    //构造圆角矩形路径
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

    //drawable 转换为 bitmap
    private void createMarkBitmap(Path path, RectF rectF) {
        if (oldWidth != rectF.width() || oldHeight != rectF.height()) {
            oldWidth = rectF.width();
            oldHeight = rectF.height();
            mDstBitmap = Bitmap.createBitmap((int) rectF.width(), (int) rectF.height(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(mDstBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        canvas.drawPath(path, paint);
    }

    //创建边框 bitmap
    private Bitmap createBorderBackground(RectF rectF, Path path) {
        Bitmap bitmap = null;
        if (mBorderWidth > 0) {
            bitmap = Bitmap.createBitmap((int) rectF.width(), (int) rectF.height(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            mPaintBorder.setColor(mBorderColor);
            canvas.drawPath(path, mPaintBorder);
        }
        return bitmap;
    }

    //合成目标形状的圆形图
    private Bitmap createTargetBitmap(Bitmap dstBitmap, Bitmap srcBitmap, RectF dstRectF, RectF srcRectF) {
        mSrcRect.set(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
        //绘制圆角图形
        Bitmap bitmap = Bitmap.createBitmap((int) dstRectF.width(), (int) dstRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        adjustCanvasMatrix(canvas, dstRectF);
        canvas.drawBitmap(dstBitmap, null, dstRectF, mSrcPaint);
        mSrcPaint.setXfermode(xfermode);
        canvas.drawBitmap(srcBitmap, null, srcRectF, mSrcPaint);
        mSrcPaint.setXfermode(null);
        return bitmap;
    }

    //设置边框后调整矩阵的缩放系数
    private void adjustCanvasMatrix(Canvas canvas, RectF rectF) {
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

    //当设置边框后调整 矩形的绘制坐标
    private void adjustRectF(RectF rectF) {
        rectF.set(rectF.left + mBorderWidth,
                rectF.top + mBorderWidth,
                rectF.right + mBorderWidth,
                rectF.bottom + mBorderWidth);
    }

}
