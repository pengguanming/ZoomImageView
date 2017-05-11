package com.example.ngfngf.zoomimageview.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by ngfngf on 2017/4/10.
 */

public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {
    private boolean mOnce;//初始化
    private float mInitScale;//初始化缩放的值
    private float mMidScale;//双击放大到达的值
    private float mMaxScale;//放大的最大值
    private Matrix mMatrix;
    private ScaleGestureDetector mScaleGestureDetector;
    //-------------------------------------自由移动
    private int mLastPointerCount;//记录上一次多点触控的缩放的比例
    private float mLastX, mLastY;
    private int mTouchSlop;//移动临界值
    private boolean isCanDrag;//是否能移动
    private boolean isCheckLeftAndRifgt;
    private boolean isCheckTopAndBottom;
    //--------------------------------双击缩放
    private GestureDetector mGestureDetector;
    private boolean isAutoScale;//是否在缩放


    private static final String TAG = "JJY";

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        setOnTouchListener(this);
        // mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTouchSlop = 300;
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isAutoScale) {
                    return true;
                }
                float x = e.getX();
                float y = e.getY();
                if (getScale() < mMidScale) {
                  /*  mMatrix.postScale(mMidScale / getScale(), mMidScale / getScale(), x, y);
                    setImageMatrix(mMatrix);*/
                    postDelayed(new AutoScaleRunnable(mMidScale, x, y), 16);
                    isAutoScale = true;
                } else {
                    /*mMatrix.postScale(mInitScale / getScale(), mInitScale / getScale(), x, y);
                    setImageMatrix(mMatrix);*/
                    postDelayed(new AutoScaleRunnable(mInitScale, x, y), 16);
                    isAutoScale = true;
                }
                return true;
            }
        });
        Log.d(TAG, "SystemTouchSlop: " + ViewConfiguration.get(context).getScaledTouchSlop());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow: ");
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onAttachedToWindow: ");
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    //监听图片加载玩完成,设置合适的图片大小
    @Override
    public void onGlobalLayout() {
        if (!mOnce) {
            //获取控件的宽高
            int width = getWidth();
            int height = getHeight();
            //获取图片的宽和高
            Drawable drawable = getDrawable();
            if (drawable == null) {
                //没有图片则返回
                return;
            }
            int drawableIntrinsicHeight = drawable.getIntrinsicHeight();
            int drawableIntrinsicWidth = drawable.getIntrinsicWidth();
            float scale = 1.0f;//缩放比
            if (drawableIntrinsicWidth > width && drawableIntrinsicHeight < height) {
                scale = width * 1.0f / drawableIntrinsicWidth;
            }
            if (drawableIntrinsicHeight > height && drawableIntrinsicWidth < width) {
                scale = height * 1.0f / drawableIntrinsicHeight;
            }
            if ((drawableIntrinsicWidth > width && drawableIntrinsicHeight > height) || (drawableIntrinsicWidth < width && drawableIntrinsicHeight < height)) {
                scale = Math.min(width * 1.0f / drawableIntrinsicWidth, height * 1.0f / drawableIntrinsicHeight);
            }
            //得到初始化时的缩放比
            mInitScale = scale;
            mMaxScale = mInitScale * 4;
            mMidScale = mInitScale * 2;

            //将图片移动到控件的中心
            int dx = getWidth() / 2 - drawableIntrinsicWidth / 2;
            int dy = getHeight() / 2 - drawableIntrinsicHeight / 2;

            mMatrix.postTranslate(dx, dy);
            mMatrix.postScale(mInitScale, mInitScale, getWidth() / 2, getHeight() / 2);
            checkBorderAndCenterWhenTranslate();
            checkBorderAndCenterWhenScale();
            setImageMatrix(mMatrix);
            mOnce = true;
            Log.d(TAG, "mMaxScale:" + mMaxScale + ";mInitScale:" + mInitScale + ";w:" + width + ";h:" + height + ";scale:" + scale + ";dw:" + drawableIntrinsicWidth + ";dh:" + drawableIntrinsicHeight);
        }
    }

    //获取当前图片的缩放值
    public float getScale() {
        float[] values = new float[9];
        mMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    //缩放空间：mInitScale~mMaxScale
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();
        if (getDrawable() == null) {
            return true;
        }
        //缩放范围的控制:当前缩放值小于最大缩放值想放大 || 当前缩放值大于最小缩放值想缩小
        if ((scale < mMaxScale && scaleFactor > 1.0f) || (scale > mInitScale && scaleFactor < 1.0f)) {
            //最小值控制
            if (scale * scaleFactor < mInitScale) {
                scaleFactor = mInitScale / scale;
            }
            //最大值控制
            if (scale * scaleFactor > mMaxScale) {
                //scale = mMaxScale / scale;
                scaleFactor = mMaxScale / scale;
            }
            //缩放,获取detector的中心点
            mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            //位置检查，防止出现白边
            checkBorderAndCenterWhenScale();
            setImageMatrix(mMatrix);
        }
        return true;
    }

    //在缩放时进行边界控制、位置控制
    private void checkBorderAndCenterWhenScale() {
        RectF rectF = getMatrixRectF();
        float detalX = 0;
        float detalY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectF.width() >= width) {
            if (rectF.left > 0) {
                detalX = -rectF.left;
            }
            if (rectF.right < width) {
                detalX = width - rectF.right;
            }
        }
        //图片高度小于屏幕高度很正常，因此只有大于等于屏幕高度时才处理白边
        if (rectF.height() >= height) {
            if (rectF.top > 0) {
                detalY = -rectF.top;
            }
            if (rectF.bottom < height) {
                detalY = height - rectF.bottom;
            }
        }
        //如果宽或高大小于控件的宽和高；则让器居中
        if (rectF.width() < width) {
            detalX = width * 1.0f / 2f - rectF.right + rectF.width() * 1.0f / 2f;
        }
        if (rectF.height() < height) {
            detalY = height * 1.0f / 2f - rectF.bottom + rectF.height() * 1.0f / 2f;
        }
        mMatrix.postTranslate(detalX, detalY);
    }

    //获取图片缩放后的矩阵
    private RectF getMatrixRectF() {
        Matrix matrix = mMatrix;
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        mScaleGestureDetector.onTouchEvent(event);
        float x = 0;
        float y = 0;
        //拿到多点触控的数量
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            isCanDrag = false;
            x += event.getX(i);
            y += event.getY(i);
        }
        x /= pointerCount;
        y /= pointerCount;

        if (mLastPointerCount != pointerCount) {
            mLastPointerCount = pointerCount;
            mLastX = x;
            mLastY = y;
            isCanDrag = false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //将viewpager放行自控将的Touch拦截，加0.01是为了浮点数操作将值缩小或者放大一点点
                if (getMatrixRectF().width() > getWidth() + 0.01 || getMatrixRectF().height() > getHeight() + 0.01) {
                    if (getParent() instanceof ViewPager) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //将viewpager放行自控将的Touch拦截，加0.01是为了浮点数操作将值缩小或者放大一点点
                if (getMatrixRectF().width() > getWidth() + 0.01 || getMatrixRectF().height() > getHeight() + 0.01) {
                    if (getParent() instanceof ViewPager) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                mLastPointerCount = pointerCount;
                float dx = x - mLastX;
                float dy = y - mLastY;
                if (!isCanDrag) {
                    isCanDrag = isActionMove(dx, dy);
                }
                if (isCanDrag) {
                    RectF rectF = getMatrixRectF();
                    if (getDrawable() != null) {
                        isCheckLeftAndRifgt = true;
                        isCheckTopAndBottom = true;
                        //如果移动后的矩阵宽度小于控件的宽度，不允许横向移动
                        if (rectF.width() < getWidth()) {
                            isCheckLeftAndRifgt = false;
                            dx = 0;
                        }
                        //如果移动后的矩阵高度度小于控件的高度，不允许竖向移动
                        if (rectF.height() < getHeight()) {
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }
                        mMatrix.postTranslate(dx / 5, dy / 5);
                        checkBorderAndCenterWhenTranslate();
                        setImageMatrix(mMatrix);
                    }
                }
                mLastY = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount = 0;
                break;
        }

        return true;
    }

    //移动时，检查位置，防止出现白边
    private void checkBorderAndCenterWhenTranslate() {
        RectF rectF = getMatrixRectF();
        float detalX = 0;
        float detalY = 0;

        int width = getWidth();
        int heigh = getHeight();

        if (rectF.top > 0 && isCheckTopAndBottom) {
            detalY = -rectF.top;
        }
        if (rectF.bottom < heigh && isCheckTopAndBottom) {
            detalY = heigh - rectF.bottom;
        }

        if (rectF.left > 0 && isCheckLeftAndRifgt) {
            detalX = -rectF.left;
        }

        if (rectF.right < width && isCheckLeftAndRifgt) {
            detalX = width - rectF.right;
        }

        mMatrix.postTranslate(detalX, detalY);

    }

    //判断是否移动
    private boolean isActionMove(float dx, float dy) {
        Log.d(TAG, "detal: " + Math.sqrt(dx * dx + dy * dy) + ";mTouchSlop:" + mTouchSlop + ";isCanDrag:" + isCanDrag);
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }

    //自动缓慢缩放
    private class AutoScaleRunnable implements Runnable {
        //缩放的目标值
        private float mTagetScale;
        //缩放的中心坐标
        private float x;
        private float y;
        //缩放的梯度
        private final float BIGGER = 1.07f;
        private final float SMALLER = 0.93f;

        private float temScale;

        public AutoScaleRunnable(float tagetScale, float x, float y) {
            mTagetScale = tagetScale;
            this.x = x;
            this.y = y;

            if (getScale() < mTagetScale) {
                temScale = BIGGER;
            }
            if (getScale() > mTagetScale) {
                temScale = SMALLER;
            }
        }

        @Override
        public void run() {
            //进行缩放
            mMatrix.postScale(temScale, temScale, x, y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mMatrix);
            float currentScale = getScale();
            if ((temScale > 1.0f && currentScale < mTagetScale) || (temScale < 1.0f && currentScale > mTagetScale)) {
                postDelayed(this, 16);
            } else {//设置为我们的目标值
                float scale = mTagetScale / currentScale;
                mMatrix.postScale(scale, scale, x, y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mMatrix);
                isAutoScale = false;
            }
        }
    }
}
