package com.baidu.ai.edge.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.baidu.ai.edge.ui.util.StringUtil;
import com.baidu.ai.edge.ui.view.model.BaseRectBoundResultModel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by ruanshimin on 2018/5/15.
 */

public class ResultMaskView extends View {
    private static final String TAG = "ResultMaskView";
    private float sizeRatio;
    private List<BaseRectBoundResultModel> mResultModelList;
    private Point originPt = new Point();
    private int imgWidth;
    private int imgHeight;

    private Handler handler;

    public void setHandler(Handler mHandler) {
        handler = mHandler;
    }

    public ResultMaskView(Context context) {
        super(context);
    }

    public ResultMaskView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ResultMaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }


    public void setRectListInfo(List<BaseRectBoundResultModel> modelList, int width, int height) {
        imgWidth = width;
        imgHeight = height;
        mResultModelList = modelList;
        handler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }


    public void clear() {
        mResultModelList = null;
        handler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    private void preCaculate() {
        float ratio = (float) getMeasuredWidth() / (float) getMeasuredHeight();
        float ratioBitmap = (float) imgWidth / (float) imgHeight;
        // | |#####| |模式
        if (ratioBitmap < ratio) {
            sizeRatio = (float) getMeasuredHeight() / (float) imgHeight;
            int x = (int) (getMeasuredWidth() - sizeRatio * imgWidth) / 2;
            originPt.set(x, 0);
        } else {
            // ------------
            //
            // ------------
            // ############
            // ------------
            //
            // ------------
            sizeRatio = (float) getMeasuredWidth() / (float) imgWidth;
            int y = (int) (getMeasuredHeight() - sizeRatio * imgHeight) / 2;
            originPt.set(0, y);
        }

    }

    private Map<Integer, Paint> paintRandomPool = new HashMap<>();

    private Paint getRandomMaskPaint(int index) {
        if (paintRandomPool.containsKey(index)) {

            return paintRandomPool.get(index);
        }

        int[] seed = new int[3];
        int offset = index % 3;
        seed[offset] = 255;

        Paint paint = new Paint();
        Random rnd = new Random();
        paint.setARGB(170,
                (rnd.nextInt(255) + seed[0]) / 2,
                (rnd.nextInt(255) + seed[1]) / 2,
                (rnd.nextInt(255) + seed[2]) / 2);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        paintRandomPool.put(index, paint);

        return paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 实时识别的时候第一次渲染
        if (mResultModelList == null) {
            super.onDraw(canvas);
            return;
        }

        preCaculate();

        // 绘制边框的宽度
        int stokeWidth = 2;

        int fontSize = 38;
        int labelPadding = 5;
        int labelHeight = 46 + 2 * labelPadding;
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(stokeWidth);
        paint.setStyle(Paint.Style.STROKE);

        Paint paintFillAlpha = new Paint();
        paintFillAlpha.setStyle(Paint.Style.FILL);
        paintFillAlpha.setColor(Color.parseColor("#3B85F5"));
        paintFillAlpha.setAlpha(50);

        /*Paint paintFill = new Paint();
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(Color.parseColor("#3B85F5"));*/

        Paint paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(fontSize);
        DecimalFormat df = new DecimalFormat("0.00");


        List<Float> points;
        for (int i = 0; i < mResultModelList.size(); i++) {


            BaseRectBoundResultModel model = mResultModelList.get(i);
            if (model.getName().equals("luomu")) {
                paint.setColor(Color.BLUE);
            } else {
                paint.setColor(Color.RED);
            }

            Rect rect = model.getBounds(sizeRatio, originPt);
            // 绘制框
            if (!model.isHasMask()) {
                canvas.drawRect(rect, paint);
                canvas.drawRect(rect, paintFillAlpha);
                /*canvas.drawRect(new Rect(rect.left, rect.top, rect.right, rect.top + labelHeight), paintFill);*/
            }
            Log.d(TAG, "onDraw: " + model.getName());
            canvas.drawText(
                    model.getName() + " " /*+ StringUtil.formatFloatString(model.getConfidence())*/,
                    rect.left + labelPadding,
                    rect.top + fontSize + labelPadding, paintText);


            // 绘制mask
            if (model.isHasMask()) {

                Paint paintMask = getRandomMaskPaint(model.getColorId());
                points = new ArrayList<>();
                byte[] maskData = model.getMask();

                for (int w = 0; w < imgWidth * sizeRatio; w++) {
                    for (int h = 0; h < imgHeight * sizeRatio; h++) {

                        int realX = (int) (w / sizeRatio);
                        int realY = (int) (h / sizeRatio);

                        int offset = imgWidth * realY + realX;
                        if (offset < maskData.length && maskData[offset] == 1) {
                            points.add(originPt.x + (float) w);
                            points.add(originPt.y + (float) h);
                        }
                    }
                }

                float[] ptft = new float[points.size()];
                for (int j = 0; j < points.size(); j++) {
                    ptft[j] = points.get(j);
                }
                canvas.drawPoints(ptft, paintMask);
            }
        }


        super.onDraw(canvas);
    }
}
