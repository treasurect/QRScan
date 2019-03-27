package com.treasure.qrscan.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.treasure.qrscan.R;
import com.treasure.qrscan.decoding.RGBLuminanceSource;
import com.treasure.qrscan.encoding.EncodingHandler;
import com.treasure.qrscan.ui.base.BaseActivity;
import com.treasure.qrscan.ui.view.AlertDialog;
import com.treasure.qrscan.utils.GenderChangeListener;
import com.treasure.qrscan.utils.LogUtil;
import com.treasure.qrscan.utils.ToastUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.OnClick;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.UploadFileListener;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.functions.Consumer;

public class QRCreateActivity extends BaseActivity implements GenderChangeListener {

  public static void start(Context context) {
    Intent intent = new Intent(context, QRCreateActivity.class);
    context.startActivity(intent);
  }

  @BindView(R.id.title)
  TextView title;
  @BindView(R.id.back)
  ImageView back;
  @BindView(R.id.edit_qr_create)
  EditText editQRCreate;
  @BindView(R.id.create_text)
  LinearLayout createText;
  @BindView(R.id.create_image)
  LinearLayout createImage;
  @BindView(R.id.create_web)
  LinearLayout createWeb;
  @BindView(R.id.create_video)
  LinearLayout createVideo;
  @BindView(R.id.create_phone)
  LinearLayout createPhone;

  private PopupWindow mPopupWindow;
  private int type = -1;
  private String media_type = "text";
  private AlertDialog alertDialog;
  private String media_url;

  @Override
  protected void loadContentLayout() {
    setContentView(R.layout.activity_qrcreate);
  }

  @Override
  protected void initView() {
    title.setText("二维码生成");
    back.setVisibility(View.GONE);
  }

  @Override
  protected void setListener() {

  }

