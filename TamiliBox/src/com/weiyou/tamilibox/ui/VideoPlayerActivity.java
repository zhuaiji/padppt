package com.weiyou.tamilibox.ui;

import java.io.IOException;

import com.weiyou.tamilibox.BaseActivity;
import com.weiyou.tamilibox.R;
import com.weiyou.tamilibox.R.id;
import com.weiyou.tamilibox.R.layout;
import com.weiyou.tamilibox.R.menu;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;

public class VideoPlayerActivity extends BaseActivity implements
		OnCompletionListener, OnErrorListener, OnInfoListener,
		OnPreparedListener, OnSeekCompleteListener, OnVideoSizeChangedListener,
		SurfaceHolder.Callback {
	private Display currDisplay;
	private SurfaceView surfaceView;
	private SurfaceHolder holder;
	private MediaPlayer player;
	private int vWidth, vHeight;

	// private boolean readyToPlay = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_player);

		surfaceView = (SurfaceView) this.findViewById(R.id.video_surface);
		// ��SurfaceView���CallBack����
		holder = surfaceView.getHolder();
		holder.addCallback(this);
		// Ϊ�˿��Բ�����Ƶ����ʹ��CameraԤ����������Ҫָ����Buffer����
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// ���濪ʼʵ����MediaPlayer����
		player = new MediaPlayer();
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
		player.setOnInfoListener(this);
		player.setOnPreparedListener(this);
		player.setOnSeekCompleteListener(this);
		player.setOnVideoSizeChangedListener(this);
		Log.v("Begin:::", "surfaceDestroyed called");
		// Ȼ��ָ����Ҫ�����ļ���·������ʼ��MediaPlayer
		Bundle bl = this.getIntent().getExtras();
		String dataPath = bl.getString("file_path");
		try {
			player.setDataSource(dataPath);
			Log.v("Next:::", "surfaceDestroyed called");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Ȼ������ȡ�õ�ǰDisplay����
		currDisplay = this.getWindowManager().getDefaultDisplay();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// ��Surface�ߴ�Ȳ����ı�ʱ����
		Log.v("Surface Change:::", "surfaceChanged called");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// ��SurfaceView�е�Surface��������ʱ�򱻵���
		// ����������ָ��MediaPlayer�ڵ�ǰ��Surface�н��в���
		player.setDisplay(holder);
		// ��ָ����MediaPlayer���ŵ����������ǾͿ���ʹ��prepare����prepareAsync��׼��������
		player.prepareAsync();

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		Log.v("Surface Destory:::", "surfaceDestroyed called");
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer arg0, int arg1, int arg2) {
		// ��video��С�ı�ʱ����
		// �������������player��source�����ٴ���һ��
		Log.v("Video Size Change", "onVideoSizeChanged called");

	}

	@Override
	public void onSeekComplete(MediaPlayer arg0) {
		// seek�������ʱ����
		Log.v("Seek Completion", "onSeekComplete called");

	}

	@Override
	public void onPrepared(MediaPlayer player) {
		// ��prepare��ɺ󣬸÷������������������ǲ�����Ƶ

		// ����ȡ��video�Ŀ�͸�
		vWidth = player.getVideoWidth();
		vHeight = player.getVideoHeight();

		if (vWidth > currDisplay.getWidth()
				|| vHeight > currDisplay.getHeight()) {
			// ���video�Ŀ���߸߳����˵�ǰ��Ļ�Ĵ�С����Ҫ��������
			float wRatio = (float) vWidth / (float) currDisplay.getWidth();
			float hRatio = (float) vHeight / (float) currDisplay.getHeight();

			// ѡ����һ����������
			float ratio = Math.max(wRatio, hRatio);

			vWidth = (int) Math.ceil((float) vWidth / ratio);
			vHeight = (int) Math.ceil((float) vHeight / ratio);

			// ����surfaceView�Ĳ��ֲ���
			surfaceView.setLayoutParams(new LinearLayout.LayoutParams(vWidth,
					vHeight));

			// Ȼ��ʼ������Ƶ

			player.start();
		}
		player.start();
	}

	@Override
	public boolean onInfo(MediaPlayer player, int whatInfo, int extra) {
		// ��һЩ�ض���Ϣ���ֻ��߾���ʱ����
		switch (whatInfo) {
		case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
			break;
		case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
			break;
		case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
			break;
		case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
			break;
		}
		return false;
	}

	@Override
	public boolean onError(MediaPlayer player, int whatError, int extra) {
		Log.v("Play Error:::", "onError called");
		switch (whatError) {
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			Log.v("Play Error:::", "MEDIA_ERROR_SERVER_DIED");
			break;
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			Log.v("Play Error:::", "MEDIA_ERROR_UNKNOWN");
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer player) {
		// ��MediaPlayer������ɺ󴥷�
		Log.v("Play Over:::", "onComletion called");
		this.finish();
	}
}
