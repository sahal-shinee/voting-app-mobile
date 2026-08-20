package com.suarakita.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.suarakita.R;

import java.util.ArrayList;
import java.util.List;

// Ring/donut chart sederhana berbasis Canvas -- tanpa dependency charting library.
// Dipakai untuk preview kategori (1 segmen) maupun hasil penuh (multi-segmen per kandidat).
public class DonutChartView extends View {

    private static final int[] PALETTE = {
            R.color.chart_color_1,
            R.color.chart_color_2,
            R.color.chart_color_3,
            R.color.chart_color_4,
            R.color.chart_color_5,
    };

    public static int colorForIndex(Context context, int index) {
        int colorRes = PALETTE[index % PALETTE.length];
        return context.getResources().getColor(colorRes, context.getTheme());
    }

    public static class Segment {
        public final float percentage;
        public final int color;

        public Segment(float percentage, int color) {
            this.percentage = percentage;
            this.color = color;
        }
    }

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private List<Segment> segments = new ArrayList<>();
    private String centerText;
    private float strokeWidthPx;

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokeWidthPx = 14f * getResources().getDisplayMetrics().density;
        int trackColor = getResources().getColor(R.color.color_surface, getContext().getTheme());
        int textColor = getResources().getColor(R.color.color_text_primary, getContext().getTheme());

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidthPx);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setColor(trackColor);

        centerTextPaint.setColor(textColor);
        centerTextPaint.setTextAlign(Paint.Align.CENTER);
        centerTextPaint.setFakeBoldText(true);
    }

    public void setSegments(List<Segment> newSegments) {
        this.segments = newSegments != null ? newSegments : new ArrayList<>();
        invalidate();
    }

    public void setCenterText(String text) {
        this.centerText = text;
        invalidate();
    }

    public void setStrokeWidthDp(float dp) {
        strokeWidthPx = dp * getResources().getDisplayMetrics().density;
        arcPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setStrokeWidth(strokeWidthPx);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = getPaddingLeft();
        float top = getPaddingTop();
        float right = getWidth() - getPaddingRight();
        float bottom = getHeight() - getPaddingBottom();
        if (right <= left || bottom <= top) {
            return;
        }

        float size = Math.min(right - left, bottom - top);
        float radius = (size - strokeWidthPx) / 2f;
        float centerX = (left + right) / 2f;
        float centerY = (top + bottom) / 2f;

        arcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        float total = 0f;
        for (Segment segment : segments) {
            total += segment.percentage;
        }

        canvas.drawArc(arcBounds, 0, 360, false, trackPaint);

        if (!segments.isEmpty() && total > 0f) {
            float startAngle = -90f;
            for (Segment segment : segments) {
                float sweep = Math.min(Math.max(segment.percentage, 0f), 100f) / 100f * 360f;
                if (sweep <= 0f) {
                    continue;
                }
                arcPaint.setColor(segment.color);
                canvas.drawArc(arcBounds, startAngle, sweep, false, arcPaint);
                startAngle += sweep;
            }
        }

        if (centerText != null && !centerText.isEmpty() && radius > 0) {
            centerTextPaint.setTextSize(radius * 0.45f);
            float textY = centerY - ((centerTextPaint.descent() + centerTextPaint.ascent()) / 2f);
            canvas.drawText(centerText, centerX, textY, centerTextPaint);
        }
    }
}