  @OnClick({R.id.create_text, R.id.create_image, R.id.create_web, R.id.create_video,
      R.id.create_phone, R.id.qr_scan, R.id.qr_scan_album, R.id.qr_create, R.id.btn_qr_create})
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.create_text:
        if (type != 0) {
          editQRCreate.setText("");
        }
        resetBG();
        type = 0;
        createText.setSelected(true);
        editQRCreate.setHint("请输入文本");
        editQRCreate.setInputType(InputType.TYPE_CLASS_TEXT);
        break;
      case R.id.create_image:
        resetBG();
        type = -1;
        showImageAlbum();
        editQRCreate.setInputType(InputType.TYPE_CLASS_TEXT);
        break;
      case R.id.create_web:
        if (type != 2) {
          editQRCreate.setText("");
        }
        resetBG();
        type = 2;
        createWeb.setSelected(true);
        editQRCreate.setHint("请输入网址，https/ftp/http开头");
        editQRCreate.setInputType(InputType.TYPE_CLASS_TEXT);
        break;
      case R.id.create_video:
        resetBG();
        type = -1;
        showVideoAlbum();
        editQRCreate.setInputType(InputType.TYPE_CLASS_TEXT);
        break;
      case R.id.create_phone:
        if (type != 4) {
          editQRCreate.setText("");
        }
        resetBG();
        type = 4;
        createPhone.setSelected(true);
        editQRCreate.setHint("请输入电话号码，11位数");
        editQRCreate.setInputType(InputType.TYPE_CLASS_PHONE);
        break;
      case R.id.qr_scan:
        QRScanActivity.start(this);
        break;
      case R.id.qr_scan_album:
        type = 5;
        showImageAlbum();
        break;
      case R.id.btn_qr_create:
      case R.id.qr_create:
        String trim = editQRCreate.getText().toString().trim();
        if (TextUtils.isEmpty(trim)) {
          ToastUtils.showSingleToast("请输入内容");
          return;
        }
        Bitmap bitmap;
        switch (type) {
          case -1:
            ToastUtils.showSingleToast("请先选择类型");
            break;
          case 0:
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_text);
            getQRCode("text-" + trim, bitmap);
            break;
          case 2:
            Pattern pattern = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");
            Matcher matcher = pattern.matcher(trim);
            if (matcher.matches()) {
              //网址
              bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_web);
              getQRCode("web-" + trim, bitmap);
            } else {
              ToastUtils.showSingleToast("请核对网址");
            }
            break;
          case 4:
            Pattern pattern2 = Pattern.compile("^((13[0-9])|(14[5,7,9])|(15[^4])|(18[0-9])|(17[0,1,3,5,6,7,8]))\\d{8}$");
            Matcher matcher2 = pattern2.matcher(trim);
            if (matcher2.matches()) {
              bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_phone);
              getQRCode("phone-" + trim, bitmap);
            } else {
              ToastUtils.showSingleToast("请输入正确的手机号");
            }
            break;
        }
    }
  }

  /**
   * 扫描二维码图片的方法
   *
   * @param path
   * @return
   */
  public Result scanningImage(String path) {
    if (TextUtils.isEmpty(path)) {
      return null;
    }
    Hashtable<DecodeHintType, String> hints = new Hashtable<>();
    hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); //设置二维码内容的编码

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true; // 先获取原大小
    Bitmap scanBitmap = BitmapFactory.decodeFile(path, options);
    options.inJustDecodeBounds = false; // 获取新的大小
    int sampleSize = (int) (options.outHeight / (float) 200);
    if (sampleSize <= 0)
      sampleSize = 1;
    options.inSampleSize = sampleSize;
    scanBitmap = BitmapFactory.decodeFile(path, options);
    RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
    BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
    QRCodeReader reader = new QRCodeReader();
    try {
      return reader.decode(bitmap1, hints);
    } catch (NotFoundException | ChecksumException | FormatException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void resetBG() {
    createText.setSelected(false);
    createImage.setSelected(false);
    createWeb.setSelected(false);
    createVideo.setSelected(false);
    createPhone.setSelected(false);
  }

  private void showImageAlbum() {
    PictureSelector.create(this)
        .openGallery(PictureMimeType.ofImage())
        .theme(R.style.customPictureStyle)
        .imageSpanCount(3)
        .selectionMode(PictureConfig.SINGLE)
        .previewImage(true)
        .previewVideo(false)
        .isCamera(false)
        .videoQuality(1)
        .compress(true)
        .isGif(true)
        .videoMinSecond(0)
        .videoMaxSecond(15)
        .recordVideoSecond(15)
        .forResult(PictureMimeType.ofAll());
  }

  private void showVideoAlbum() {
    PictureSelector.create(this)
        .openGallery(PictureMimeType.ofVideo())
        .theme(R.style.customPictureStyle)
        .imageSpanCount(3)
        .selectionMode(PictureConfig.SINGLE)
        .previewImage(true)
        .previewVideo(false)
        .isCamera(false)
        .videoQuality(1)
        .compress(true)
        .isGif(true)
        .videoMinSecond(0)
        .videoMaxSecond(15)
        .recordVideoSecond(15)
        .forResult(PictureMimeType.ofAll());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK && requestCode == PictureMimeType.ofAll()) {
      List<LocalMedia> localMediaList = PictureSelector.obtainMultipleResult(data);
      if (localMediaList.size() != 0) {
        LocalMedia localMedia = localMediaList.get(0);
        String path = localMedia.getPath();
        final String pictureType = localMedia.getPictureType();
        showLoading();
        if (type == 5) {
          Result result = scanningImage(path);
          if (result != null) {
            String resultString = result.getText();
            media_type = resultString.split("-")[0];
            if (resultString.length() != media_type.length()) {
              media_url = resultString.substring(media_type.length() + 1);
            } else {
              media_url = resultString;
            }
            if (alertDialog == null) {
              alertDialog = new AlertDialog(this);
              alertDialog.setListener(this);
            }
            alertDialog.show();

            if (media_type.equals("image")) {
              alertDialog.setContent("图片：\n" + media_url);
            } else if (media_type.equals("video")) {
              alertDialog.setContent("视频：\n" + media_url);
            } else if (media_type.equals("web")) {
              alertDialog.setContent("网站：\n" + media_url);
            } else if (media_type.equals("phone")) {
              alertDialog.setContent("是否拨打：\n" + media_url);
            } else {
              alertDialog.setContent("文字：\n" + media_url);
            }
          } else {
            ToastUtils.showSingleToast("二维码识别失败");
          }
          dissLoading();
        } else {
          final BmobFile bmobFile = new BmobFile(new File(path));
          bmobFile.uploadblock(new UploadFileListener() {
            @Override
            public void done(BmobException e) {
              if (e == null) {
                String type = pictureType.split("/")[0];
                Bitmap bitmap;
                if (type.equals("image")) {
                  bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_image);
                } else {
                  bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_video);
                }
                getQRCode(type + "-" + bmobFile.getFileUrl(), bitmap);
              } else {
                ToastUtils.showSingleToast("文件上传失败：" + e.getMessage());
              }
              dissLoading();
            }

            @Override
            public void onProgress(Integer value) {
              super.onProgress(value);
              LogUtil.e("~~~~~~~~~~~~value:" + value);
            }
          });
        }
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
        if (media_type.equals("image")) {
          ScanResultActivity.start(this, media_url, 1);
        } else if (media_type.equals("video")) {
          ScanResultActivity.start(this, media_url, 3);
        } else if (media_type.equals("web")) {
          ScanResultActivity.start(this, media_url, 2);
        } else if (media_type.equals("phone")) {
          Intent intent = new Intent(Intent.ACTION_DIAL);
          Uri data = Uri.parse("tel:" + media_url);
          intent.setData(data);
          startActivity(intent);
        } else {
          ScanResultActivity.start(this, media_url, 0);
        }
        break;
    }
  }

  public void getQRCode(String message, Bitmap bitmap) {
    Bitmap qrCode = EncodingHandler.createQRCode(message, 300, 300, bitmap);
    showUserQRWindow(qrCode);
  }

  private void showUserQRWindow(Bitmap qrCode) {
    View convertView = LayoutInflater.from(this).inflate(R.layout.layout_user_qrscan, null);
    mPopupWindow = new PopupWindow(convertView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
    mPopupWindow.setAnimationStyle(R.style.leaveMesPopupWindow);
    mPopupWindow.setOutsideTouchable(true);
    mPopupWindow.setBackgroundDrawable(new ColorDrawable(0x66000000));
    ImageView qrImg = (ImageView) convertView.findViewById(R.id.qr_image);
    ImageView qrDownload = (ImageView) convertView.findViewById(R.id.qr_download);
    FrameLayout qrLayout = (FrameLayout) convertView.findViewById(R.id.layout);

    if (qrCode != null) {
      qrImg.setImageBitmap(qrCode);
    }
    qrLayout.setOnClickListener(view -> mPopupWindow.dismiss());
    qrDownload.setOnClickListener(view -> {
      if (qrCode != null) toDownLoadImage(qrCode);
    });
    View rootView = LayoutInflater.from(this).inflate(R.layout.activity_qrscan, null);
    mPopupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);
  }

  @SuppressLint("CheckResult")
  private void toDownLoadImage(Bitmap qrCode) {
    Observable.create(new ObservableOnSubscribe<String>() {
      @Override
      public void subscribe(ObservableEmitter<String> emitter) throws Exception {
        String bmpPath = saveBmpToFile(qrCode, System.currentTimeMillis() + "QRScreen");
        emitter.onNext(bmpPath);
        emitter.onComplete();
      }
    }).subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<String>() {
          @Override
          public void accept(String bmpPath) throws Exception {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + bmpPath)));
            ToastUtils.showSingleToast("保存成功，地址：\n" + bmpPath);
          }
        });
  }

  public static String saveBmpToFile(Bitmap bmp, String path) {
    File imageFileName;
    FileOutputStream out;
    File imageFileFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/QRScanCache");
    imageFileFolder.mkdir();
    FileOutputStream out2;
    imageFileName = new File(imageFileFolder, path + ".png");
    try {
      out2 = new FileOutputStream(imageFileName);
      bmp.compress(Bitmap.CompressFormat.PNG, 100, out2);
      out = out2;
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (bmp != null && !bmp.isRecycled()) {
      bmp.recycle();
      bmp = null;
    }
    return imageFileName.getPath();
  }
}
