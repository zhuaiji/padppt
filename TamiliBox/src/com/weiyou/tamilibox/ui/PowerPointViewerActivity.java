package com.weiyou.tamilibox.ui;

import com.weiyou.tamilibox.BaseActivity;
import com.weiyou.tamilibox.R;
import com.weiyou.tamilibox.util.CommonUtil;
import com.weiyou.tamilibox.widget.PowerPointView;

import android.os.Bundle;
import android.os.Environment;

public class PowerPointViewerActivity extends BaseActivity {

	private final static String TAG = PowerPointViewerActivity.class.getSimpleName();
	private PowerPointView mPptView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_power_point_viewer);
		CommonUtil.LogV(TAG, "zhouqi Create");
		mPptView = (PowerPointView) findViewById(R.id.ppt_view);
		String path = Environment.getExternalStorageDirectory().getPath()
				+ "/ssadagopan.ppt";
		CommonUtil.LogV(TAG, "PowerPointViewerActivity open path " + path);
		mPptView.loadPPT(this, path);
		CommonUtil.LogV(TAG, "finish Create");
	}
}
