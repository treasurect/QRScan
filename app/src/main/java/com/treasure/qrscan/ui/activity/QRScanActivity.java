package com.treasure.qrscan.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.treasure.qrscan.R;
import com.treasure.qrscan.camera.CameraManager;
import com.treasure.qrscan.decoding.CaptureActivityHandler;
import com.treasure.qrscan.decoding.InactivityTimer;
import com.treasure.qrscan.decoding.RGBLuminanceSource;
import com.treasure.qrscan.ui.base.BaseActivity;
import com.treasure.qrscan.ui.view.AlertDialog;
import com.treasure.qrscan.ui.view.ViewfinderView;
import com.treasure.qrscan.utils.GenderChangeListener;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Initial the camera
 *
 * @author Ryan.Tang
 */
public class QRScanActivity extends BaseActivity implements Callback, View.OnClickListener, GenderChangeListener, DialogInterface.OnDismissListener {

  private AlertDialog alertDialog;
  private String url;

  public static void start(Context context) {
    Intent intent = new Intent(context, QRScanActivity.class);
    context.startActivity(intent);
  }

  @BindView(R.id.title)
  TextView title;
  @BindView(R.id.qrscan_view)
  SurfaceView surfaceView;
  @BindView(R.id.viewfinder_content)
  ViewfinderView viewfinderView;

  private CaptureActivityHandler handler;
  private boolean hasSurface;
  private Vector<BarcodeFormat> decodeFormats;
  private String characterSet;
  private InactivityTimer inactivityTimer;
  private MediaPlayer mediaPlayer;
  private boolean playBeep;
  private static final float BEEP_VOLUME = 0.10f;
  private boolean vibrate;
  private Bitmap scanBitmap;
  private boolean isPageDestroy;
  private String type = "text";

  @Override
  protected void loadContentLayout() {
    setContentView(R.layout.activity_qrscan);
  }

  @Override
  protected void initView() {
    title.setText("扫描");
    //ViewUtil.addTopView(getApplicationContext(), this, R.string.scan_card);
    CameraManager.init(getApplication());
    hasSurface = false;
    inactivityTimer = new InactivityTimer(this);
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

  /**
   * 重置
   */
  private void resetQRScan() {
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    CameraManager.get().closeDriver();

    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      initCamera(surfaceHolder);
    } else {
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    decodeFormats = null;
    characterSet = null;

    playBeep = true;
    AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
    if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
      playBeep = false;
    }
    initBeepSound();
    vibrate = true;
  }

  /**
   * Handler scan result
   *
   * @param result
   * @param barcode
   */
  public void handleDecode(Result result, Bitmap barcode) {
    inactivityTimer.onActivity();
    playBeepSoundAndVibrate();
    String resultString = result.getText();
    if (TextUtils.isEmpty(resultString)) {
      if (!isPageDestroy) {
        Toast.makeText(QRScanActivity.this, "Scan failed!", Toast.LENGTH_SHORT).show();
      }
    } else {
      type = resultString.split("-")[0];
      if (resultString.length() != type.length()) {
        url = resultString.substring(type.length() + 1);
      }else {
        url = resultString;
      }
      if (alertDialog == null) {
        alertDialog = new AlertDialog(this);
        alertDialog.setListener(this);
        alertDialog.setOnDismissListener(this);
      }
      alertDialog.show();

      if (type.equals("image")) {
        alertDialog.setContent("图片：\n" + url);
      } else if (type.equals("video")) {
        alertDialog.setContent("视频：\n" + url);
      } else if (type.equals("web")) {
        alertDialog.setContent("网站：\n" + url);
      } else if (type.equals("phone")){
        alertDialog.setContent("是否拨打：\n" + url);
      }else {
        alertDialog.setContent("文字：\n" + url);
      }
    }
  }

  @Override
  public void genderClick(int id) {
    switch (id) {
      case R.id.done:
        if (alertDialog != null && alertDialog.isShowing()) {
          alertDialog.dismiss();
        }
        if (type.equals("image")) {
          ScanResultActivity.start(this, url,1);
        } else if (type.equals("video")) {
          ScanResultActivity.start(this, url,3);
        } else if (type.equals("web")) {
          ScanResultActivity.start(this, url,2);
        } else if (type.equals("phone")){
          Intent intent = new Intent(Intent.ACTION_DIAL);
          Uri data = Uri.parse("tel:" + url);
          intent.setData(data);
          startActivity(intent);
        }else {
          ScanResultActivity.start(this, url,0);
        }
        break;
    }
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    resetQRScan();
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    try {
      CameraManager.get().openDriver(surfaceHolder);
    } catch (IOException | RuntimeException ioe) {
      return;
    }
    if (handler == null) {
      handler = new CaptureActivityHandler(this, decodeFormats,
          characterSet);
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width,
                             int height) {

  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    if (!hasSurface) {
      hasSurface = true;
      initCamera(holder);
    }

  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;

  }

  public ViewfinderView getViewfinderView() {
    return viewfinderView;
  }

  public Handler getHandler() {
    return handler;
  }

  public void drawViewfinder() {
    viewfinderView.drawViewfinder();

  }

  private void initBeepSound() {
    if (playBeep && mediaPlayer == null) {
      setVolumeControlStream(AudioManager.STREAM_MUSIC);
      mediaPlayer = new MediaPlayer();
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mediaPlayer.setOnCompletionListener(beepListener);

      AssetFileDescriptor file = getResources().openRawResourceFd(
          R.raw.beep);
      try {
        mediaPlayer.setDataSource(file.getFileDescriptor(),
            file.getStartOffset(), file.getLength());
        file.close();
        mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
        mediaPlayer.prepare();
      } catch (IOException e) {
        mediaPlayer = null;
      }
    }
  }

  private static final long VIBRATE_DURATION = 200L;

  private void playBeepSoundAndVibrate() {
    if (playBeep && mediaPlayer != null) {
      mediaPlayer.start();
    }
    if (vibrate) {
      Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
      vibrator.vibrate(VIBRATE_DURATION);
    }
  }

  /**
   * When the beep has finished playing, rewind to queue up another one.
   */
  private final OnCompletionListener beepListener = new OnCompletionListener() {
    public void onCompletion(MediaPlayer mediaPlayer) {
      mediaPlayer.seekTo(0);
    }
  };


  @Override
  protected void onResume() {
    super.onResume();
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      initCamera(surfaceHolder);
    } else {
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    decodeFormats = null;
    characterSet = null;

    playBeep = true;
    AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
    if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
      playBeep = false;
    }
    initBeepSound();
    vibrate = true;
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    CameraManager.get().closeDriver();
  }

  @Override
  protected void onDestroy() {
    isPageDestroy = true;
    inactivityTimer.shutdown();
    super.onDestroy();
  }
}