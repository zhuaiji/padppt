package com.weiyou.tamilibox.widget;

import java.io.File;

import com.olivephone.office.TempFileManager;
import com.olivephone.office.powerpoint.DocumentSession;
import com.olivephone.office.powerpoint.DocumentSessionBuilder;
import com.olivephone.office.powerpoint.DocumentSessionStatusListener;
import com.olivephone.office.powerpoint.IMessageProvider;
import com.olivephone.office.powerpoint.ISystemColorProvider;
import com.olivephone.office.powerpoint.android.AndroidMessageProvider;
import com.olivephone.office.powerpoint.android.AndroidSystemColorProvider;
import com.olivephone.office.powerpoint.android.AndroidTempFileStorageProvider;
import com.olivephone.office.powerpoint.view.PersentationView;
import com.olivephone.office.powerpoint.view.SlideShowNavigator;
import com.olivephone.office.powerpoint.view.SlideView;
import com.weiyou.tamilibox.util.CommonUtil;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class PowerPointView extends RelativeLayout implements DocumentSessionStatusListener {

	private final static String TAG = PowerPointView.class.getSimpleName();

	Context ctx;
	private DocumentSession session;
	PersentationView slide;
	Activity act;
	private SlideShowNavigator navitator;
	private int currentSlideNumber;

	public PowerPointView(Context context, AttributeSet attrs) {
		super(context, attrs);
		CommonUtil.LogV(TAG, "zhouqi create PowerPointView");
		ctx = context;
		this.slide = new PersentationView(ctx, attrs);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(-2, -2);
		params.addRule(13);
		addView(this.slide, params);
	}

	public void loadPPT(Activity act, String path) {
		this.act = act;
		try {
			CommonUtil.LogV(TAG, "loadPPT path " + path);
			IMessageProvider msgProvider = new AndroidMessageProvider(this.ctx);
			TempFileManager tmpFileManager = new TempFileManager(new AndroidTempFileStorageProvider(this.ctx));
			ISystemColorProvider sysColorProvider = new AndroidSystemColorProvider();

			this.session = new DocumentSessionBuilder(new File(path)).setMessageProvider(msgProvider)
					.setTempFileManager(tmpFileManager).setSystemColorProvider(sysColorProvider)
					.setSessionStatusListener(this).build();
			this.session.startSession();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDocumentException(Exception arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDocumentReady() {
		this.act.runOnUiThread(new Runnable() {
			public void run() {
				PowerPointView.this.navitator = new SlideShowNavigator(PowerPointView.this.session.getPPTContext());
				PowerPointView.this.currentSlideNumber = (PowerPointView.this.navitator.getFirstSlideNumber() - 1);
				PowerPointView.this.next();
				PowerPointView.this.slide.setVisibility(0);
			}
		});
	}

	private void navigateTo(int slideNumber) {
		SlideView slideShow = this.navitator.navigateToSlide(this.slide.getGraphicsContext(), slideNumber);
		this.slide.setContentView(slideShow);
	}

	private void next() {
		if (this.navitator != null) {
			if (this.navitator.getFirstSlideNumber() + this.navitator.getSlideCount() - 1 > this.currentSlideNumber) {
				navigateTo(++this.currentSlideNumber +1);
			} else {
			}
		}
	}

	@Override
	public void onSessionEnded() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionStarted() {
		// TODO Auto-generated method stub

	}
}
