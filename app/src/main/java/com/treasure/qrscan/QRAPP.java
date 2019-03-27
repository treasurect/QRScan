package com.treasure.qrscan;

import android.app.Application;
import android.content.Context;

import cn.bmob.v3.Bmob;

/**
 * ============================================================
 * Author：   treasure
 * time：  2019/3/25
 * description:
 * ============================================================
 */
public class QRAPP extends Application {
  private static Context context;
  @Override
  public void onCreate() {
    super.onCreate();
    context =this;
    //初始化BMob云
    Bmob.initialize(this, "1afb5f88522dc546c0965e6c52065b37");
  }

  public static Context getContext() {
    return context;
  }
}
