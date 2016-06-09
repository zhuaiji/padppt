package com.shockwave.pdfium;

import android.support.v4.util.ArrayMap;

import java.util.Map;

public class PdfDocument {
    public final Object Lock = new Object();
    PdfDocument(){}
    public long mNativeDocPtr;
    final Map<Integer, Long> mNativePagesPtr = new ArrayMap<Integer, Long>();
    public boolean hasPage(int index){ return mNativePagesPtr.containsKey(index);
    }
}
