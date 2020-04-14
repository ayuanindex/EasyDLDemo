package com.baidu.ai.edge.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.ai.edge.ui.R;
import com.baidu.ai.edge.ui.camera.CameraListener;
import com.baidu.ai.edge.ui.layout.ActionBarLayout;
import com.baidu.ai.edge.ui.util.ImageUtil;
import com.baidu.ai.edge.ui.util.StringUtil;
import com.baidu.ai.edge.ui.util.ThreadPoolManager;
import com.baidu.ai.edge.ui.util.UiLog;
import com.baidu.ai.edge.ui.view.PreviewDecoratorView;
import com.baidu.ai.edge.ui.view.PreviewView;
import com.baidu.ai.edge.ui.view.ResultListView;
import com.baidu.ai.edge.ui.view.ResultMaskView;
import com.baidu.ai.edge.ui.view.adapter.ClassifyResultAdapter;
import com.baidu.ai.edge.ui.view.adapter.DetectResultAdapter;
import com.baidu.ai.edge.ui.view.model.BaseRectBoundResultModel;
import com.baidu.ai.edge.ui.view.model.BaseResultModel;
import com.baidu.ai.edge.ui.view.model.ClassifyResultModel;
import com.baidu.ai.edge.ui.view.model.DetectResultModel;
import com.baidu.ai.edge.ui.view.model.SegmentResultModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruanshimin on 2018/5/3.
 */

public abstract class MainActivity extends AppCompatActivity {

    public static final int MODEL_SEGMENT = 6;

    public static final int MODEL_DETECT = 2;

    public static final int MODEL_CLASSIFY = 1;

    protected int model;

    static final int ACTION_TYPE_SCAN = 0;

    static final int ACTION_TYPE_TAKE = 1;

    private int actionType = ACTION_TYPE_TAKE;

    private ActionBarLayout actionBarLayout;
    private PreviewView mPreviewView;
    private ImageView switchSideButton;
    private ImageView takePictureButton;
    private ImageView albumSelectButton;

    private ViewGroup realtimeControlViewGroup;

    private ResultMaskView resultMaskView;
    private ResultMaskView realtimeResultMaskView;

    private SeekBar confidenceSeekbar;
    private TextView seekbarText;

    private SeekBar realtimeConfidenceSeekbar;
    private TextView realtimeSeekbarText;

    private ImageView realtimeToggleButton;

    private ResultListView detectResultView;
    private ResultListView classifyResultView;

    private PreviewDecoratorView mPreviewDecoratorView;

    boolean isRealtimeStatusRunning = false;

    private ImageView backInResult;
    private ImageView backInPreview;
    private ImageView moreMenuButton;

    private TextView actionTakePictureBtn;
    private TextView actionRealtimeBtn;

    private View mCameraPageView;
    private ViewGroup mResultPageView;

    private ViewGroup resultTablePopview;

    private ViewGroup recLoading;

    private ImageView resultImage;

    private static final int REQUEST_PERMISSION_CODE_CAMERA = 100;
    private static final int REQUEST_PERMISSION_CODE_STORAGE = 101;

    private static final int INTENT_CODE_PICK_IMAGE = 100;

    protected static final int PAGE_CAMERA = 100;
    protected static final int PAGE_RESULT = 101;

    protected int pageCode;

    private float resultConfidence;

    private float realtimeConfidence;


    Handler uiHandler;

    private String name;

    public abstract void onActivityCreate();

    public abstract void onActivityDestory();

    public abstract void onDetectBitmap(Bitmap bitmap, float confidence,
                                        ResultListener.DetectListener listener);

    public abstract void onClassifyBitmap(Bitmap bitmap, float confidence,
                                          ResultListener.ClassifyListener listener);

    public abstract void onSegmentBitmap(Bitmap bitmap, float confidence,
                                         ResultListener.SegmentListener listener);

    public abstract void dumpDetectResult(List<DetectResultModel> model,
                                          Bitmap bitmap, float min);

    public abstract void dumpClassifyResult(List<ClassifyResultModel> model,
                                            Bitmap bitmap, float min);

