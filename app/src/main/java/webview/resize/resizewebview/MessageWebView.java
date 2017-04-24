/*
 * Copyright (C) 2013 Google Inc.
 * Licensed to The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package webview.resize.resizewebview;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

import webview.resize.resizewebview.utils.Clock;
import webview.resize.resizewebview.utils.LogTag;
import webview.resize.resizewebview.utils.LogUtils;
import webview.resize.resizewebview.utils.Throttle;


/**
 * A WebView designed to live within a {@link MessageScrollView}.
 */
public class MessageWebView extends WebView implements MessageScrollView.Touchable {

    private static final String LOG_TAG = LogTag.getLogTag();

    private static Handler sMainThreadHandler;

    private boolean mTouched;

    private static final int MIN_RESIZE_INTERVAL = 200;
    private static final int MAX_RESIZE_INTERVAL = 300;
    private final Clock mClock = Clock.INSTANCE;

    private final Throttle mThrottle = new Throttle("MessageWebView",
            new Runnable() {
                @Override public void run() {
                    performSizeChangeDelayed();
                }
            }, getMainThreadHandler(),
            MIN_RESIZE_INTERVAL, MAX_RESIZE_INTERVAL);

    private int mRealWidth;
    private int mRealHeight;
    private boolean mIgnoreNext;
    private long mLastSizeChangeTime = -1;
    protected MessageScrollView mRoot;

    public void setRootView(MessageScrollView view) {
        mRoot = view;
    }

    public MessageWebView(Context c) {
        this(c, null);
    }

    public MessageWebView(Context c, AttributeSet attrs) {
        super(c, attrs);
        getSettings().setSupportZoom(true);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
    }

    @Override
    public boolean wasTouched() {
        return mTouched;
    }

    @Override
    public void clearTouched() {
        mTouched = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mTouched = true;
        final boolean handled = super.onTouchEvent(event);
        LogUtils.d(MessageScrollView.LOG_TAG,"OUT WebView.onTouch, returning handled=%s ev=%s",
                handled, event);
        return handled;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        mRealWidth = w;
        mRealHeight = h;
        final long now = mClock.getTime();
        boolean recentlySized = (now - mLastSizeChangeTime < MIN_RESIZE_INTERVAL);

        // It's known that the previous resize event may cause a resize event immediately. If
        // this happens sufficiently close to the last resize event, drop it on the floor.
        if (mIgnoreNext) {
            mIgnoreNext = false;
            if (recentlySized) {
                    LogUtils.w(LOG_TAG, "Suppressing size change in MessageWebView");
                return;
            }
        }

        if (recentlySized) {
            mThrottle.onEvent();
        } else {
            // It's been a sufficiently long time - just perform the resize as normal. This should
            // be the normal code path.
            performSizeChange(ow, oh);
        }
    }

    private void performSizeChange(int ow, final int oh) {
        super.onSizeChanged(mRealWidth, mRealHeight, ow, oh);
        mLastSizeChangeTime = mClock.getTime();
        if (mRealHeight != oh ) {
            final int scrollY = mRoot.getScrollY();
            mRoot.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int distance = mRealHeight - oh;
                    if (distance > 0) {
                        if (scrollY > 100) {
                            LogUtils.d(LOG_TAG, scrollY+"'");
                            distance *= 0.8;
                            mRoot.scrollBy(0, distance);
                        }
                    }
                    else {
                        distance *= 0.6;
                        LogUtils.d(LOG_TAG, distance+"'");
                        mRoot.scrollBy(0, distance);
                    }
                }
            }, 300);
        }
    }

    private void performSizeChangeDelayed() {
        mIgnoreNext = true;
        performSizeChange(getWidth(), getHeight());
    }

    /**
     * @return a {@link Handler} tied to the main thread.
     */
    public static Handler getMainThreadHandler() {
        if (sMainThreadHandler == null) {
            // No need to synchronize -- it's okay to create an extra Handler, which will be used
            // only once and then thrown away.
            sMainThreadHandler = new Handler(Looper.getMainLooper());
        }
        return sMainThreadHandler;
    }
}
