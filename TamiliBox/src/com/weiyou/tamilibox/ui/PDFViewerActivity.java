package com.weiyou.tamilibox.ui;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.weiyou.tamilibox.BaseActivity;
import com.weiyou.tamilibox.R;
import com.weiyou.tamilibox.R.id;
import com.weiyou.tamilibox.R.layout;
import com.weiyou.tamilibox.R.menu;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PDFViewerActivity extends BaseActivity {
	
	private static final String TAG = PDFViewerActivity.class.getName();
	private PdfiumCore mPdfCore;
	private PdfDocument mPdfDoc = null;
	
    private SurfaceHolder mPdfSurfaceHolder;
    private boolean isSurfaceCreated = false;
    private int mCurrentPageIndex = 0;
    private int mPageCount = 0;
    private final Rect mPageRect = new Rect();
    private final RectF mPageRectF = new RectF();
    private final Rect mScreenRect = new Rect();
    private FileInputStream mDocFileStream = null;
    
    private final ExecutorService mPreLoadPageWorker = Executors.newSingleThreadExecutor();
    private final ExecutorService mRenderPageWorker = Executors.newSingleThreadExecutor();
    private Runnable mRenderRunnable;
    private final Matrix mTransformMatrix = new Matrix();
    private boolean isScaling = false;
    private boolean isReset = true;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pdfviewer);
		mPdfCore = new PdfiumCore(this);
		String path = Environment.getExternalStorageDirectory().getPath()
				+ "/ssadagopan.pdf";
		
        mRenderRunnable = new Runnable() {
            @Override
            public void run() {
                loadPageIfNeed(mCurrentPageIndex);

                resetPageFit(mCurrentPageIndex);
                mPdfCore.renderPage(mPdfDoc, mPdfSurfaceHolder.getSurface(), mCurrentPageIndex,
                        mPageRect.left, mPageRect.top,
                        mPageRect.width(), mPageRect.height());

                mPreLoadPageWorker.submit(new Runnable() {
                    @Override
                    public void run() {
                        loadPageIfNeed(mCurrentPageIndex + 1);
                        loadPageIfNeed(mCurrentPageIndex + 2);
                    }
                });
            }
        };
		
        SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surface);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                isSurfaceCreated = true;
                updateSurface(holder);
                if (mPdfDoc != null) {
                    mRenderPageWorker.submit(mRenderRunnable);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.w(TAG, "Surface Changed");
                updateSurface(holder);
                if(mPdfDoc != null){
                    mRenderPageWorker.submit(mRenderRunnable);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                isSurfaceCreated = false;
                Log.w(TAG, "Surface Destroy");
            }
        });

        try{
        	mDocFileStream = new FileInputStream(path);

            mPdfDoc = mPdfCore.newDocument(mDocFileStream.getFD());
            Log.d("Main", "Open Document");

            mPageCount = mPdfCore.getPageCount(mPdfDoc);
            Log.d(TAG, "Page Count: " + mPageCount);

        }catch(IOException e){
            e.printStackTrace();
            Log.e("Main", "Data uri: ");
        }
	}
	private void resetPageFit(int pageIndex){
        float pageWidth = mPdfCore.getPageWidth(mPdfDoc, pageIndex);
        float pageHeight = mPdfCore.getPageHeight(mPdfDoc, pageIndex);
        float screenWidth = mPdfSurfaceHolder.getSurfaceFrame().width();
        float screenHeight = mPdfSurfaceHolder.getSurfaceFrame().height();

        /**Portrait**/
        if(screenWidth < screenHeight){
            if( (pageWidth / pageHeight) < (screenWidth / screenHeight) ){
                //Situation one: fit height
                pageWidth *= (screenHeight / pageHeight);
                pageHeight = screenHeight;

                mPageRect.top = 0;
                mPageRect.left = (int)(screenWidth - pageWidth) / 2;
                mPageRect.right = (int)(mPageRect.left + pageWidth);
                mPageRect.bottom = (int)pageHeight;
            }else{
                //Situation two: fit width
                pageHeight *= (screenWidth / pageWidth);
                pageWidth = screenWidth;

                mPageRect.left = 0;
                mPageRect.top = (int)(screenHeight - pageHeight) / 2;
                mPageRect.bottom = (int)(mPageRect.top + pageHeight);
                mPageRect.right = (int)pageWidth;
            }
        }else{

            /**Landscape**/
            if( pageWidth > pageHeight ){
                //Situation one: fit height
                pageWidth *= (screenHeight / pageHeight);
                pageHeight = screenHeight;

                mPageRect.top = 0;
                mPageRect.left = (int)(screenWidth - pageWidth) / 2;
                mPageRect.right = (int)(mPageRect.left + pageWidth);
                mPageRect.bottom = (int)pageHeight;
            }else{
                //Situation two: fit width
                pageHeight *= (screenWidth / pageWidth);
                pageWidth = screenWidth;

                mPageRect.left = 0;
                mPageRect.top = 0;
                mPageRect.bottom = (int)(mPageRect.top + pageHeight);
                mPageRect.right = (int)pageWidth;
            }
        }

        isReset = true;
    }
	
    private void loadPageIfNeed(final int pageIndex){
        if( pageIndex >= 0 && pageIndex < mPageCount && !mPdfDoc.hasPage(pageIndex) ){
            Log.d(TAG, "Load page: " + pageIndex);
            mPdfCore.openPage(mPdfDoc, pageIndex);
        }
    }

    private void updateSurface(SurfaceHolder holder){
        mPdfSurfaceHolder = holder;
        mScreenRect.set(holder.getSurfaceFrame());
    }
    
    @Override
    public void onDestroy(){
        try{
            if(mPdfDoc != null && mDocFileStream != null){
                mPdfCore.closeDocument(mPdfDoc);
                Log.d("Main", "Close Document");

                mDocFileStream.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            super.onDestroy();
        }
    }
}
