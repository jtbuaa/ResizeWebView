package webview.resize.resizewebview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private MessageScrollView mScrollView;
    private MessageWebView mMessageWeb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resize_web);

        mScrollView = (MessageScrollView) findViewById(R.id.message_scrollview);
        mMessageWeb = (MessageWebView) findViewById(R.id.reading_webview);
        mScrollView.setInnerScrollableView(mMessageWeb);
        mMessageWeb.setRootView(mScrollView);

        mMessageWeb.loadUrl("file:///android_asset/parse.html");
    }
}
