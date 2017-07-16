package com.example.administrator.myapplication;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * OverWrite Webview to get scroll listener
 * 通过自定义接口监听 webview 的滚动事件
 * Created by H.S.H on 2017/7/15.
 */

public class MyWebView extends WebView {
    private OnScrollChangedCallback mOnScrollChangedCallback;

    public MyWebView(final Context context) {
        super(context);
    }

    public MyWebView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public MyWebView(final Context context, final AttributeSet attrs,
                             final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl,
                                   final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (mOnScrollChangedCallback != null) {
            mOnScrollChangedCallback.onScroll(l - oldl, t - oldt);
        }
    }

    public OnScrollChangedCallback getOnScrollChangedCallback() {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(
            final OnScrollChangedCallback onScrollChangedCallback) {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }

    /**
     * Impliment in the activity/fragment/view that you want to listen to the webview
     */
    public static interface OnScrollChangedCallback {
        void onScroll(int dx, int dy);
    }
}
