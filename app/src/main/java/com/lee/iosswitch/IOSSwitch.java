package com.lee.iosswitch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * User: Lee
 * Date: 2016-06-08
 * Time: 17:38
 * FIXME
 */
public class IOSSwitch extends View {
    //快关状态
    private SwitchState mState = SwitchState.SWITCH_STATE_CLOSE;
    //开关控件的宽高、圆弧半径、位置范围
    private float mWidth;
    private float mHeight;
    private float mRadius;
    private RectF mRectF;
    //控件默认的宽高（即wrap_content情况下的宽高）
    private static final float mDefaultWidth = 30;
    private static final float mDefaultHeight = 50;
    //开关内部的初始宽高、圆弧半径、位置范围
    private float mInnerWidth;
    private float mInnerHeight;
    private float mInnerRadius;
    private RectF mInnerRectF;
    //滑块的初始宽高、圆弧半径、位置范围
    private float mBlockWidth;
    private float mBlockHeight;
    private float mBlockRadius;
    private RectF mBlockRectF;
    //滑块能滑动的最大距离、当前滑动距离、滑动进度
    private float mMaxDistance;
    private float currentDistance;
    private float progress;
    //滑块的上下
    private float mBlockTop;
    private float mBlockBottom;
    private float mBlockLeft;
    private float mBlockRight;
    //内部宽高与组件的比例,innerScale = 0~mDefaultInnerScale;
    private static final float mDefaultInnerScale = 0.95f;
    private float innerScale = mDefaultInnerScale;
    //点击开关时，滑块伸长的比例(与控件的宽的比例)
    private float blockScale;
    private float mDefaultBlockScale = 0.2f;
    //IOS的UISwitch的默认宽高比
    private static final float mDefaultPercent = 5f / 3f;
    //相关颜色
    private static final int mBackgroundColor = 0xFFCCCCCC;
    private static final int mInnerColor = 0xFFEFEFEF;
    private int mCustomColor = 0xFFFF0000;
    private static final int mBlockColor = 0xFFFFFFFF;
    //画笔
    private Paint mPaint;
    //是否在按开关
    private boolean isPressed;

    //动画
    private AnimatorSet pressAnimatorSet;
    private AnimatorSet openAnimatorSet;
    private AnimatorSet closeAnimatorSet;

    public IOSSwitch(Context context) {
        this(context, null, 0);
    }

