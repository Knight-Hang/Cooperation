package com.example.administrator.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class OfflineActivity extends AppCompatActivity {
    private WebView web;
    private String xuanke = "";
    private String kechengbiao = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.offline_layout);
        web = (WebView) findViewById(R.id.web);
        web.getSettings().setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
        String temp = "<h1 align=\"center\">当前为离线状态或不处于校内网！</h1>\n" +
                "<p style=\"color:red  \"align=\"center\">\n" +
                "\ttips:你可以点击选课结果或者课程表按钮来查看上次登陆缓存的数据。<br/>当然为了安全，你也可以点击清除缓存以清除保存在本地的个人数据。\n" +
                "</p>";
        web.getSettings().setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
        web.loadData(temp, "text/html; charset=UTF-8", null);//这种写法可以正确解码
        Button button1 = (Button) findViewById(R.id.xuanke);
        Button button2 = (Button) findViewById(R.id.kechengbiao);
        Button button3 = (Button) findViewById(R.id.clear);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 查看缓存的选课结果
                SharedPreferences read = getSharedPreferences("buff", MODE_PRIVATE);
                xuanke = read.getString("xuan ke", "");
                if ("".equals(xuanke))
                    web.loadData("<h1 align=\"center\">系统没有选课结果的缓存！</h1>\n", "text/html; charset=UTF-8", null);
                else
                    web.loadData(xuanke, "text/html; charset=UTF-8", null);//这种写法可以正确解码
                xuanke = "";
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //查看缓存的课程表结果
                SharedPreferences read = getSharedPreferences("buff", MODE_PRIVATE);
                kechengbiao = read.getString("ke cheng biao", "");
                if ("".equals(kechengbiao))
                    web.loadData("<h1 align=\"center\">系统没有课程表的缓存！</h1>\n", "text/html; charset=UTF-8", null);
                else
                    web.loadData(kechengbiao, "text/html; charset=UTF-8", null);//这种写法可以正确解码
                kechengbiao = "";
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences read = getSharedPreferences("buff", MODE_PRIVATE);
                SharedPreferences.Editor editor = read.edit();
                editor.clear();
                editor.apply();
                web.getSettings().setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
                web.loadData("<h1 align=\"center\">删除缓存成功！</h1>\n", "text/html; charset=UTF-8", null);
            }
        });
    }
}
