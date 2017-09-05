package com.example.administrator.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    private Button button;
    private ImageView CodeImg;
    private TextView textView;
    private EditText text1;
    private EditText text2;
    private EditText text3;
    private CheckBox remember;
    private String stu_no = "";
    private String passwd = "";
    private String GetCode = "";
    private String cookie1 = "";
    private String cookie2 = "";
    private String htmlbuffer = "";
    private String url = "http://192.168.240.168/xuanke";
    private File file;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        // 获取组件
        button = (Button) findViewById(R.id.button);
        Button button2 = (Button) findViewById(R.id.button2);
        button.setOnClickListener(this);
        button2.setOnClickListener(this);
        textView = (TextView) findViewById(R.id.show_tips);
        text1 = (EditText) findViewById(R.id.username);
        text2 = (EditText) findViewById(R.id.passwd);
        text3 = (EditText) findViewById(R.id.code);
        CodeImg = (ImageView) findViewById(R.id.image);
        remember = (CheckBox) findViewById(R.id.remember_pass);
        // 申请运行时权限
        if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LoginActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        // 自动填充已保存的账号和密码
        fill_info();
        // 准备验证码并显示
        show_code_image();
        // 点击验证码刷新
        CodeImg.setOnClickListener(this);
    }

    // 权限申请结果反馈
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(LoginActivity.this, "写入权限 √\n请重启应用", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(LoginActivity.this, "写入权限 ×\n程序无法正常使用", Toast.LENGTH_SHORT).show();
                if (grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(LoginActivity.this, "读取权限 √\n请重启应用", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(LoginActivity.this, "读取权限 ×\n程序无法正常使用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     * 具体功能的实现方法
     */
    // 初始化验证码
    private void show_code_image() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 获取cookies
                List<String> cookies = GetHttpResponseHeader();
                if (cookies != null) {
                    cookie1 = cookies.get(0);
                    cookie2 = cookies.get(1);
                    cookie1 = cookie1.substring(0, cookie1.indexOf(';'));
                    cookie2 = cookie2.substring(0, cookie2.indexOf(';'));
                    cookie1 = cookie1 + "; " + cookie2;
                    Log.d("1", "run: " + cookie1);
                    // 获取验证码
                    getImg();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            button.setEnabled(false);
                            Toast.makeText(getApplicationContext(),
                                    "不在内部网环境或者无网络\n仅离线模式可用", Toast.LENGTH_SHORT).show();
                            textView.setText("Tips: \n当前网络环境无法使用在线服务\n仅离线模式可用");
                        }
                    });
                }
            }
        }).start();
    }
    // 自动填写账号和密码
    private void fill_info() {
        SharedPreferences save = getSharedPreferences("data", MODE_PRIVATE);
        String account = save.getString("account", "");
        String password = save.getString("password", "");
        if (!"".equals(account)) {
            text1.setText(account);
            text2.setText(password);
            // 自动勾选保存密码选项
            remember.setChecked(true);
        }
    }
    // 点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                // 获取输入框信息
                stu_no = text1.getText().toString();
                passwd = text2.getText().toString();
                GetCode = text3.getText().toString();
                // 检查保存密码的勾选状态
                SharedPreferences save = getSharedPreferences("data", MODE_PRIVATE);
                SharedPreferences.Editor editor = save.edit();
                if (remember.isChecked()) {
                    editor.putString("account", stu_no);
                    editor.putString("password", passwd);
                } else {
                    editor.clear();
                }
                editor.apply();
                Post(); // post登陆
                // 登陆成功后获取选课信息
                break;
            case R.id.button2:
                // 跳转到离线活动
                Intent intent = new Intent(LoginActivity.this, OfflineActivity.class);
                startActivity(intent);
                break;
            case R.id.image:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getImg();
                    }
                }).start();
                break;
            default:
                break;
        }
    }
    // 发送登陆信息
    void Post() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String PostAdd = url + "/entrance1.asp";
                    URL u = new URL(PostAdd);
                    // post
                    String sendStr = "stu_no=" + stu_no + "&passwd=" + passwd + "&GetCode=" + GetCode;
                    Log.d("1", "run: " + stu_no + " " + passwd + " " + GetCode);

                    HttpURLConnection connection = (HttpURLConnection) u.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setInstanceFollowRedirects(false);   // 自动重定向
                    connection.setRequestProperty("Cookie", cookie1);
                    OutputStream out = connection.getOutputStream();
                    out.write(sendStr.getBytes());
                    out.flush();
                    out.close();
                    Reader r = new InputStreamReader(connection.getInputStream(), "GB2312");
                    int c;
                    htmlbuffer = "";
                    while ((c = r.read()) != -1) {  htmlbuffer += (char) c; }
                    r.close();
                    Looper.prepare();
                    if (htmlbuffer.contains("验证码输入错误")) {
                        Toast toast = Toast.makeText(getBaseContext(), "验证码错误,请重新输入", Toast.LENGTH_SHORT);
                        toast.show();
                        getImg();   // 重新获取验证码
                    } else if (htmlbuffer.contains("密码错")) {
                        Toast toast = Toast.makeText(getBaseContext(), "密码错误，请重新输入", Toast.LENGTH_SHORT);
                        toast.show();
                        getImg();   // 重新获取验证码
                    } else if (htmlbuffer.contains("学号错")) {
                        Toast toast = Toast.makeText(getBaseContext(), "学号错误，请重新输入", Toast.LENGTH_SHORT);
                        toast.show();
                        getImg();   // 重新获取验证码
                    } else {
                        // 跳转到下个活动
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("cookie", cookie1);
                        intent.putExtra("url", url);
                        startActivity(intent);
                    }
                    Looper.loop();
                    Log.d("1", "run: " + htmlbuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    // 获取验证码
    void getImg() {
        try {
            // double a = Math.random();
            String getImg = url + "/captcha.asp";   // 旧版本: "/code.asp?id=!!!&random=" + a;
            URL u = new URL(getImg);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "image/webp,image/*,*/*;q=0.8");
            connection.setRequestProperty("Cookie", cookie1);

            DataInputStream input = new DataInputStream(connection.getInputStream());
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"codeimg.jpg");
            FileOutputStream output = new FileOutputStream(file);
            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.flush();
            output.close();
            input.close();
            CodeImg.post(new Runnable() {
                @Override
                public void run() {
                    CodeImg.setImageURI(null);
                    CodeImg.setImageURI(Uri.fromFile(file));
                    Log.d("1", "img run code");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 获取Cookie
    List<String> GetHttpResponseHeader() {
        try {
            URL obj = new URL("http://192.168.240.168/xuanke/edu_login.asp");
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setConnectTimeout(1000);
            Map<String, List<String>> map = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> header : conn.getHeaderFields().entrySet()) {
                Log.d("1", "Header: " + header.getKey() + "=" + header.getValue());
            }
            return map.get("Set-Cookie");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