    public IOSSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IOSSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mRectF = new RectF();
        mInnerRectF = new RectF();
        mBlockRectF = new RectF();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthSpecMode == MeasureSpec.AT_MOST || heightMeasureSpec == MeasureSpec.AT_MOST) {
            mWidth = mDefaultWidth;
            mHeight = mDefaultHeight;
        } else {
            mWidth = getWidth();
            mHeight = getHeight();
        }
        initInch();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawSwitchBg(canvas);
        drawSwitchInner(canvas);
        drawSwichBlock(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.i("IOSSwitch", "ACTION_DOWN");
                initPressAnimation().start();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mState == SwitchState.SWITCH_STATE_CLOSE) {
                    initOpenAnimation().start();
                } else {
                    initCloseAnimation().start();
                }
                break;
            default:
                break;
        }
        return true;
    }

    private AnimatorSet initPressAnimation() {
        pressAnimatorSet = new AnimatorSet();
        ObjectAnimator blockScaleAnimator;
        ObjectAnimator innerScaleAnimator;
        if (mState == SwitchState.SWITCH_STATE_CLOSE) {
            innerScaleAnimator = ObjectAnimator.ofFloat(this, "innerScale", mDefaultInnerScale, 0f);
            blockScaleAnimator = ObjectAnimator.ofFloat(this, "blockScale", 0f, mDefaultBlockScale);
            pressAnimatorSet.playTogether(blockScaleAnimator, innerScaleAnimator);
        } else {
            blockScaleAnimator = ObjectAnimator.ofFloat(this, "blockScale", 0f, mDefaultBlockScale);
            pressAnimatorSet.playTogether(blockScaleAnimator);
        }
        return pressAnimatorSet;
    }

    private AnimatorSet initOpenAnimation() {
        openAnimatorSet = new AnimatorSet();
        openAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mState = SwitchState.SWITCH_STATE_OPEN;
            }
        });
        ObjectAnimator progressAnimator = ObjectAnimator.ofFloat(this, "progress", 0f, 1f);
        ObjectAnimator blockScaleAnimator = ObjectAnimator.ofFloat(this, "blockScale", blockScale, 0f);
        openAnimatorSet.playTogether(progressAnimator, blockScaleAnimator);
        openAnimatorSet.setDuration(300);
        return openAnimatorSet;
    }

    private AnimatorSet initCloseAnimation() {
        closeAnimatorSet = new AnimatorSet();
        closeAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationCancel(animation);
                mState = SwitchState.SWITCH_STATE_CLOSE;
            }
        });
        ObjectAnimator progressAnimator = ObjectAnimator.ofFloat(this, "progress", 1f, 0f);
        ObjectAnimator innerScaleAnimator = ObjectAnimator.ofFloat(this, "innerScale", 0f, mDefaultInnerScale);
        ObjectAnimator blockScaleAnimator = ObjectAnimator.ofFloat(this, "blockScale", blockScale, 0f);
        closeAnimatorSet.play(progressAnimator).with(blockScaleAnimator).with(innerScaleAnimator);
        return closeAnimatorSet;
    }

    /**
     * 绘制最外层背景
     *
     * @param canvas
     */
    private void drawSwitchBg(Canvas canvas) {
        mPaint.setColor(RGBColorTransform(mBackgroundColor, mCustomColor, progress));
        drawRoundRect(canvas, mRectF, 0f, 0f, mWidth, mHeight, mRadius, mPaint);
    }

    /**
     * 绘制开关内部
     *
     * @param canvas
     */
    private void drawSwitchInner(Canvas canvas) {
        mPaint.setColor(mInnerColor);
        drawRoundRect(canvas, mInnerRectF, (1f - innerScale) / 2f * mWidth, (1f - innerScale) / 2f * mHeight, (1f + innerScale) / 2f * mWidth, (1f + innerScale) / 2f * mHeight, innerScale / 2f * mHeight, mPaint);
    }

    /**
     * 绘制开关滑块
     *
     * @param canvas
     */
    private void drawSwichBlock(Canvas canvas) {
        mPaint.setColor(mBlockColor);
        if (mState == SwitchState.SWITCH_STATE_CLOSE) {
            drawRoundRect(canvas, mBlockRectF, mBlockLeft + mMaxDistance * progress, mBlockTop, mBlockRight + mWidth * blockScale + mMaxDistance * progress, mBlockBottom, mBlockRadius, mPaint);
        } else {
            drawRoundRect(canvas, mBlockRectF, mBlockLeft + mMaxDistance * progress - mWidth * blockScale, mBlockTop, mBlockRight + mMaxDistance * progress, mBlockBottom, mBlockRadius, mPaint);
        }
    }

    /**
     * 绘制圆角矩形
     */
    private void drawRoundRect(Canvas canvas, RectF tempRectF, float left, float top, float right, float bottom, float radius, Paint paint) {
        tempRectF.left = left;
        tempRectF.top = top;
        tempRectF.right = right;
        tempRectF.bottom = bottom;
        canvas.drawRoundRect(tempRectF, radius, radius, paint);
    }

    /**
     * 初始化各种尺寸
     */
    private void initInch() {
        checkViewInch();

        mRadius = mHeight / 2f;

        mInnerWidth = mWidth * mDefaultInnerScale;
        mInnerHeight = mHeight * mDefaultInnerScale;
        mInnerRadius = mInnerHeight / 2f;

        mBlockWidth = mInnerHeight;
        mBlockHeight = mInnerHeight;
        mBlockRadius = mBlockHeight / 2f;

        mBlockLeft = (1f - mDefaultInnerScale) / 2f * mWidth;
        mBlockTop = (1f - mDefaultInnerScale) / 2f * mHeight;
        mBlockRight = (1f + mDefaultInnerScale) / 2f * mHeight;
        mBlockBottom = (1f + mDefaultInnerScale) / 2f * mHeight;

        mMaxDistance = mInnerWidth - mBlockWidth;
    }

    /**
     * 检查控件的宽高比例的合法性,ios默认的宽高好像是5:3
     */
    private void checkViewInch() {
        if (mWidth / mHeight < mDefaultPercent) {
            mWidth = mHeight * 2f;
        }
    }

    /**
     * 计算颜色渐变过程中各个色值
     *
     * @param fromColor 起始颜色
     * @param toColor   最终颜色
     * @param progress  滑动的进度
     * @return 变化的色值
     */
    private int RGBColorTransform(int fromColor, int toColor, float progress) {
        int fromRed = (fromColor >> 16) & 0xff;
        int fromGreen = (fromColor >> 8) & 0xff;
        int fromBlue = fromColor & 0xff;

        int toRed = (toColor >> 16) & 0xff;
        int toGreen = (toColor >> 8) & 0xff;
        int toBlue = toColor & 0xff;

        int redGap = (int) ((float) (toRed - fromRed) * progress);
        int greenGap = (int) ((float) (toGreen - fromGreen) * progress);
        int blueGap = (int) ((float) (toBlue - fromBlue) * progress);

        return 0xff000000 | (fromRed + redGap) << 16 | (fromGreen + greenGap) << 8 | (fromBlue + blueGap);
    }

    private enum SwitchState {
        SWITCH_STATE_OPEN, SWITCH_STATE_CLOSE;
    }

    //getter and setter
    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public float getInnerScale() {
        return innerScale;
    }

    public void setInnerScale(float innerScale) {
        this.innerScale = innerScale;
        invalidate();
    }

    public float getBlockScale() {
        return blockScale;
    }

    public void setBlockScale(float blockScale) {
        this.blockScale = blockScale;
        invalidate();
    }
}