package com.example.administrator.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

public class Quit_Course extends AppCompatActivity implements View.OnClickListener{

    private String url;     // example: http://192.168.240.168/xuanke/cancel.asp?stu_no=test&course_no=1502900002
    private String cookie1;
    private String cookie2;

    private EditText course_code_input;
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quit_course);
        // 获取组件
        Button quit = (Button) findViewById(R.id.quit);
        Button cancel = (Button) findViewById(R.id.cancel_quit);
        course_code_input = (EditText) findViewById(R.id.quit_course_code_input);
        web = (WebView) findViewById(R.id.show_quit);
        // 获取传递信息
        Intent intent = getIntent();
        String cookie = intent.getStringExtra("cookie");
        url = intent.getStringExtra("url");
        String[] cookies = cookie.split(";");
        for (int i=0; i < cookies.length; i++) {
            cookies[i] = cookies[i].trim();
            Log.d("1", "split cookie: " + cookies[i]);
        }
        cookie1 = cookies[0];
        cookie2 = cookies[1];
        Log.d("1", "get cookie: " + cookie);
        Log.d("1", "get url: " + url);
        // 设置组件
        quit.setOnClickListener(this);
        cancel.setOnClickListener(this);
        web.getSettings().setDefaultTextEncodingName("GB2312"); // 设置为GB2312
        web.setWebViewClient(new WebViewClient());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.quit) {
            String course_code = course_code_input.getText().toString();
            String target_url = url + "/cancel.asp?stu_no=test&course_no=" + course_code;
            // 设置cookie
            CookieSyncManager.createInstance(Quit_Course.this);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setCookie("http://192.168.240.168/xuanke/", cookie1);  // 特别注意, 每对key-value要单独设置
            cookieManager.setCookie("http://192.168.240.168/xuanke/", cookie2);
            CookieSyncManager.getInstance().sync();
            // 发送请求
            web.loadUrl(target_url);
        } else if (v.getId() == R.id.cancel_quit) {
            finish();
        }
    }
}
