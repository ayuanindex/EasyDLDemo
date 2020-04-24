package com.realmax.easydldemo.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.ddk.DDKConfig;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.detect.DetectionResultModel;
import com.baidu.ai.edge.core.infer.InferConfig;
import com.baidu.ai.edge.core.infer.InferInterface;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.snpe.SnpeConfig;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import com.baidu.ai.edge.core.util.FileUtil;
import com.baidu.ai.edge.core.util.Util;
import com.baidu.ai.edge.ui.util.ThreadPoolManager;
import com.baidu.ai.edge.ui.view.ResultMaskView;
import com.baidu.ai.edge.ui.view.model.BaseRectBoundResultModel;
import com.baidu.ai.edge.ui.view.model.DetectResultModel;
import com.google.gson.Gson;
import com.realmax.easydldemo.R;
import com.realmax.easydldemo.bean.ConfigBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ayuan
 */
public class Demo extends AppCompatActivity {
    private static final String TAG = "Demo";
    /**
     * 在这里替换序列号
     */
    public static final String SERIAL_NUM = "CB4E-6CDD-8F98-BA44";
    private ImageView iv_image;
    private ResultMaskView realtime_result_mask;
    private Handler uiHandler;

    /**
     * 图片集合
     */
    private ArrayList<Bitmap> bitmapList;

    /**
     * 配置信息
     */
    private ConfigBean configBean;

