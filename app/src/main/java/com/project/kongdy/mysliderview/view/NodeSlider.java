package com.project.kongdy.mysliderview.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


import com.project.kongdy.mysliderview.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kongdy
 *         on 2016/8/15
 *         <p/>
 *         带有吸附节点的滑动器
 *         <p/>
 *         吸附算法尝试
 *         <p/>
 *         这个与另外一个滑动器重复，刚开始的时候未考虑到做一个公共框架
 *         <p/>
 *         <h1>
 *         后期有时间的话，完善公共框架
 *         </h1>
 */
public class NodeSlider extends View {

    /**
     * 外圈画笔
     */
    private Paint outPaint;
    /**
     * 内圈画笔
     */
    private Paint inPaint;
    /**
     * 内圈未到达画笔
     */
    private Paint inNoReachPaint;
    /**
     * 底部标注画笔
     */
    private TextPaint labelPaint;

    private int nodeSize;
    private Drawable slider;

    private Rect sliderRect;
    private RectF showRect;

    private boolean showLabel;

    private int sliderHeight;

    private int mWidth;
    private int mHeight;
    private float sliderTrack;

    private List<String> labels;

    private boolean sliderOnDrag = false;
    private boolean onFling = false;

    private final static long DEFAULT_FLING_TIME = 300;

    /**
     * 到达节点
     */
    private int reachCount;
    /**
     * 到达率
     */
    private float reachRate;

    public NodeSlider(Context context) {
        super(context);
        init(null);
    }

    public NodeSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public NodeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NodeSlider(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void openHighEffect(Paint paint) {
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setFilterBitmap(true);
        paint.setSubpixelText(true);
    }


    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.NodeSlider);
        nodeSize = a.getInteger(R.styleable.NodeSlider_ns_node_count, -1);
        slider = a.getDrawable(R.styleable.NodeSlider_ns_sliderBlock);
        int outColor = a.getColor(R.styleable.NodeSlider_ns_out_color, Color.GRAY);
        int inColor = a.getColor(R.styleable.NodeSlider_ns_in_reach_color, Color.GREEN);
        int labelColor = a.getColor(R.styleable.NodeSlider_ns_sliderLabelColor, Color.BLACK);
        int noReachColor = a.getColor(R.styleable.NodeSlider_ns_inNoReachColor, Color.DKGRAY);
        showLabel = a.getBoolean(R.styleable.NodeSlider_ns_show_slider_label, true);
        float labelSize = a.getDimension(R.styleable.NodeSlider_ns_sliderLabelTextSize, 12f);
        a.recycle();

        outPaint = new Paint();
        inPaint = new Paint();
        labelPaint = new TextPaint();
        inNoReachPaint = new Paint();

        sliderRect = new Rect();
        showRect = new RectF();

        labels = new ArrayList<>();

        openHighEffect(outPaint);
        openHighEffect(inPaint);
        openHighEffect(labelPaint);
        openHighEffect(inNoReachPaint);

        outPaint.setColor(outColor);
        inPaint.setColor(inColor);
        labelPaint.setColor(labelColor);
        labelPaint.setTextSize(labelSize);
        inNoReachPaint.setColor(noReachColor);

