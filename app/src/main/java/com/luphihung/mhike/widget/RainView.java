package com.luphihung.mhike.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Draws gently falling rain over whatever sits behind this view.
 * Each drop is a short slanted streak with its own speed, length and
 * transparency so the shower looks natural rather than mechanical.
 */
public class RainView extends View {

    private static final int DROP_COUNT = 110;
    /** Horizontal drift as a fraction of drop length (wind blowing slightly left). */
    private static final float SLANT = 0.18f;

    private final Paint dropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    private float[] dropX;
    private float[] dropY;
    private float[] dropLength;
    private float[] dropSpeed;
    private int[] dropAlpha;
    private long lastFrameTime;

    public RainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dropPaint.setColor(Color.WHITE);
        dropPaint.setStrokeWidth(3f);
        dropPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        dropX = new float[DROP_COUNT];
        dropY = new float[DROP_COUNT];
        dropLength = new float[DROP_COUNT];
        dropSpeed = new float[DROP_COUNT];
        dropAlpha = new int[DROP_COUNT];
        for (int i = 0; i < DROP_COUNT; i++) {
            resetDrop(i, true);
        }
    }

    /** Places a drop at a random position; new drops start above the screen. */
    private void resetDrop(int i, boolean anywhereOnScreen) {
        dropX[i] = random.nextFloat() * getWidth();
        dropY[i] = anywhereOnScreen
                ? random.nextFloat() * getHeight()
                : -random.nextFloat() * getHeight() * 0.2f;
        dropLength[i] = 24 + random.nextFloat() * 36;
        dropSpeed[i] = 900 + random.nextFloat() * 700;
        dropAlpha[i] = 50 + random.nextInt(90);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dropX == null) {
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds = lastFrameTime == 0
                ? 0 : Math.min((now - lastFrameTime) / 1_000_000_000f, 0.05f);
        lastFrameTime = now;

        for (int i = 0; i < DROP_COUNT; i++) {
            dropY[i] += dropSpeed[i] * deltaSeconds;
            dropX[i] -= dropSpeed[i] * deltaSeconds * SLANT;
            if (dropY[i] > getHeight()) {
                resetDrop(i, false);
            }
            dropPaint.setAlpha(dropAlpha[i]);
            float slant = dropLength[i] * SLANT;
            canvas.drawLine(dropX[i] + slant, dropY[i] - dropLength[i],
                    dropX[i], dropY[i], dropPaint);
        }

        if (isShown()) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            lastFrameTime = 0;
            postInvalidateOnAnimation();
        }
    }
}
