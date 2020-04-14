package com.realmax.easydldemo;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.baidu.ai.edge.core.base.BaseManager;

import xcrash.XCrash;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        String basePath = Environment.getExternalStorageDirectory().toString() + "/" + base.getPackageName();
        XCrash.InitParameters params = new XCrash.InitParameters();
        params.setAppVersion(BaseManager.VERSION);
        params.setLogDir(basePath + "/xCrash");
        XCrash.init(this, params);
    }
}