    /**
     * 当前芯片类型
     */
    private String currentSoc;
    public static final int TYPE_INFER = 0;
    public static final int TYPE_DDK150 = 1;
    public static final int TYPE_DDK200 = 11;
    public static final int TYPE_SNPE = 2;
    private int platform;
    private int count;
    private int MODEL_DETECT = 2;
    private InferInterface mInferManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: ---------------------");
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: -------------------------");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ---------------------");
        requestPermission();
        Log.d(TAG, "onStart: ---------------------");
    }

    /**
     * 权限申请判断
     */
    private void requestPermission() {
        Log.d(TAG, "requestPermission: --------------------");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10);
        } else {
            initView();
            init();
        }
        Log.d(TAG, "requestPermission: --------------------");
    }

    private void initView() {
        Log.d(TAG, "initView: -------------------------");
        iv_image = (ImageView) findViewById(R.id.iv_image);
        realtime_result_mask = (ResultMaskView) findViewById(R.id.realtime_result_mask);
        Log.d(TAG, "initView: -------------------------");
    }

    /**
     * 初始化操作
     */
    private void init() {
        Log.d(TAG, "init: ----------------------------");
        uiHandler = new Handler(getMainLooper());
        realtime_result_mask.setHandler(uiHandler);
        bitmapList = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            int identifier = getResources().getIdentifier("pic_" + (i + 1), "drawable", getPackageName());
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), identifier);
            bitmapList.add(bitmap);
        }

        // 初始化配置信息
        initConfig();

        // 判断芯片类型是否受支持
        if (checkChip()) {
            realtime_result_mask.clear();

            // 选择设备类型
            choosePlatform();

            ThreadPoolManager.executeSingle(new Runnable() {
                @Override
                public void run() {
                    initManager();
                }
            });

            count = 0;

            ThreadPoolManager.createAutoFocusTimerTask(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        Bitmap bitmap = bitmapList.get(count);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                iv_image.setImageBitmap(bitmap);
                            }
                        });

                        try {
                            if (mInferManager != null) {
                                detection(bitmap, new Listener() {
                                    @Override
                                    public void onResult(List<BaseRectBoundResultModel> detect) {
                                        if (detect != null) {
                                            Log.d(TAG, "onResult: ---------------------");
                                            realtime_result_mask.setRectListInfo(detect, bitmap.getWidth(), bitmap.getHeight());
                                            Log.d(TAG, "onResult: ---------------------");
                                        }
                                    }
                                });
                            }
                        } catch (BaseException e) {
                            e.printStackTrace();
                        }

                        count++;
                        if (count >= bitmapList.size()) {
                            count = 0;
                        }
                    }
                }
            });
        }
        Log.d(TAG, "init: ----------------------------");
    }

    /**
     * 初始化配置信息
     */
    private void initConfig() {
        Log.d(TAG, "initConfig: -----------------------------------");
        try {
            String configJsonStr = FileUtil.readAssetFileUtf8String(getAssets(), "demo/config.json");
            configBean = new Gson().fromJson(configJsonStr, ConfigBean.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "initConfig: -----------------------------------");
    }

    /**
     * 判断芯片类型
     */
    private boolean checkChip() {
        Log.d(TAG, "checkChip: --------------------------");
        if (configBean.getSoc().contains("dsp") && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            currentSoc = "dsp";
            Log.d(TAG, "checkChip: --------------------------");
            return true;
        }

        if (configBean.getSoc().contains("npu") && (Build.HARDWARE.contains("kirin970") || Build.HARDWARE.contains("kirin980"))) {
            if (Build.HARDWARE.contains("kirin970")) {
                currentSoc = "npu150";
            } else if (Build.HARDWARE.contains("kirin980")) {
                currentSoc = "npu200";
            }
            Log.d(TAG, "checkChip: --------------------------");
            return true;
        }

        if (configBean.getSoc().contains("arm")) {
            currentSoc = "arm";
            Log.d(TAG, "checkChip: --------------------------");
            return true;
        }
        Log.d(TAG, "checkChip: --------------------------");
        return false;
    }

    /**
     * 选择设备类型
     */
    private void choosePlatform() {
        Log.d(TAG, "choosePlatform: ---------------------");
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
        Log.d(TAG, "choosePlatform: ---------------------");
    }

    private void initManager() {
        try {
            Log.d(TAG, "initManager: -----------------------");
            if (configBean.getModel_type() == MODEL_DETECT) {
                if (mInferManager == null) {
                    switch (platform) {
                        case TYPE_DDK200:
                            DDKConfig ddkConfig = new DDKConfig(getAssets(), "ddk-detect/config.json");
                            mInferManager = new DDKManager(this, ddkConfig, SERIAL_NUM);
                            break;
                        case TYPE_SNPE:
                            SnpeConfig snpeConfig = new SnpeConfig(getAssets(), "snpe-detect/config.json");
                            mInferManager = new SnpeManager(this, snpeConfig, SERIAL_NUM);
                            break;
                        case TYPE_INFER:
                        default:
                            InferConfig inferConfig = new InferConfig(getAssets(), "infer-detect/config.json");
                            // 可修改ARM推断使用的CPU核心数
                            inferConfig.setThread(Util.getInferCores());
                            mInferManager = new InferManager(this, inferConfig, SERIAL_NUM);
                            break;

                    }
                }
            }
            Log.d(TAG, "initManager: -----------------------");
        } catch (BaseException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检测图片中的物体
     *
     * @param bitmap
     * @param listener
     * @throws BaseException
     */
    private void detection(Bitmap bitmap, Listener listener) throws BaseException {
        Log.d(TAG, "detection: ----------------------");
        List<DetectionResultModel> detect = mInferManager.detect(bitmap);
        if (detect != null) {
            listener.onResult(fillDetectionResultModel(detect));
        }
        Log.d(TAG, "detection: ----------------------");
    }

    /**
     * 填充检测结果模型
     *
     * @param modelList
     * @return
     */
    private List<BaseRectBoundResultModel> fillDetectionResultModel(List<DetectionResultModel> modelList) {
        Log.d(TAG, "fillDetectionResultModel: ----------------------------");
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
        Log.d(TAG, "fillDetectionResultModel: ----------------------------");
        return results;
    }

    interface Listener {
        /**
         * 返回转换后的识别结果
         *
         * @param detect
         */
        void onResult(List<BaseRectBoundResultModel> detect);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ---------------------");
        ThreadPoolManager.cancelAutoFocusTimer();
        bitmapList.clear();
        System.gc();
        Log.d(TAG, "onPause: ---------------------");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mInferManager != null) {
                mInferManager.destroy();
                realtime_result_mask.clear();
            }
        } catch (BaseException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onDestroy: ----------------------");
    }
}
