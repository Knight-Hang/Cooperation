package com.example.administrator.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    WebView web;
    Button button;
    EditText input;
    String cookie;
    String url;
    String xuanke;
    String kechengbiao;
    String query;
    String QueryNum;

    ArrayList<String> limit = new ArrayList<>();
    ArrayList<String> chosen = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.send);
        input = (EditText) findViewById(R.id.input);
        input.setText("互联网");
        web = (WebView) findViewById(R.id.web);
        Intent intent = getIntent();
        cookie = intent.getStringExtra("cookie");
        url = intent.getStringExtra("url");
        //登陆成功显示页面
        String temp = "<h1 align=\"center\">登陆成功,欢迎!</h1>\n";
        web.getSettings().setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
        web.loadData(temp, "text/html; charset=UTF-8", null);//这种写法可以正确解码
        new Thread(new Runnable() {
            @Override
            public void run() {
                xuanke = getRequest("/showresult.asp");
                kechengbiao = getRequest("/get_schedule.asp?");
            }
        }).start();
        // 查询按钮
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QueryNum = input.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
//                        query = getRequest("/sele_count1.asp?course_no=" + QueryNum);
//                        query = getRequest("/searchcourse.asp?querytype=2&course_no=" + "%BB%A5%C1%AA%CD%F8");//QueryNum;
                        try {
                            query = getRequest("/searchcourse.asp?querytype=2&course_no=" + URLEncoder.encode(QueryNum, "gb2312"));//QueryNum;
                            Log.d("1", "encode: " + URLEncoder.encode(QueryNum, "gb2312"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        // 过滤无关信息，改造表格
                        query = query.replaceAll("备注</td></tr>","备注</td><td>限制数</td><td>已选数</td></tr>");
                        query = query.replaceAll("<td>选课<br>人数</td>", "");
                        query = query.replaceAll("<td><img [\\w\\W]{0,140}></td>","</td>");
                        Log.d("1", "run: " + query);
// 互联网
                        // 查找对应课程的人数
                        String regex = "<td>([a-zA-z0-9]{2,})</td>"; // 课程编号(考虑MOOC的情况，前面有MC两个字母)
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(query);
                        while (matcher.find()) {
                            // 获得人数信息页面
                            String num_status = getRequest("/sele_count1.asp?course_no=" + matcher.group(1));
                            Log.d("1", "课程编号 group1: " + matcher.group(1));
                            Log.d("1", "num page: " + num_status);
                            // 抓取人数信息并保存
                            String num_regex = "<br>限制人数：([\\d]+)&nbsp;&nbsp;已选人数：([\\d]+)</body>";
                            Pattern pattern1 = Pattern.compile(num_regex);
                            Matcher matcher1 = pattern1.matcher(num_status);
                            while (matcher1.find()) {
                                limit.add(matcher1.group(1));
                                chosen.add(matcher1.group(2));
                                Log.d("1", "limit num: " + matcher1.group(1));
                                Log.d("1", "chosen num: " + matcher1.group(2));
                            }
                        }
                        // 将获得的限制人数和已选人数的数据添加到表格
                        int size = limit.size();
                            String[] part = query.split("</tr>");
                            for (String temp : part)
                                Log.d("1", "part: " + temp);
                        for (int i=1; i<part.length-1; i++) {
                            part[i] += "<td>" + limit.get(i-1) + "</td><td>" + chosen.get(i-1) + "</td></tr>";
                        }
                        String new_table = "";
                        for (int i=0; i<part.length; i++) {
                            new_table += part[i];
                        }
                        query = new_table;

                        web.post(new Runnable() {
                            @Override
                            public void run() {
                                web.getSettings().setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
                                web.loadData(query, "text/html; charset=UTF-8", null);//这种写法可以正确解码
                            }
                        });
                    }
                }).start();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // 选课结果
            case R.id.showresult:
                web.loadData(xuanke, "text/html; charset=UTF-8", null);//这种写法可以正确解码
                break;
            // 课程表
            case R.id.kechengbiao:
                web.loadData(kechengbiao, "text/html; charset=UTF-8", null);//这种写法可以正确解码
                break;
            // 选课功能
            case R.id.xuanke:
                Log.d("1", "onOptionsItemSelected: " + query);
//                web.loadData(query, "text/html; charset=UTF-8", null);//这种写法可以正确解码
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 获得相应课程的人数信息
    public String getRequest(String str) {
        try {
            String getshowresult = url + str;
            URL u = new URL(getshowresult);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Cookie", cookie);

            Reader r = new InputStreamReader(connection.getInputStream(), "GB2312");
            int c;
            String html = "";
            while ((c = r.read()) != -1) {
                html += (char) c;
            }
            r.close();
            return html;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
