package com.example.administrator.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* 需要完善的地方:
 * [1] 没有空位的课程添加后台监控
 */
public class Choose_Course extends AppCompatActivity implements View.OnClickListener {

    private RadioButton required_select;
    private ImageView code_image;
    private EditText course_id_input;
    private EditText code_input;
    private WebView web;
    private File file;

    private String url;
    private String cookie;
    private String cookie1;
    private String cookie2;
    private String queryStr;
    private String code_str;
    private static final String required_encode = "%B1%D8%D0%DE"; // "必修"编码后的字符串[GB2312]
    private static final String elective_encode = "%D1%A1%D0%DE"; // "选修"编码后的字符串[GB2312]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("开始选课");
        setContentView(R.layout.choose);
        // 获取传递信息
        Intent intent = getIntent();
        cookie = intent.getStringExtra("cookie");
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
        // 获取组件
        required_select = (RadioButton) findViewById(R.id.requiered);
        Button ensure = (Button) findViewById(R.id.ensure);
        Button cancel = (Button) findViewById(R.id.cancel);
        code_image = (ImageView) findViewById(R.id.image_code);
        course_id_input = (EditText) findViewById(R.id.course_code_input);
        code_input = (EditText) findViewById(R.id.choose_code_input);
        web = (WebView) findViewById(R.id.show_result);
        // 获取验证码
        getImg();
        // 组件设置
        web.getSettings().setDefaultTextEncodingName("GB2312"); // 设置为GB2312
        web.setWebViewClient(new WebViewClient());
        ensure.setOnClickListener(this);
        cancel.setOnClickListener(this);
        code_image.setOnClickListener(this);
    }

    // 获取验证码
    void getImg() {
        /* 尝试向头部信息添加refer
        String test_url = "http://192.168.240.168/xuanke/choosecheck.asp?stu_no=test&no_type=";
        String course_num = course_id_input.getText().toString();
        String type;
        if (required_select.isChecked())
            type = required_encode;
        else
            type = elective_encode;
        final String refer = test_url + course_num + "+" + type;
        */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // double a = Math.random();
                    String getImg = url + "/code.asp";
                    Log.d("1", "getImg: " + getImg);
                    URL u = new URL(getImg);
                    HttpURLConnection connection = (HttpURLConnection) u.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "image/webp,image/*,*/*;q=0.8");
                    connection.setRequestProperty("Cookie", cookie);
                    /* 尝试向头部信息添加refer
                    connection.setRequestProperty("Referer", refer);
                    Log.d("1", "code referer: " + refer);
                    */

                    DataInputStream input = new DataInputStream(connection.getInputStream());
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "codeimg1.jpg");
                    FileOutputStream output = new FileOutputStream(file);
                    byte[] b = new byte[1024];
                    int len;
                    while ((len = input.read(b)) != -1) {
                        output.write(b, 0, len);
                    }
                    output.flush();
                    output.close();
                    input.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            code_image.setImageURI(null);
                            code_image.setImageURI(Uri.fromFile(file));
                            Log.d("1", "img1 run code");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ensure) {
            queryStr = course_id_input.getText().toString();
            code_str = code_input.getText().toString();
            String course_type;
            if (required_select.isChecked())
                course_type = required_encode;
            else
                course_type = elective_encode;
            Log.d("1", "course type: " + course_type);
            is_select(queryStr, course_type);    // 查询是否可选该课程
        } else if (v.getId() == R.id.cancel) {
            finish();
        } else if (v.getId() == R.id.image_code) {
            getImg();
        }
    }


    void is_select(final String str, final String type) {
        // 抓取人数信息
        new Thread(new Runnable() {
            @Override
            public void run() {
                String num_status1;
                num_status1 = getRequest("/sele_count1.asp?course_no=" + str);
                /* 判断人数信息种类start    [2种: 4个数字\2个数字] */
                String info_type_regex = "已选";
                Pattern pattern0 = Pattern.compile(info_type_regex);
                Matcher matcher0 = pattern0.matcher(num_status1);
                int count = 0;
                while (matcher0.find())
                    count++;
                Log.d("1", "count: " + count);
                if (count == 2) {
                /* 判断人数信息种类end */
                Log.d("1", "run getrequest: " + num_status1);
                String num_regex = "<br>主选学生限制人数：([\\d]+)&nbsp;&nbsp;主选学生已选人数：([\\d]+)<br>非主选学生限制人数：([\\d]+)&nbsp;&nbsp;非主选学生已选人数：([\\d]+)</body>";
                Pattern pattern1 = Pattern.compile(num_regex);
                Matcher matcher1 = pattern1.matcher(num_status1);
                while (matcher1.find()) {
                    int main_limit_num = Integer.parseInt(matcher1.group(1));
                    int main_chosen_num = Integer.parseInt(matcher1.group(2));
                    int limit_num = Integer.parseInt(matcher1.group(3));
                    int chosen_num = Integer.parseInt(matcher1.group(4));
                    Log.d("1", "main_limit num: " + main_limit_num);
                    Log.d("1", "main_chosen num: " + main_chosen_num);
                    Log.d("1", "limit num: " + limit_num);
                    Log.d("1", "chosen num: " + chosen_num);
                    if (main_limit_num > main_chosen_num || limit_num > chosen_num) {    // 主选or非主选有空位
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                send_require(type);
                            }
                        });
                    } else {    // 没有空位
                        Looper.prepare();
                        Toast.makeText(Choose_Course.this, "还没有空位...", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                }
                /* 判断人数信息种类start */
                }
                else if (count == 1) {
                    // 抓取人数信息
                    String num_regex = "<br>限制人数：([\\d]+)&nbsp;&nbsp;已选人数：([\\d]+)</body>";
                    Pattern pattern1 = Pattern.compile(num_regex);
                    Matcher matcher1 = pattern1.matcher(num_status1);
                    while (matcher1.find()) {
                        int limit_num = Integer.parseInt(matcher1.group(1));
                        int chosen_num = Integer.parseInt(matcher1.group(2));
                        Log.d("1", "limit num: " + limit_num);
                        Log.d("1", "chosen num: " + chosen_num);
                        if (limit_num > chosen_num) {    // 有空位
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    send_require(type);
                                }
                            });
                        } else {    // 没有空位
                            Looper.prepare();
                            Toast.makeText(Choose_Course.this, "还没有空位...", Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }
                    }
                }
                else
                    Log.d("1", "count error: ");
            /* 判断人数信息种类end */
            }
        }).start();
    }

    // 发送选课请求
    void send_require(String type) {
        // 设置头部信息
//        Map<String, String> referer = new HashMap<>();
//        referer.put("Referer", "http://192.168.240.168/xuanke/choosecheck.asp?stu_no=test&no_type=" + queryStr + "+" + type);
        web.loadUrl(null);
        String target_url = url + "/choose.asp?GetCode=" + code_str + "&no_type=" + queryStr
                + "+" + type + "&submit=%C8%B7++++%B6%A8";
        // 设置cookie
        CookieSyncManager.createInstance(Choose_Course.this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie("http://192.168.240.168/xuanke/", cookie1);  // 特别注意, 每对key-value要单独设置
        cookieManager.setCookie("http://192.168.240.168/xuanke/", cookie2);
        CookieSyncManager.getInstance().sync();
        // 发送请求
//        web.loadUrl(target_url, referer);
        web.loadUrl(target_url);
        Log.d("1", "send_require target url:\n" + target_url);
    }

    // 获得相应课程的人数信息
    private String getRequest(String keyword) {
        try {
            String get_result = url + keyword;
            Log.d("1", "getRequest: " + get_result);
            URL u = new URL(get_result);
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
            e.printStackTrace();
        }
        return "";
    }
}
