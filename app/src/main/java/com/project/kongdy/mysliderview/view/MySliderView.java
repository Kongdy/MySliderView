package com.project.kongdy.mysliderview.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.project.kongdy.mysliderview.R;


/**
 * @author kongdy
 *         on 2016/8/8
 *         滑块条
 */
public class MySliderView extends View {

    private int mWidth;
    private int mHeight;

    private int cursorHeight;
    private int cursorWidth;

    private int cursorLeft;
    private int cursorRight;
    private int cursorTop;
    private int cursorBottom;

    private Paint sliderPaint;

    private Drawable sliderCursor;

    /**
     * 当前进度，0.0~100.0
     */
    private float value;
    /**
     * 当前进度，0.0~1.0
     */
    private float progressValue;

    private boolean cursorOnDrag = false;

    private MySliderListener mySliderListener;

    private Handler sliderHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if (msg.what == 1) {
                if (mySliderListener != null) {
                    mySliderListener.onValueChanged(value);
                }
            }
        }
    };

    public MySliderView(Context context) {
        super(context);
        init(null);
    }

    public MySliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MySliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MySliderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MySliderView);

        sliderCursor = a.getDrawable(R.styleable.MySliderView_slider_cursor);

        a.recycle();

        cursorWidth = sliderCursor.getIntrinsicWidth();
        cursorHeight = sliderCursor.getIntrinsicHeight();

        sliderPaint = new Paint();


        openHighEffect(sliderPaint);
        sliderPaint.setStrokeJoin(Paint.Join.ROUND);
        sliderPaint.setStrokeCap(Paint.Cap.ROUND);
        sliderPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                cursorOnDrag = catchCursor(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (cursorOnDrag) {
                    final float x = event.getX();
                    progressValue = (x-getPaddingLeft()-cursorWidth/2)
                            / (mWidth);
                    if (progressValue > 1) {
                        progressValue = 1;
                    } else if (progressValue < 0) {
                        progressValue = 0;
                    }
                    float oldValue = value;
                    value = progressValue * 100;
                    if (Math.abs(value - oldValue) > 0.1 || value == 100 || value == 0) {
                        sliderHandler.sendEmptyMessage(1);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                cursorOnDrag = false;
                break;
        }
        requestLayout();
        postInvalidate();
        return true;
    }


    private void openHighEffect(Paint paint) {
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setFilterBitmap(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(cursorHeight,MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        cursorLeft = (int) (getPaddingLeft() + progressValue * mWidth) - cursorWidth / 2;
        cursorRight = cursorLeft + cursorWidth;

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w - getPaddingLeft() - getPaddingRight();
        mHeight = h - getPaddingTop() - getPaddingBottom();
        mHeight = cursorHeight;

        cursorTop = mHeight / 2 - cursorHeight / 2;
        cursorBottom = cursorTop + cursorHeight;

        sliderPaint.setStrokeWidth(mHeight/4);
        sliderPaint.setShader(new LinearGradient(getPaddingLeft(), cursorTop, getPaddingLeft() + mWidth,
                cursorBottom, new int[]{
                Color.rgb(203, 252, 186), Color.rgb(107, 188, 85), Color.rgb(83, 165, 57)}, null, Shader.TileMode.MIRROR));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.saveLayer(0, 0, getMeasuredWidth(), getMeasuredHeight(), sliderPaint, Canvas.ALL_SAVE_FLAG);
        sliderCursor.setBounds(cursorLeft, cursorTop, cursorRight, cursorBottom);
        canvas.drawLine(sliderPaint.getStrokeWidth()+getPaddingLeft(), mHeight / 2, getPaddingLeft() + mWidth
                -sliderPaint.getStrokeWidth(), mHeight / 2, sliderPaint);
        sliderCursor.draw(canvas);
        canvas.restore();
    }

    /**
     * 触摸点是否在滑块之内
     *
     * @param event
     * @return
     */
    private boolean catchCursor(MotionEvent event) {
        Rect rect = new Rect(cursorLeft, cursorTop, cursorRight, cursorBottom);
        return rect.contains((int) event.getX(), (int) event.getY());
    }

    /**
     * 设置回调接口
     *
     * @param mySliderListener
     */
    public void setMySilderListener(MySliderListener mySliderListener) {
        this.mySliderListener = mySliderListener;
    }

    /**
     * 手动设置当前进度
     *
     * @param value
     */
    public void setValue(float value) {
        this.value = value;
        progressValue = value / 100f;
        sliderHandler.sendEmptyMessage(1);
        postInvalidate();
    }

    public interface MySliderListener {
        void onValueChanged(float value);
    }
}