        roundPaint(outPaint);
        roundPaint(inPaint);
        roundPaint(inNoReachPaint);

        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void roundPaint(Paint paint) {
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                sliderOnDrag = catchCursor(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (sliderOnDrag && !onFling) {
                    final float x = event.getX();
                    reachRate = (x - showRect.left) / sliderTrack;
                    if (reachRate < 0) {
                        reachRate = 0;
                    } else if (reachRate > 1) {
                        reachRate = 1;
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                sliderOnDrag = false;
                fixOffset();
                break;
        }
        return true;
    }

    private void fixCount() {
        reachCount = (int) (reachRate*(nodeSize -1));
    }

    // 吸附
    private void fixOffset() {
        if(onFling) {
            return;
        }
        onFling = true;
        /**
         * 吸附运算方式:
         * e.g:
         *
         * space = 100;
         * left point = 100;
         * right point = 200;
         *
         * x = 161;
         * calc process:
         *
         * 161+50 = 211
         * 211/100 = 2
         * 2x100=200
         *
         * x = 149
         * calc process:
         *
         * 149+50 = 199
         * 199/100 = 1
         * 1x100 = 100
         *
         * 为了保证运算方式的结果，
         * 以int形式进行计算,运算
         * 结果出来之后再转换为rate
         */
        int rateSpace = (int) ((1f/ (nodeSize-1))*1000f);
        int orgRate = (int) (reachRate*1000f);
        int absorbRate = ((orgRate+rateSpace/2)/rateSpace)*rateSpace;// 吸附公式
        float absorbRateF = absorbRate/1000f;
        ValueAnimator animator = ValueAnimator.ofFloat(reachRate,absorbRateF);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                reachRate = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.setDuration(DEFAULT_FLING_TIME);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                onFling = false;
            }
            @Override
            public void onAnimationCancel(Animator animation) {
                onFling = false;
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.saveLayer(0, 0, getMeasuredWidth(), getMeasuredHeight(), outPaint, Canvas.ALL_SAVE_FLAG);

        canvas.drawLine(showRect.left, showRect.top + sliderHeight / 2, showRect.right, showRect.top + sliderHeight / 2, outPaint);
        canvas.drawLine(showRect.left, showRect.top + sliderHeight / 2, showRect.right, showRect.top + sliderHeight / 2, inNoReachPaint);

        drawNode(canvas);

        // slider的绘制放在最后
        drawSlider(canvas);
        canvas.restore();
    }

    private void drawSlider(Canvas canvas) {
        float xOffset = reachRate * sliderTrack + showRect.left - sliderHeight/2f;
        final int sliderWidth = sliderRect.width();
        float radius = inPaint.getStrokeWidth();
        sliderRect.left = (int) xOffset;
        sliderRect.right = sliderRect.left + sliderWidth;
        // drawOver line
        canvas.drawLine(showRect.left,showRect.top + sliderHeight / 2f,sliderRect.right-sliderWidth/2f,
                showRect.top + sliderHeight / 2f,inPaint);
        fixCount();
        for (int i = 0;i <= reachCount;i++) {
            canvas.drawCircle(showRect.left + i * showRect.width() / (nodeSize - 1) - radius / 2,
                    showRect.top + sliderHeight / 2,radius,inPaint);
        }
        slider.setBounds(sliderRect);
        slider.draw(canvas);
    }

    private void drawNode(Canvas canvas) {
        float radius = inNoReachPaint.getStrokeWidth();
        for (int i = 0; i < labels.size(); i++) {
            if (showLabel) {
                canvas.drawText(labels.get(i), showRect.left + i * showRect.width() / (nodeSize - 1), showRect.bottom, labelPaint);
            }
            canvas.drawCircle(showRect.left + i * showRect.width() / (nodeSize - 1) - radius / 2,
                    showRect.top + sliderHeight / 2, radius, inNoReachPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w - getPaddingLeft() - getPaddingRight();
        mHeight = h - getPaddingTop() - getPaddingBottom();

        initProperty();

        showRect.top = getPaddingTop();

        showRect.bottom = h - getPaddingBottom();

        sliderHeight = mHeight;
        if (showLabel)
            sliderHeight = (int) (showRect.bottom - labelPaint.getFontSpacing());

        outPaint.setStrokeWidth(sliderHeight);
        inPaint.setStrokeWidth(sliderHeight / 5f);
        inNoReachPaint.setStrokeWidth(sliderHeight / 5f);

        showRect.left = getPaddingLeft() + sliderRect.width() / 2f + sliderHeight / 2f;
        showRect.right = w - getPaddingRight() - sliderRect.width() / 2f - sliderHeight / 2f;

        if (labels.size() <= 0) {
            for (int i = 0; i < nodeSize; i++) {
                labels.add(String.valueOf(i + 1));
            }
        } else if (labels.size() < nodeSize) {
            for (int i = labels.size() - 1; i < nodeSize; i++) {
                labels.add(String.valueOf(i + 1));
            }
        } else if (labels.size() > nodeSize) {
            for (int i = labels.size() - 1; i > nodeSize - 1; i++) {
                labels.remove(i);
            }
        }

        sliderTrack = showRect.width();
    }

    private void initProperty() {
        if (slider == null) {
            sliderRect.left = 0;
            sliderRect.top = 0;
            sliderRect.right = mWidth / 15f > mHeight ? mHeight : (int) (mWidth / 15f);
            sliderRect.bottom = sliderRect.width();

            Paint tempPaint = new Paint();
            tempPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            tempPaint.setColor(Color.GREEN); // 默认绿色
            openHighEffect(tempPaint);
            Bitmap result = Bitmap.createBitmap(sliderRect.width(), sliderRect.height(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            canvas.drawRect(sliderRect, tempPaint);
            slider = new BitmapDrawable(getContext().getResources(), result);
        } else {
            sliderRect.left = 0;
            sliderRect.top = 0;
            sliderRect.right = slider.getIntrinsicWidth();
            sliderRect.bottom = slider.getIntrinsicHeight();
        }
    }

    /**
     * 触摸点是否在滑块之内
     *
     * @param event
     * @return
     */
    private boolean catchCursor(MotionEvent event) {
        Rect rect = new Rect(sliderRect.left, sliderRect.top, sliderRect.right, sliderRect.bottom);
        return rect.contains((int) event.getX(), (int) event.getY());
    }
}
