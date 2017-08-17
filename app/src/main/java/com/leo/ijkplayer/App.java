package com.leo.ijkplayer;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created on 2017/8/17 上午10:08.
 * leo linxiaotao1993@vip.qq.com
 */

public class App extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        if (!LeakCanary.isInAnalyzerProcess(this)){
            LeakCanary.install(this);
        }

    }
}
