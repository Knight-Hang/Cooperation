package com.example.administrator.myapplication;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.Toast;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private LocalActivityManager lam;
    private WebView web;
    private WebView web2;
    private WebView web3;
    private Button button;
    private AutoCompleteTextView input;
    private Spinner spinner;
    private String cookie;
    private String url;
    private String xuanke="";
    private String kechengbiao="";
    private String query;
    private String QueryStr;
    private String query_encode;
    private String temp_history;
    private String classes = "2015计算机科学与技术01\n" +
            "2015计算机科学与技术02\n" +
            "2015计算机科学与技术03\n" +
            "2015计算机科学与技术04\n" +
            "2015计算机科学与技术05\n" +
            "2015软件工程01\n" +
            "2015软件工程02\n" +
            "2015软件工程03\n" +
            "2015网络工程01\n";
    private int type;

    private List<String> history = new ArrayList<>();
    private ArrayList<Integer> main_limit = new ArrayList<>();
    private ArrayList<Integer> main_chosen = new ArrayList<>();
    private ArrayList<Integer> limit = new ArrayList<>();
    private ArrayList<Integer> chosen = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.send);
        input = (AutoCompleteTextView) findViewById(R.id.input);
        spinner = (Spinner) findViewById(R.id.spinner);
        web = (WebView) findViewById(R.id.web);
        web2 = (WebView) findViewById(R.id.web_view_2);
        web3 = (WebView) findViewById(R.id.web_view_3);
        Intent intent = getIntent();
        cookie = intent.getStringExtra("cookie");
        url = intent.getStringExtra("url");
        // 获取课程表和选课结果
        new Thread(new Runnable() {
            @Override
            public void run() {
                xuanke = getRequest("/showresult.asp");
                kechengbiao = getRequest("/get_schedule.asp?");
                SaveBuff(); // 保存结果到本地
            }
        }).start();
        /* 设置选项卡 */
        lam = new LocalActivityManager(MainActivity.this, false);
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        lam.dispatchCreate(savedInstanceState);
        tabHost.setup(lam);
        // 第1个页面
        TabHost.TabSpec tab1 = tabHost.newTabSpec("tab1")
                .setIndicator("查课")
                .setContent(R.id.tab_1);
        tabHost.addTab(tab1);
        // 第2个页面
        TabHost.TabSpec tab2 = tabHost.newTabSpec("tab2")
                .setIndicator("选课结果")
                .setContent(R.id.tab_2);
        tabHost.addTab(tab2);
        // 第3个页面
        TabHost.TabSpec tab3 = tabHost.newTabSpec("tab3")
                .setIndicator("课程表")
                .setContent(R.id.tab_3);
        tabHost.addTab(tab3);
        // 构造下拉菜单选项
        setSpinner();
        // 登陆成功显示页面
        String temp = "<h1 align=\"center\">登陆成功,欢迎!</h1>\n";
        web.getSettings().setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
        web.loadData(temp, "text/html; charset=UTF-8", null);//这种写法可以正确解码
        // 生成历史纪录显示
        get_history();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, history);
        input.setAdapter(adapter);
        // 加载页面信息
        while (xuanke.equals("")){}
        web2.loadData(xuanke, "text/html; charset=UTF-8", null);
        while (kechengbiao.equals("")){}
        web3.loadData(kechengbiao, "text/html; charset=UTF-8", null);
        // 查询按钮
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QueryStr = input.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            query_encode = URLEncoder.encode(QueryStr, "gb2312");
                            query = getRequest("/searchcourse.asp?querytype=" + type + "&course_no=" + query_encode);//QueryStr;
                            Log.d("1", "encode: " + query_encode);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        // 过滤无关信息，改造表格
                        query = query.replaceAll("备注</td></tr>", "备注</td><td>主选限制数</td><td>主选已选数</td><td>非主选限制数</td><td>非主选已选数</td></tr>");
                        query = query.replaceAll("<td>必<br>修</td><td>选<br>修</td>", "");
                        query = query.replaceAll("<td>选课<br>人数</td>", "");
                        query = query.replaceAll("<td><img [\\w\\W]{0,140}></td>", "</td>");
                        query = query.replaceAll("<td><input [\\w\\W]{0,50}修\"></td>","");
                        Log.d("1", "run: " + query);
                        // 查找对应课程的课程编号
                        String regex = "<td>([a-zA-z0-9]{2,})</td>"; // 课程编号(考虑MOOC的情况，前面有MC两个字母)
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(query);
                        while (matcher.find()) {
                            // 获得人数信息页面
                            String num_status = getRequest("/sele_count1.asp?course_no=" + matcher.group(1));
                            Log.d("1", "课程编号 group1: " + matcher.group(1));
                            Log.d("1", "num page: " + num_status);
                            // 抓取人数信息并保存
                            String num_regex = "<br>主选学生限制人数：([\\d]+)&nbsp;&nbsp;主选学生已选人数：([\\d]+)<br>非主选学生限制人数：([\\d]+)&nbsp;&nbsp;非主选学生已选人数：([\\d]+)</body>";
                            Pattern pattern1 = Pattern.compile(num_regex);
                            Matcher matcher1 = pattern1.matcher(num_status);
                            while (matcher1.find()) {
                                int main_limit_num = Integer.parseInt(matcher1.group(1));
                                int main_chosen_num = Integer.parseInt(matcher1.group(2));
                                int limit_num = Integer.parseInt(matcher1.group(3));
                                int chosen_num = Integer.parseInt(matcher1.group(4));
                                main_limit.add(main_limit_num);
                                main_chosen.add(main_chosen_num);
                                limit.add(limit_num);
                                chosen.add(chosen_num);
                                Log.d("1", "main_limit num: " + main_limit_num);
                                Log.d("1", "main_chosen num: " + main_chosen_num);
                                Log.d("1", "limit num: " + limit_num);
                                Log.d("1", "chosen num: " + chosen_num);
                            }
                        }
                        /* 将获得的限制人数和已选人数的数据添加到表格 */
                        // 分割表格的每一行
                        String[] part = query.split("</tr>");
                        // 将人数信息添加到每一行末尾
                        for (int i = 1; i < part.length - 1; i++) {
                            int main_limit_num = main_limit.get(i - 1);
                            int main_chosen_num = main_chosen.get(i - 1);
                            int limit_num = limit.get(i - 1);
                            int chosen_num = chosen.get(i - 1);
                            part[i] += "<td>" + main_limit_num + "</td><td>" + main_chosen_num + "</td><td>"
                                    + limit_num + "</td><td>" + chosen_num + "</td></tr>";
                            // 高亮显示可选课程
                            if (main_chosen_num < main_limit_num) {
                                part[i] = part[i].replaceAll("<tr>", "<tr bgcolor=\"yellow\">");
                            }
                        }
                        // 将每一行合并
                        String new_table = "";
                        for (String aPart : part) {
                            new_table += aPart;
                        }
                        query = new_table;
                        // 显示新设计好的表格
                        web.post(new Runnable() {
                            @Override
                            public void run() {
                                web.getSettings().setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
                                web.loadData(query, "text/html; charset=UTF-8", null);//这种写法可以正确解码
                            }
                        });
                    }
                }).start();
                // 储存查询词
                if(history.indexOf(QueryStr) == -1) {
                    // 本地化储存
                    SharedPreferences save_history = getSharedPreferences("history", MODE_PRIVATE);
                    SharedPreferences.Editor editor = save_history.edit();
                    temp_history += QueryStr + "\n";
                    editor.putString("history", temp_history);
                    editor.apply();
                    // 刷新列表
                    adapter.add(QueryStr);
                    history.add(QueryStr);
                }
            }
        });
    }
    // 获取历史记录
    private void get_history() {
        SharedPreferences history_data = getSharedPreferences("history", MODE_PRIVATE);
        temp_history = classes;
        temp_history += history_data.getString("history", "");
        if (!"".equals(temp_history)) {
            String[] temp = temp_history.split("\n");
            Collections.addAll(history, temp);
        }
    }
    // 保存离线缓存
    private void SaveBuff() {
        // save xuanke html
        SharedPreferences buff = getSharedPreferences("buff", MODE_PRIVATE);
        SharedPreferences.Editor editor = buff.edit();
        editor.putString("xuan ke", xuanke);
        Log.d("1", "save xuan ke ");
        // save kechengbiao html
        editor.putString("ke cheng biao", kechengbiao);
        editor.apply();
        Log.d("1", "save ke cheng biao ");
    }
    // 菜单点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // 清除历史
            case R.id.clear_history:
                SharedPreferences save_history = getSharedPreferences("history", MODE_PRIVATE);
                SharedPreferences.Editor editor = save_history.edit();
                editor.clear();
                editor.apply();
                // 刷新列表
                history.clear();
                adapter.clear();
                Toast.makeText(this, "查询历史已清除", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return true;
    }
    // 创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    // 创建下拉菜单选项
    private void setSpinner() {
        type = 0;
        final String arr[]=new String[]{"课程编号", "课程名称", "主选班级"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arr);
        spinner.setAdapter(arrayAdapter);
        // 注册事件
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner=(Spinner) parent;
                if(spinner.getItemAtPosition(position)==arr[0])
                    type = 1;
                else if(spinner.getItemAtPosition(position)==arr[1])
                    type = 2;
                else if(spinner.getItemAtPosition(position)==arr[2])
                    type = 4;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    // 获得相应课程的人数信息
    private String getRequest(String str) {
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
    @Override
    protected void onPause() {
        // 重写的OnPause方法必须有，漏掉会错
        lam.dispatchPause(isFinishing());
        super.onPause();
    }
    @Override
    protected void onResume() {
        lam.dispatchResume();
        super.onResume();
    }
}
