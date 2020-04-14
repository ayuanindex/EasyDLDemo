package com.realmax.easydldemo.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.ddk.DDKConfig;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.detect.DetectInterface;
import com.baidu.ai.edge.core.detect.DetectionResultModel;
import com.baidu.ai.edge.core.infer.InferConfig;
import com.baidu.ai.edge.core.infer.InferInterface;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.segment.SegmentInterface;
import com.baidu.ai.edge.core.snpe.SnpeConfig;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import com.baidu.ai.edge.core.util.FileUtil;
import com.baidu.ai.edge.core.util.Util;
import com.baidu.ai.edge.ui.activity.ResultListener;
import com.baidu.ai.edge.ui.layout.PreviewFrameLayout;
import com.baidu.ai.edge.ui.util.ThreadPoolManager;
import com.baidu.ai.edge.ui.view.PreviewDecoratorView;
import com.baidu.ai.edge.ui.view.ResultMaskView;
import com.baidu.ai.edge.ui.view.model.BaseRectBoundResultModel;
import com.baidu.ai.edge.ui.view.model.DetectResultModel;
import com.google.gson.Gson;
import com.realmax.easydldemo.R;
import com.realmax.easydldemo.bean.ConfigBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int MODEL_DETECT = 2;
    private ImageView iv_image;

    // 请替换为您的序列号
    private static final String SERIAL_NUM = "CB4A-C78C-50AA-FC2E";
    private Handler uiHandler;
    /**
     * 配置信息的JavaBean
     */
    private ConfigBean configBean;
    private String currentSoc = "";
    private boolean autoTakeFlag = true;
    private List<BaseRectBoundResultModel> detectResultModelCache;
    private boolean isOnline = false;
    public static final int TYPE_INFER = 0;
    public static final int TYPE_DDK150 = 1;
    public static final int TYPE_DDK200 = 11;
    public static final int TYPE_SNPE = 2;
    private int platform;
    InferInterface mInferManager;
    DetectInterface mOnlineDetect;
    private boolean isInitializing = false;
    private boolean modelLoadStatus = false;
    private ResultMaskView realtime_result_mask;
    private ArrayList<Bitmap> bitmaps;
    private int i;
    private boolean isStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        init();
    }

    private void initView() {
        realtime_result_mask = (ResultMaskView) findViewById(R.id.realtime_result_mask);
        iv_image = (ImageView) findViewById(R.id.iv_image);
    }

    private void init() {
        uiHandler = new Handler(getMainLooper());
        new Thread() {
            @Override
            public void run() {
                super.run();
                bitmaps = new ArrayList<>();
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_40));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_42));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_43));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_48));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_50));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_58));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_61));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_69));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_80));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_99));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_105));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_119));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_132__1_));
                bitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.pic_luosi_luomu));

                // 初始化读取config.json配置
                /*initConfig();*/

                // 验证芯片类型是否支持
                if (checkChip()) {
                    realtime_result_mask.setHandler(uiHandler);
                    realtime_result_mask.clear();

                    /*MainActivity.this.start();*/
                    // 选择设备类型
                    choosePlatform();
                    detect();
                }
            }
        }.start();
    }

    private void detect() {
        i = 0;
        ThreadPoolManager.createAutoFocusTimerTask(new Runnable() {
            @Override
            public void run() {
                if (isStart)
                    realtime_result_mask.clear();
                Bitmap bitmap = bitmaps.get(i);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iv_image.setImageBitmap(bitmap);
                    }
                });

                setPictureProcess(bitmap);
                i++;
                if (i >= bitmaps.size()) {
                    i = 0;
                }
            }
        });
    }

    /**
     * 初始化配置信息
     */
    private void initConfig() {
        try {
            String configJson = FileUtil.readAssetFileUtf8String(getAssets(), "demo/config.json");
            configBean = new Gson().fromJson(configJson, ConfigBean.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 验证新品类型是否支持
     *
     * @return
     */
    private boolean checkChip() {
        if (configBean.getSoc().contains("dsp") && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            currentSoc = "dsp";
            return true;
        } else if (configBean.getSoc().contains("npu") && (Build.HARDWARE.contains("kirin970") || Build.HARDWARE.contains("kirin980"))) {
            if (Build.HARDWARE.contains("kirin970")) {
                currentSoc = "npu150";
            }
            if (Build.HARDWARE.contains("kirin980")) {
                currentSoc = "npu200";
            }
            return true;
        } else if (configBean.getSoc().contains("arm")) {
            currentSoc = "arm";
            return true;
        }
        return false;
    }

    private void start() {

        // paddleLite需要保证初始化与预测在同一线程保证速度
        ThreadPoolManager.executeSingle(new Runnable() {
            @Override
            public void run() {
                initManager();
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ((configBean.getModel_type() == MODEL_DETECT && mInferManager != null)) {
                            modelLoadStatus = true;
                        }
                    }
                });*/
            }
        });
    }

    private void choosePlatform() {
        switch (currentSoc) {
            case "dsp":
                platform = TYPE_SNPE;
                break;
            case "npu150":
                platform = TYPE_DDK150;
                break;
            case "npu200":
                platform = TYPE_DDK200;
                break;
            default:
            case "arm":
                platform = TYPE_INFER;
        }
    }

    private void initManager() {
        try {
            if (configBean.getModel_type() == MODEL_DETECT) {
                switch (platform) {
                    case TYPE_DDK200:
                        DDKConfig mDetectConfig = new DDKConfig(getAssets(), "ddk-detect/config.json");
                        mInferManager = new DDKManager(this, mDetectConfig, SERIAL_NUM);
                        break;
                    case TYPE_SNPE:
                        SnpeConfig mSnpeClassifyConfig = new SnpeConfig(this.getAssets(), "snpe-detect/config.json");
                        mInferManager = new SnpeManager(this, mSnpeClassifyConfig, SERIAL_NUM);
                        break;
                    case TYPE_INFER:
                    default:
                        InferConfig mInferConfig = new InferConfig(getAssets(), "infer-detect/config.json");
                        // 可修改ARM推断使用的CPU核数
                        mInferConfig.setThread(Util.getInferCores());
                        mInferManager = new InferManager(this, mInferConfig, SERIAL_NUM);
                        break;
                }
            }
        } catch (BaseException e) {
            e.printStackTrace();
        }
    }


    private void setPictureProcess(Bitmap bitmap) {
        // 模块中的
        ThreadPoolManager.execute(new Runnable() {
            @Override
            public void run() {
                // 线程同步
                synchronized (this) {
                    // 解决检测结果
                    resolveDetectResult(bitmap, BaseConfig.DEFAULT_THRESHOLD, new ResultListener.ListListener<DetectResultModel>() {
                        @Override
                        public void onResult(List<BaseRectBoundResultModel> results) {
                            if (results != null) {
                                // 物体检测
                                if (configBean.getModel_type() == MODEL_DETECT) {
                                    realtime_result_mask.setRectListInfo(results, bitmap.getWidth(), bitmap.getHeight());
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 解决检测结果
     *
     * @param bitmap
     * @param confidence
     * @param listener
     */
    private void resolveDetectResult(Bitmap bitmap, float confidence, final ResultListener.ListListener listener) {
        // 物体检测
        if (configBean.getModel_type() == MODEL_DETECT) {
            onDetectBitmap(bitmap, confidence, new ResultListener.DetectListener() {
                @Override
                public void onResult(List<BaseRectBoundResultModel> models) {
                    if (models == null) {
                        listener.onResult(null);
                        return;
                    }
                    detectResultModelCache = models;
                    listener.onResult(models);
                }
            });

        }
    }

    /**
     * 新线程中调用 ，从照相机中获取bitmap
     *
     * @param bitmap     RGBA格式
     * @param confidence [0-1）
     * @return
     */
    public void onDetectBitmap(Bitmap bitmap, float confidence, final ResultListener.DetectListener listener) {
        try {
            /*if (isOnline) {
                mOnlineDetect.detect(bitmap, confidence, new DetectInterface.OnResultListener() {
                    @Override
                    public void onResult(List<DetectionResultModel> result) {
                        listener.onResult(fillDetectionResultModel(result));
                    }

                    @Override
                    public void onError(BaseException ex) {
                        listener.onResult(null);
                        ex.printStackTrace();
                        Log.d(TAG, "onError: " + ex.getMessage());
                    }
                });
                return;
            }*/

            if (mInferManager == null) {
                Log.d(TAG, "onDetectBitmap: 模型初始化中，请稍后");
                listener.onResult(null);
                return;
            }

            List<DetectionResultModel> modelList = mInferManager.detect(bitmap, confidence);
            listener.onResult(fillDetectionResultModel(modelList));
        } catch (BaseException e) {
            e.printStackTrace();
            Log.d(TAG, "onDetectBitmap: " + e.getMessage());
            listener.onResult(null);
        }
    }

    /**
     * 填充检测结果模型
     *
     * @param modelList
     * @return
     */
    private List<BaseRectBoundResultModel> fillDetectionResultModel(List<DetectionResultModel> modelList) {
        List<BaseRectBoundResultModel> results = new ArrayList<>();
        for (int i = 0; i < modelList.size(); i++) {
            DetectionResultModel mDetectionResultModel = modelList.get(i);
            DetectResultModel mDetectResultModel = new DetectResultModel();
            mDetectResultModel.setIndex(i + 1);
            mDetectResultModel.setConfidence(mDetectionResultModel.getConfidence());
            mDetectResultModel.setName(mDetectionResultModel.getLabel());
            mDetectResultModel.setBounds(mDetectionResultModel.getBounds());
            results.add(mDetectResultModel);
        }
        return results;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isStart = false;

        ThreadPoolManager.executeSingle(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mInferManager != null) {
                        mInferManager.destroy();
                    }
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        isStart = true;
        initConfig();
        start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThreadPoolManager.cancelAutoFocusTimer();
    }
}