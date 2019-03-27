package com.treasure.qrscan.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.treasure.qrscan.R;
import com.treasure.qrscan.utils.GenderChangeListener;
import com.treasure.qrscan.utils.ScreenUtils;


/**
 * Created by 18410 on 2017/8/25.
 */

public class AlertDialog extends Dialog {
  private GenderChangeListener listener;
  private Context context;
  private TextView titleTxt;

  public AlertDialog(@NonNull Context context) {
    super(context, R.style.gender_change);
    this.context = context;
  }

  public void setListener(GenderChangeListener listener) {
    this.listener = listener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_alert);
    init();
    Window window = getWindow();
    window.setGravity(Gravity.CENTER);
    window.setWindowAnimations(R.style.gender_change);
    titleTxt = (TextView) findViewById(R.id.text_title);
  }

  private void init() {
    findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        listener.genderClick(R.id.done);
      }
    });

  }

  public void setContent(String title) {
    if (titleTxt != null)
      titleTxt.setText(title);
  }

  public TextView getTitleTxt() {
    return titleTxt;
  }
}
