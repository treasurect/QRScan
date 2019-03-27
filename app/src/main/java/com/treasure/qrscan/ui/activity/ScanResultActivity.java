package com.treasure.qrscan.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.treasure.qrscan.R;
import com.treasure.qrscan.ui.base.BaseActivity;
import com.treasure.qrscan.ui.view.TextureVideoViewOutlineProvider;
import com.treasure.qrscan.ui.view.VideoPlayerView;
import com.treasure.qrscan.utils.ScreenUtils;

import butterknife.BindView;
import butterknife.OnClick;

public class ScanResultActivity extends BaseActivity {

  public static void start(Context context, String url,int type) {
    Intent intent = new Intent(context, ScanResultActivity.class);
    intent.putExtra("url", url);
    intent.putExtra("type", type);
    context.startActivity(intent);
  }

  @BindView(R.id.title)
  TextView title;
  @BindView(R.id.iv_bg)
  ImageView ivBg;
  @BindView(R.id.video_player)
  VideoPlayerView videoPlayer;
  @BindView(R.id.webview)
  LinearLayout webViewLayout;
  @BindView(R.id.image_view)
  ImageView imageView;
  @BindView(R.id.text_view)
  TextView textView;

  private String url;
  private WebView webView;
  private int type;

  @Override
  protected void loadContentLayout() {
    setContentView(R.layout.activity_scan_result);
  }

  @Override
  protected void initView() {
    title.setText("结果");
    Intent intent = getIntent();
    if (intent != null) {
      url = intent.getStringExtra("url");
      //type 0：text   1:image  2:web  3:video
      type = intent.getIntExtra("type", 0);
      switch (type) {
        case 0:
          ivBg.setVisibility(View.GONE);
          videoPlayer.setVisibility(View.GONE);
          textView.setText(url);
          break;
        case 1:
          ivBg.setVisibility(View.GONE);
          videoPlayer.setVisibility(View.GONE);
          Picasso.get().load(url).into(imageView);
          break;
        case 2:
          ivBg.setVisibility(View.GONE);
          videoPlayer.setVisibility(View.GONE);
          initWebView();
          break;
        case 3:
          initVideoView();
          break;
      }
    }
  }

  private void initWebView() {
    webView = new WebView(this);
    webView.setWebChromeClient(new WebChromeClient());
    webView.setWebViewClient(new WebViewClient());
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setJavaScriptEnabled(true);
    settings.setSupportZoom(false);
    webView.loadUrl(url);
    webViewLayout.addView(webView);
  }

  private void initVideoView() {
    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) ivBg.getLayoutParams();
    layoutParams.width = ScreenUtils.getScreenWidth(this) - ScreenUtils.dip2px(this, 32);
    layoutParams.height = ScreenUtils.getScreenHeight(this) - ScreenUtils.dip2px(this, 143);
    layoutParams.addRule(Gravity.CENTER);
    ivBg.setLayoutParams(layoutParams);
    videoPlayer.setLayoutParams(layoutParams);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      ivBg.setOutlineProvider(new TextureVideoViewOutlineProvider(this, ScreenUtils.dip2px(this, 6)));
      ivBg.setClipToOutline(true);
      videoPlayer.setOutlineProvider(new TextureVideoViewOutlineProvider(this, ScreenUtils.dip2px(this, 6)));
      videoPlayer.setClipToOutline(true);
    }


    videoPlayer.setPlayData(url);
    videoPlayer.initViewDisplay();
  }

  @Override
  protected void setListener() {

  }

  @OnClick({R.id.back})
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.back:
        onBackPressed();
        break;
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
    if (keyCode == keyEvent.KEYCODE_BACK && type == 2) {//监听返回键，如果可以后退就后退
      if (webView.canGoBack()) {
        webView.goBack();
        return true;

      } else {
        finish();
      }
    }
    return super.onKeyDown(keyCode, keyEvent);
  }

  @Override
  protected void onDestroy() {

    if (webView != null && type == 2) {
      // 如果先调用destroy()方法，则会命中if (isDestroyed()) return;这一行代码，需要先onDetachedFromWindow()，再
      // destory()
      ViewParent parent = webView.getParent();
      if (parent != null) {
        ((ViewGroup) parent).removeView(webView);
      }

      webView.stopLoading();
      // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
      webView.getSettings().setJavaScriptEnabled(false);
      webView.clearHistory();
      webView.clearView();
      webView.removeAllViews();
      webView.destroy();

    }
    super.onDestroy();
  }
}