    protected void setConfidence(float value) {
        resultConfidence = value;
        realtimeConfidence = value;
        confidenceSeekbar.setProgress((int) (resultConfidence * 100));
        realtimeConfidenceSeekbar.setProgress((int) (realtimeConfidence * 100));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取上一个界面传递过来的数据
        name = getIntent().getStringExtra("name");
        model = getIntent().getIntExtra("model_type", MODEL_DETECT);

        // 请求权限
        requestPermissionCamera();
    }

    private void init() {
        // 获取主线程的Handler
        uiHandler = new Handler(getMainLooper());
        pageCode = PAGE_CAMERA;

        setContentView(R.layout.ui_activity_main);
        actionBarLayout = findViewById(R.id.action_bar);
        resultImage = findViewById(R.id.result_image);
        mCameraPageView = findViewById(R.id.camera_page);
        mResultPageView = findViewById(R.id.result_page);
        mPreviewDecoratorView = findViewById(R.id.preview_decorator_view);

        resultMaskView = findViewById(R.id.result_mask);
        resultMaskView.setHandler(uiHandler);
        realtimeResultMaskView = findViewById(R.id.realtime_result_mask);
        realtimeResultMaskView.setHandler(uiHandler);

        resultMaskView = findViewById(R.id.result_mask);

        seekbarText = findViewById(R.id.seekbar_text);
        realtimeSeekbarText = findViewById(R.id.realtime_seekbar_text);
        realtimeControlViewGroup = findViewById(R.id.realtime_confidence_control);

        recLoading = findViewById(R.id.rec_loading);

        resultTablePopview = findViewById(R.id.result_table_popview);
        confidenceSeekbar = findViewById(R.id.confidence_seekbar);
        realtimeConfidenceSeekbar = findViewById(R.id.realtime_confidence_seekbar);
        ((TextView) findViewById(R.id.model_name)).setText(name);

        initSeekbarArea();
        // 添加监听
        addListener();
        // 更新实时控制视图组
        updateRealtimeControlViewGroup();
        // 更新实时结果弹出视图组
        updateRealtimeResultPopViewGroup();

        // 设置自动拍照过程
        setAutoTakePictureProcess();

        onActivityCreate();
    }

    private void initSeekbarArea() {
        if (model == MODEL_CLASSIFY) {
            setConfidence(0);
        }
    }

    private void updateRealtimeControlViewGroup() {
        if (actionType == ACTION_TYPE_SCAN) {
            realtimeControlViewGroup.setVisibility(View.VISIBLE);
        } else {
            realtimeControlViewGroup.setVisibility(View.GONE);
        }
    }

    private void updateRealtimeResultPopViewGroup() {
        if (model == MODEL_CLASSIFY) {
            if (actionType == ACTION_TYPE_SCAN && isRealtimeStatusRunning) {
                resultTablePopview.setVisibility(View.VISIBLE);
            } else {
                resultTablePopview.setVisibility(View.GONE);
            }
        }
        if (model == MODEL_DETECT || model == MODEL_SEGMENT) {
            resultTablePopview.setVisibility(View.GONE);
            if (actionType == ACTION_TYPE_SCAN && !isRealtimeStatusRunning) {
                realtimeResultMaskView.clear();
            }
        }

    }


    private boolean isProcessingAutoTakePicture = false;

    protected boolean canAutoRun = false;

    private boolean autoTakeFlag = true;

    private void setAutoTakePictureProcess() {
        // 模块中的
        ThreadPoolManager.execute(new Runnable() {
            @Override
            public void run() {
                while (autoTakeFlag) {
                    // 线程同步
                    synchronized (this) {
                        if (!isProcessingAutoTakePicture && isRealtimeStatusRunning) {
                            mPreviewView.takePicture(new CameraListener.TakePictureListener() {
                                @Override
                                public void onTakenPicture(final Bitmap bitmap) {
                                    UiLog.info("auto picture has processed");
                                    isProcessingAutoTakePicture = true;
                                    resolveDetectResult(bitmap,
                                            realtimeConfidence, new ResultListener.ListListener<DetectResultModel>() {
                                                @Override
                                                public void onResult(List<BaseRectBoundResultModel> results) {
                                                    if (isRealtimeStatusRunning && results != null) {
                                                        if (model == MODEL_DETECT) {
                                                            realtimeResultMaskView.setRectListInfo(results,
                                                                    bitmap.getWidth(), bitmap.getHeight());
                                                        }
                                                        if (model == MODEL_CLASSIFY) {
                                                            fillClassifyList(results);
                                                        }
                                                        if (model == MODEL_SEGMENT) {
                                                            realtimeResultMaskView.setRectListInfo(results,
                                                                    bitmap.getWidth(), bitmap.getHeight());
                                                        }
                                                    }
                                                    isProcessingAutoTakePicture = false;
                                                }
                                            });

                                }
                            });
                        }
                    }
                }
            }
        });
    }

