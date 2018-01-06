package com.zhendi.OvalImageView.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.zhendi.OvalImageView.R;


/**
 * Created by zze on 28/12/17.
 */

@SuppressLint("AppCompatCustomView")
public class OvalImageView extends ImageView {
    //    private static final String TAG = OvalImageView.class.getSimpleName();
    public static final int NORMAL = 0;
    public static final int ROUND = 1;
    public static final int CIRCLE = 2;

    private int mDrawShape;

    private RectF mRectF = new RectF();

    //数据带把着圆角 左上角半径xy值 右上角半径xy值 右下角半径xy值 左下角半径xy值
    private float[] radius = new float[8];

    public OvalImageView(Context context) {
        super(context);
    }

    public OvalImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OvalImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(context, attrs);
    }

    private void initData(Context context, AttributeSet attrs) {
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


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mRectF.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {

        setPaint();

        Path path = null;
        if (mDrawShape == ROUND) {
            path = getRoundPath();
        } else if (mDrawShape == CIRCLE) {
            path = getCirclePath();
        }


        if (path != null) {
            canvas.clipPath(path);
        }

        super.onDraw(canvas);
    }

    private void setPaint() {
        if (getDrawable() == null)
            return;

        BitmapDrawable drawable;
        if (getDrawable() instanceof BitmapDrawable) {
            drawable = (BitmapDrawable) getDrawable();
        } else {
            return;
        }

        Paint mPaint = drawable.getPaint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setFilterBitmap(true);
    }

    private Path getRoundPath() {
        Path path = new Path();
        path.addRoundRect(mRectF, radius, Path.Direction.CW);
        return path;
    }

    private Path getCirclePath() {
        Path path = new Path();
        final float width = getMeasuredWidth();
        final float height = getMeasuredHeight();
        float radius = width > height ? height : width;
        path.addCircle(width / 2, height / 2, radius / 2, Path.Direction.CW);
        return path;
    }

}
