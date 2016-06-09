package com.weiyou.tamilibox.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.weiyou.tamilibox.BaseActivity;
import com.weiyou.tamilibox.R;

public class PictureViewerActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picture_viewer);
		ImageView imageView= (ImageView)this.findViewById(R.id.imageView);
		Intent intent = this.getIntent();
		Bundle bl = intent.getExtras();
		String path = bl.getString("file_path");
		
        InputStream inputStream = null;  
        try {  
            inputStream = new FileInputStream(path);  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        }  
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);  
        imageView.setImageBitmap(bitmap);
	}
}