    protected void showMessage(final String msg) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.setTitle("提示")
                        .setMessage(msg)
                        .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        });

    }

    protected void showMessage(final int errorCode, final String msg) {
        showMessage("errorcode: " + errorCode + ":" + msg);
    }

    /**
     * 设置“拍摄照片”按钮可用
     *
     * @param available
     */
    protected void setTakePictureButtonAvailable(boolean available) {
        if (takePictureButton == null) {
            return;
        }
        if (available) {
            takePictureButton.setColorFilter(null);
        } else {
            takePictureButton.setColorFilter(Color.GRAY);
        }
    }

    private void addListener() {
        mPreviewView = findViewById(R.id.preview_view);

        switchSideButton = findViewById(R.id.switchSide);
        takePictureButton = findViewById(R.id.takePicture);
        // 设置拍摄照片按钮是否可用
        setTakePictureButtonAvailable(false);
        albumSelectButton = findViewById(R.id.albumSelect);

        actionTakePictureBtn = findViewById(R.id.action_takepicture_btn);
        actionRealtimeBtn = findViewById(R.id.action_realtime_btn);

        backInResult = findViewById(R.id.back_in_result);
        backInPreview = findViewById(R.id.back_in_preview);
        moreMenuButton = findViewById(R.id.menu_more);

        realtimeToggleButton = findViewById(R.id.realtime_toggle_btn);

        detectResultView = findViewById(R.id.result_list_view);
        detectResultView.setHandler(uiHandler);
        classifyResultView = findViewById(R.id.result_list_popview);
        classifyResultView.setHandler(uiHandler);


        switchSideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionType == ACTION_TYPE_SCAN && model == MODEL_DETECT) {
                    realtimeResultMaskView.clear();
                }
                if (actionType == ACTION_TYPE_SCAN && model == MODEL_CLASSIFY) {
                    classifyResultView.clear();
                }
                if (actionType == ACTION_TYPE_SCAN && model == MODEL_SEGMENT) {
                    realtimeResultMaskView.clear();
                }
                mPreviewView.switchSide();
            }
        });


        confidenceSeekbar.setMax(100);
        confidenceSeekbar.setProgress((int) (resultConfidence * 100));
        seekbarText.setText(StringUtil.formatFloatString(resultConfidence));
        confidenceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                resultConfidence = seekBar.getProgress() / 100f;
                seekbarText.setText(StringUtil.formatFloatString(resultConfidence));

                if (model == MODEL_DETECT && detectResultModelCache != null) {
                    updateResultImageAndList(detectResultModelCache, detectBitmapCache, resultConfidence);
                }
                if (model == MODEL_CLASSIFY && classifyResultModelCache != null) {
                    updateResultImageAndList(classifyResultModelCache, detectBitmapCache, resultConfidence);
                }
                if (model == MODEL_SEGMENT && segmentResultModelCache != null) {
                    updateResultImageAndList(segmentResultModelCache, detectBitmapCache, resultConfidence);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        realtimeConfidenceSeekbar.setMax(100);
        realtimeConfidenceSeekbar.setProgress((int) (realtimeConfidence * 100));
        realtimeSeekbarText.setText(StringUtil.formatFloatString(resultConfidence));
        realtimeConfidenceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                realtimeConfidence = seekBar.getProgress() / 100f;
                realtimeSeekbarText.setText(StringUtil.formatFloatString(realtimeConfidence));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }
        });


        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoading) {
                    return;
                }
                mPreviewView.takePicture(new CameraListener.TakePictureListener() {
                    @Override
                    public void onTakenPicture(Bitmap bitmap) {
                        UiLog.info("picture has taken");
                        toggleLoading(true);
                        showResultPage(bitmap);
                    }
                });
            }
        });

        actionTakePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActionBtnHighlight(actionTakePictureBtn);
                setActionBtnDefault(actionRealtimeBtn);
                toggleDecoratorView(false);
                toggleOperationBtns(true);
            }
        });

        // 切换到实时检测
        actionRealtimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 判断是否准备好模型准备自动识别
                if (canAutoRun) {
                    setActionBtnHighlight(actionRealtimeBtn);
                    setActionBtnDefault(actionTakePictureBtn);
                    toggleDecoratorView(true);
                    toggleOperationBtns(false);

                    // 切换实时状态
                    toggleRealtimeStatus();
                } else {
                    showMessage("模型初始化中，请稍后");
                }
            }
        });

        realtimeToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRealtimeStatus();
            }
        });

        albumSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoading) {
                    return;
                }
                toggleLoading(true);
                requestPermissionAlbum();
            }
        });

        backInResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToCamera();
            }
        });

        backInPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canAutoRun) {
                    mPreviewView.stopPreview();
                    finish();
                } else {
                    showMessage("模型未初始化");
                }
            }
        });
        moreMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showActionBarMenu();
            }
        });
    }

    private boolean isLoading = false;

    private void toggleLoading(final boolean isLoading) {
        this.isLoading = isLoading;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isLoading) {
                    mPreviewView.stopPreview();
                    recLoading.setVisibility(View.VISIBLE);
                } else {
                    mPreviewView.start();
                    recLoading.setVisibility(View.GONE);
                }
            }
        });
    }

    private void toggleDecoratorView(boolean isVisible) {
        if (isVisible) {
            mPreviewDecoratorView.setVisibility(View.VISIBLE);
        } else {
            mPreviewDecoratorView.setVisibility(View.GONE);
        }
    }

    /**
     * 切换扫描或者拍照模式
     *
     * @param isTakePicture
     */
    private void toggleOperationBtns(boolean isTakePicture) {
        if (isTakePicture) {
            takePictureButton.setVisibility(View.VISIBLE);
            albumSelectButton.setVisibility(View.VISIBLE);
            realtimeToggleButton.setVisibility(View.GONE);

            if (isRealtimeStatusRunning) {
                toggleRealtimeStatus();
            }

            actionType = ACTION_TYPE_TAKE;
        } else {
            takePictureButton.setVisibility(View.INVISIBLE);
            albumSelectButton.setVisibility(View.GONE);
            realtimeToggleButton.setVisibility(View.VISIBLE);
            actionType = ACTION_TYPE_SCAN;
        }
        updateRealtimeResultPopViewGroup();
        updateRealtimeControlViewGroup();
        clearMaskInfo();
    }

    /**
     * 清除实时
     */
    private void clearMaskInfo() {
        if (model == MODEL_DETECT || model == MODEL_SEGMENT) {
            realtimeResultMaskView.clear();
        }

        if (model == MODEL_CLASSIFY) {
            classifyResultView.clear();
        }
    }

    private void toggleRealtimeStatus() {
        if (canAutoRun || isRealtimeStatusRunning) {
            isRealtimeStatusRunning = !isRealtimeStatusRunning;
            // 更新实时结果弹出视图组
            updateRealtimeResultPopViewGroup();
            // 切换实时样式
            toggleRealtimeStyle();
        }
    }


    private void toggleRealtimeStyle() {
        if (isRealtimeStatusRunning) {
            mPreviewDecoratorView.setStatus(true);
            realtimeToggleButton.setImageResource(R.drawable.realtime_stop_btn);
        } else {
            mPreviewDecoratorView.setStatus(false);
            realtimeToggleButton.setImageResource(R.drawable.realtime_start_btn);
        }
    }

    private void setActionBtnHighlight(TextView btn) {
        btn.setBackgroundResource(R.color.bk_black);
        ColorStateList color = getResources().getColorStateList(R.color.textColorHighlight);
        btn.setTextColor(color);
    }

    private void setActionBtnDefault(TextView btn) {
        btn.setBackgroundResource(R.color.bk_black);
        ColorStateList color = getResources().getColorStateList(R.color.textColor);
        btn.setTextColor(color);
    }

    private void openAlum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, INTENT_CODE_PICK_IMAGE);
    }

    private void showResultPage(final Bitmap bitmap) {

        ThreadPoolManager.executeSingle(new Runnable() {
            @Override
            public void run() {
                resolveDetectResult(bitmap, 0,
                        new ResultListener.ListListener<DetectResultModel>() {
                            @Override
                            public void onResult(List<BaseRectBoundResultModel> results) {
                                if (results != null) {
                                    updateResultImageAndList(results, bitmap, resultConfidence);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toggleLoading(false);
                                            mCameraPageView.setVisibility(View.GONE);
                                            mResultPageView.setVisibility(View.VISIBLE);
                                            resultImage.setImageBitmap(bitmap);
                                            pageCode = PAGE_RESULT;
                                            detectBitmapCache = bitmap;
                                        }
                                    });
                                } else {
                                    toggleLoading(false);
                                }
                            }
                        });

            }
        });
    }

    private Bitmap detectBitmapCache;

    private List<BaseRectBoundResultModel> detectResultModelCache;

    private List<BaseRectBoundResultModel> segmentResultModelCache;

    private List<ClassifyResultModel> classifyResultModelCache;

    private void resolveDetectResult(Bitmap bitmap, float confidence,
                                     final ResultListener.ListListener listener) {
        if (model == MODEL_DETECT) {
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
        if (model == MODEL_CLASSIFY) {

            onClassifyBitmap(bitmap, confidence, new ResultListener.ClassifyListener() {
                @Override
                public void onResult(List<ClassifyResultModel> models) {
                    if (models == null) {
                        listener.onResult(null);
                        return;
                    }

                    classifyResultModelCache = models;
                    listener.onResult(models);
                }
            });
        }
        if (model == MODEL_SEGMENT) {

            onSegmentBitmap(bitmap, confidence, new ResultListener.SegmentListener() {

                @Override
                public void onResult(List<BaseRectBoundResultModel> models) {
                    if (models == null) {
                        listener.onResult(null);
                        return;
                    }
                    segmentResultModelCache = models;
                    listener.onResult(models);
                }
            });
        }
    }

    private void updateResultImageAndList(List<? extends BaseResultModel> results, final Bitmap bitmap, float min) {


        if (model == MODEL_DETECT) {
            List<DetectResultModel> filteredList
                    = filterDetectListByConfidence((List<DetectResultModel>) results, min);
            fillDetectList(filteredList);
            fillRectImageView(filteredList, bitmap);
        }

        if (model == MODEL_CLASSIFY) {
            List<ClassifyResultModel> filteredList
                    = filterClassifyListByConfidence((List<ClassifyResultModel>) results, min);
            fillDetectList(filteredList);
        }
        if (model == MODEL_SEGMENT) {
            List<SegmentResultModel> filteredList
                    = filterSegmentListByConfidence((List<SegmentResultModel>) results, min);
            fillDetectList(filteredList);
            fillRectImageView(filteredList, bitmap);
        }
    }

    private List<ClassifyResultModel> filterClassifyListByConfidence(List<ClassifyResultModel> results, float min) {
        List<ClassifyResultModel> filteredList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < results.size(); i++) {
            ClassifyResultModel mBaseResultModel = results.get(i);
            if (mBaseResultModel.getConfidence() > min) {
                filteredList.add(new ClassifyResultModel(++j, mBaseResultModel.getName(),
                        mBaseResultModel.getConfidence()));
            }
        }
        return filteredList;
    }

    private List<DetectResultModel> filterDetectListByConfidence(List<DetectResultModel> results, float min) {
        List<DetectResultModel> filteredList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < results.size(); i++) {
            DetectResultModel mDetectResultModel = results.get(i);
            if (mDetectResultModel.getConfidence() > min) {
                filteredList.add(new DetectResultModel(++j, mDetectResultModel.getName(),
                        mDetectResultModel.getConfidence(), mDetectResultModel.getBounds()));
            }
        }
        return filteredList;
    }

    private List<SegmentResultModel> filterSegmentListByConfidence(List<SegmentResultModel> results, float min) {
        List<SegmentResultModel> filteredList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < results.size(); i++) {
            SegmentResultModel mSegmentResultModel = results.get(i);
            if (mSegmentResultModel.getConfidence() > min) {
                SegmentResultModel model = new SegmentResultModel(++j, mSegmentResultModel.getName(),
                        mSegmentResultModel.getConfidence(), mSegmentResultModel.getBounds(),
                        mSegmentResultModel.getMask());
                model.setColorId(mSegmentResultModel.getColorId());
                filteredList.add(model);
            }
        }
        return filteredList;
    }

    private void fillRectImageView(List<? extends BaseRectBoundResultModel> result, Bitmap bitmap) {
        resultMaskView.setRectListInfo((List<BaseRectBoundResultModel>) result, bitmap.getWidth(), bitmap.getHeight());
    }

    /**
     * 绘制 编号 名字 置信度的列表
     *
     * @param results
     */
    private void fillDetectList(List<? extends BaseResultModel> results) {
        final DetectResultAdapter adapter = new DetectResultAdapter(this,
                R.layout.result_detect_item, (List<BaseResultModel>) results);

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                detectResultView.setAdapter(adapter);
                detectResultView.invalidate();
            }
        });
    }

    private void fillClassifyList(List<BaseRectBoundResultModel> results) {
        final ClassifyResultAdapter adapter = new ClassifyResultAdapter(this,
                R.layout.result_table_popview_item, results);

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                classifyResultView.setAdapter(adapter);
            }
        });
    }

    private void returnToCamera() {
        mCameraPageView.setVisibility(View.VISIBLE);
        mResultPageView.setVisibility(View.GONE);
        pageCode = PAGE_CAMERA;
        resultMaskView.clear();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE_CAMERA:
                if (grantResults[0] == -1) {
                    Toast toast = Toast.makeText(this, "请选择权限", Toast.LENGTH_SHORT);
                    toast.show();
                    finish();
                } else {
                    init();
                }
                break;
            case REQUEST_PERMISSION_CODE_STORAGE:
                if (grantResults[0] == -1) {
                    Toast toast = Toast.makeText(this, "请选择权限", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    openAlum();
                }
                break;
            default:
                break;
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_CODE_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                String path = getRealPathFromURI(uri);
                UiLog.info("pick image url: " + path);
                Bitmap bitmap = readFile(path);
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(path);
                    int exifRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                    int rotation = ImageUtil.exifToDegrees(exifRotation);
                    Bitmap rotateBitmap = ImageUtil.createRotateBitmap(bitmap, rotation);
                    showResultPage(rotateBitmap);
                } catch (IOException e) {
                    return;
                }
            } else {
                toggleLoading(false);
            }
        }
    }

    private Bitmap readFile(String path) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(path);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);

            UiLog.info("pick image success");
            return bitmap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentURI, null, null, null, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private void requestPermissionAlbum() {
        // 判断是否已经赋予权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE_STORAGE);
        } else {
            openAlum();
        }
    }

    protected abstract void onSetMenu(PopupMenu actionBarMenu);

    protected abstract void onSetMenuItem(boolean isOnline);

    protected void showActionBarMenu() {
        PopupMenu actionBarMenu = new PopupMenu(MainActivity.this, actionBarLayout, Gravity.END);
        actionBarMenu.getMenuInflater().inflate(R.menu.actionbar_more_menu, actionBarMenu.getMenu());
        actionBarMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Log.d("menu", menuItem.getOrder() + "");
                menuItem.setChecked(true);
                switch (menuItem.getOrder()) {
                    case 0:
                        onSetMenuItem(false);
                        break;
                    case 1:
                        onSetMenuItem(true);
                        break;
                    default:
                        break;
                }
                return false;
            }

        });
        onSetMenu(actionBarMenu);
        actionBarMenu.show();

    }

    /**
     * 监听Back键按下事件,方法1:
     * 注意:
     * super.onBackPressed()会自动调用finish()方法,关闭
     * 当前Activity.
     * 若要屏蔽Back键盘,注释该行代码即可
     */
    @Override
    public void onBackPressed() {
        isRealtimeStatusRunning = false;
        if (pageCode == PAGE_CAMERA) {
            super.onBackPressed();
        }
        if (pageCode == PAGE_RESULT) {
            returnToCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRealtimeStatusRunning = false;
        if (mPreviewView != null) {
            mPreviewView.destory();
        }
        autoTakeFlag = false;
        onActivityDestory();
    }

    private void requestPermissionCamera() {
        // 判断是否已经赋予权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CODE_CAMERA);
        } else {
            init();
        }
    }
}
