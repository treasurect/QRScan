package com.treasure.qrscan.ui.base;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.treasure.qrscan.ui.view.LoadingDialog;

import butterknife.ButterKnife;

/**
 * Created by treasure on 2018/4/26.
 * <p>
 * ------->   treasure <-------
 */

public abstract class BaseActivity extends AppCompatActivity {
    private LoadingDialog.Builder builder;
    private LoadingDialog loading;
    private Context context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        loadContentLayout();
        context = this;
        ButterKnife.bind(this);
        initView();
        setListener();
    }

    public void showLoading() {
        if (builder == null) {
            builder = new LoadingDialog.Builder(this)
                    .setMessage("加载中...")
                    .setCancelOutside(false);
        }
        if (loading == null) {
            loading = builder.create();
            loading.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                        loading.dismiss();
                        finish();
                    }
                    return false;
                }
            });
//            loading.setOnCancelListener(new DialogInterface.OnCancelListener(){
//
//                @Override
//                public void onCancel(DialogInterface dialog) {
//                    finish();
//                }
//            });
//            loading.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                @Override
//                public void onCancel(DialogInterface dialog) {
//                    finish();
//                }
//            });
        }
        if (!loading.isShowing() && isValidContext(context))
            loading.show();
    }

    public void showLoading(String name) {
        builder = new LoadingDialog.Builder(this)
                .setMessage(name + "...")
                .setCancelOutside(false);
        loading = builder.create();
        loading.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                    loading.dismiss();
                    finish();
                }
                return false;
            }
        });

        if (!loading.isShowing() && isValidContext(context))
            loading.show();
    }

    public void dissLoading() {
        if (context != null && !((Activity) context).isFinishing() &&
                loading != null && loading.isShowing() && isValidContext(context))
            loading.dismiss();
    }

    private boolean isValidContext(Context c) {

        Activity a = (Activity) c;

        if (a.isDestroyed() || a.isFinishing()) {
            return false;
        } else {
            return true;
        }
    }

    protected abstract void loadContentLayout();

    protected abstract void initView();

    protected abstract void setListener();

}
