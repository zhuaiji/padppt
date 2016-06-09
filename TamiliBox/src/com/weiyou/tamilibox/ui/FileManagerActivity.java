package com.weiyou.tamilibox.ui;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.weiyou.tamilibox.BaseActivity;
import com.weiyou.tamilibox.R;
import com.weiyou.tamilibox.util.FileTypeUtil;
import com.weiyou.tamilibox.widget.FileDialog;

public class FileManagerActivity extends BaseActivity {
	private static final String TAG = "MyExplorerDemo";
	private static final int REQUEST_EX = 1;
	private String mDir = Environment.getExternalStorageDirectory().getPath();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filemanager);

		Button button = (Button) findViewById(R.id.button);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra("explorer_title",
						getString(R.string.dialog_read_from_dir));
				intent.setDataAndType(Uri.fromFile(new File(mDir)), "*/*");
				intent.setClass(FileManagerActivity.this, FileDialog.class);
				startActivityForResult(intent, REQUEST_EX);
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		String path;
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_EX) {
				Uri uri = intent.getData();
				TextView text = (TextView) findViewById(R.id.text);
				text.setText("select: " + uri);
				// ===如果是ppt文件===
				// if (uri.toString().endsWith("ppt")
				// || uri.toString().endsWith("pptx")) {
				// Intent pptIntent = new Intent();
				// pptIntent.putExtra("file_path", uri.getPath());
				// pptIntent.setClass(FileManagerActivity.this,
				// PowerPointViewerActivity.class);
				// startActivity(pptIntent);
				// }
				// =====如果是視頻文件=======
				if (FileTypeUtil.isVideoFileType(uri.getPath())) {
					Intent mediaIntent = new Intent();
					mediaIntent.putExtra("file_path", uri.getPath());
					mediaIntent.setClass(FileManagerActivity.this,
							VideoPlayerActivity.class);
					startActivity(mediaIntent);
				}
				// 如果是图片文件
				else if (FileTypeUtil.isImageFileType(uri.getPath())) {
					Intent imageIntent = new Intent();
					imageIntent.putExtra("file_path", uri.getPath());
					imageIntent.setClass(FileManagerActivity.this,
							PictureViewerActivity.class);
					startActivity(imageIntent);
				}

			}
		}
	}
}
