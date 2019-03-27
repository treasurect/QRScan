package com.treasure.qrscan.ui.view;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.treasure.qrscan.utils.ScreenUtils;


/**
 * Created by treasure on 2018/4/9.
 * <p>
 * ------->   treasure <-------
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TextureVideoViewOutlineProvider extends ViewOutlineProvider {
    private  int screenWidth;
    private  int dp32;
    private float mRadius;

    public TextureVideoViewOutlineProvider(Context context, float radius) {
        this.mRadius = radius;
        screenWidth = ScreenUtils.getScreenWidth(context);
        dp32 = ScreenUtils.dip2px(context,32);
    }

    @Override
    public void getOutline(View view, Outline outline) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        int leftMargin = 0;
        int topMargin = 0;
        Rect selfRect = new Rect(leftMargin, topMargin,
                screenWidth-dp32, rect.bottom - rect.top - topMargin);
        outline.setRoundRect(selfRect, mRadius);
    }
}
