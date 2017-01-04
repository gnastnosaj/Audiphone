package com.guoguang.audiphone;

import android.app.Application;

import net.qiujuer.genius.Genius;

/**
 * Created by jasontsang on 2/29/16.
 */
public class Audiphone extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Genius.initialize(this);
    }
}
