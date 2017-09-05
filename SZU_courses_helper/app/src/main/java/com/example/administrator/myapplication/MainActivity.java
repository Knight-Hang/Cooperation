package com.example.administrator.myapplication;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 期待加入的功能：
 * 【×】自动登陆SSL VPN(方便非内部网环境登陆)
 * 【√】选课功能(如果人数没有满则正常选课，人数满了则添加至后台时刻监控)
 * 【.】退课功能
 * 【×】抓取课程评价
 * 【√】学分查询
 * 【.】部分课程不能显示问题修复
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LocalActivityManager lam;
    private MyWebView web;
    private WebView web2;
    private WebView web3;
    private AutoCompleteTextView input;
    private Spinner spinner;
    private FloatingActionButton add_course;
    private String cookie;
    private String url;
    private String xuan_ke = "";
    private String ke_cheng_biao = "";
    private String query;
    private String QueryStr;
    private String query_encode;
    private String temp_history;
    private int type;

    private List<String> history = new ArrayList<>();
    private ArrayList<Integer> main_limit = new ArrayList<>();  // 主选限制
    private ArrayList<Integer> main_chosen = new ArrayList<>(); // 主选已选
    private ArrayList<Integer> limit = new ArrayList<>();       // 非主选限制
    private ArrayList<Integer> chosen = new ArrayList<>();      // 非主选已选
    private ArrayAdapter<String> adapter;

    private final String classes = "2015计算机科学与技术01\n" +
            "2015计算机科学与技术02\n" +
            "2015计算机科学与技术03\n" +
            "2015计算机科学与技术04\n" +
            "2015计算机科学与技术05\n" +
            "2015软件工程01\n" +
            "2015软件工程02\n" +
            "2015软件工程03\n" +
            "2015网络工程01\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 获取组件
        Button button = (Button) findViewById(R.id.send);
        add_course = (FloatingActionButton) findViewById(R.id.fb_add);
        FloatingActionButton remove_course = (FloatingActionButton) findViewById(R.id.fb_delete);
        input = (AutoCompleteTextView) findViewById(R.id.input);
        spinner = (Spinner) findViewById(R.id.spinner);
        web = (MyWebView) findViewById(R.id.web);
        web2 = (WebView) findViewById(R.id.web_view_2);
        web3 = (WebView) findViewById(R.id.web_view_3);
        WebView web4 = (WebView) findViewById(R.id.web_view_4);
        // 获取传递信息
        Intent intent = getIntent();
        cookie = intent.getStringExtra("cookie");
        url = intent.getStringExtra("url");
        // 获取课程表和选课结果
        get_course_table();
        // 选项卡初始化
        lam = new LocalActivityManager(MainActivity.this, false);
        lam.dispatchCreate(savedInstanceState);
        initial_tabHost();
        // 构造下拉菜单选项
        setSpinner();
        // 登陆成功显示页面
        String temp = "<h1 align=\"center\">登陆成功,欢迎!</h1>\n";
        web.getSettings().setDefaultTextEncodingName("UTF-8");  // 设置默认为utf-8
        web.loadData(temp, "text/html; charset=UTF-8", null);   // 这种写法可以正确解码
        // 获取之前储存的历史纪录
        get_history();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, history);
        input.setAdapter(adapter);
        // 加载2、3、4选项卡的页面信息
        while (xuan_ke.equals("")) {
        }         // 等待页面获取
        web2.loadData(xuan_ke, "text/html; charset=UTF-8", null);
        while (ke_cheng_biao.equals("")) {
        }   // 等待页面获取
        web3.loadData(ke_cheng_biao, "text/html; charset=UTF-8", null);
        web4.getSettings().setJavaScriptEnabled(true);
        web4.setWebViewClient(new WebViewClient());
        web4.loadUrl("http://sdt.jsmmzz.com/xuefen/index.php"); // 加载查询学分的网页
        // http://www.szu.me/kc/index.php  课程评价网页
        // 监听事件
        button.setOnClickListener(this);
        add_course.setOnClickListener(this);
        remove_course.setOnClickListener(this);
        // webview滚动监听事件,当webview滚动时隐藏悬浮按钮,停止滚动后300ms显示
        web.setOnScrollChangedCallback(new MyWebView.OnScrollChangedCallback() {
            @Override
            public void onScroll(int dx, int dy) {
                add_course.hide();
                Timer timer = new Timer(true);
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        add_course.show();
                        Looper.loop();
                    }
                };
                timer.schedule(task, 300);  // 300ms后执行延时任务
            }
        });
    }

    /*
     * 具体功能的实现方法
     */
    // 修改课程信息结果表格
    private void modify_table() {
        // 过滤无关信息，改造表格
        query = query.replaceAll("备注</td></tr>", "备注</td><td>主选限制数</td><td>主选已选数</td><td>非主选限制数</td><td>非主选已选数</td></tr>");
        query = query.replaceAll("<td>必<br>修</td><td>选<br>修</td>", "");
        query = query.replaceAll("<td>选课<br>人数</td>", "");
        query = query.replaceAll("<td><img [\\w\\W]{0,140}></td>", "</td>");    // 删除小望远镜图片
        query = query.replaceAll("<td><input [\\w\\W]{0,50}修\"></td>", "");
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
            /* 判断人数信息种类start    [2种: 4个数字\2个数字] */
            String info_type_regex = "已选";
            Pattern pattern0 = Pattern.compile(info_type_regex);
            Matcher matcher0 = pattern0.matcher(num_status);
            int count = 0;
            while (matcher0.find())
                count++;
            Log.d("1", "count: " + count);
            if (count == 2) {
            /* 判断人数信息种类end */
                // 抓取人数信息
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
            /* 判断人数信息种类start */
            }
            else if (count == 1) {
                // 抓取人数信息
                String num_regex = "<br>限制人数：([\\d]+)&nbsp;&nbsp;已选人数：([\\d]+)</body>";
                Pattern pattern1 = Pattern.compile(num_regex);
                Matcher matcher1 = pattern1.matcher(num_status);
                while (matcher1.find()) {
                    int limit_num = Integer.parseInt(matcher1.group(1));
                    int chosen_num = Integer.parseInt(matcher1.group(2));
                    main_limit.add(limit_num);
                    main_chosen.add(chosen_num);
                    Log.d("1", "limit num: " + limit_num);
                    Log.d("1", "chosen num: " + chosen_num);
                }
            }
            else
                Log.d("1", "count error: ");
            /* 判断人数信息种类end */
        }
        // 分割表格的每一行
        String[] part = query.split("</tr>");
        // 将人数信息添加到每一行末尾
        for (int i = 1; i < part.length - 1; i++) {
            int main_limit_num = main_limit.get(i - 1);
            int main_chosen_num = main_chosen.get(i - 1);
            int limit_num;
            int chosen_num;
            try {
                limit_num = limit.get(i - 1);
                chosen_num = chosen.get(i - 1);
            } catch (Exception e) {
                // 选课结束后人数信息将会变化
                // 从主选人数、主选已选人数、非主选人数、非主选已选人数4项
                // 变成限制人数、已选人数2项
                // 或者MOOC的人数, 也是只显示两项
                // 导致正则匹配不成功从而触发ArrayList的越界异常
                limit_num = 0;
                chosen_num = 0;
            }
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
    }

    // 获取历史记录
    private void get_history() {
        SharedPreferences history_data = getSharedPreferences("history", MODE_PRIVATE);
        String get_storage = history_data.getString("history", "");
        if (get_storage.contains(classes))
            temp_history = get_storage;
        else
            temp_history = get_storage + classes;
        String[] temp = temp_history.split("\n");
        Collections.addAll(history, temp);
    }

    // 获取课程表、选课结果
    private void get_course_table() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                xuan_ke = getRequest("/showresult.asp");
                ke_cheng_biao = getRequest("/get_schedule.asp?");
                // 保存结果到本地，成为离线缓存
                SharedPreferences buff = getSharedPreferences("buff", MODE_PRIVATE);
                SharedPreferences.Editor editor = buff.edit();
                editor.putString("xuan ke", xuan_ke);
                Log.d("1", "save xuan ke ");
                // save ke_cheng_biao html
                editor.putString("ke cheng biao", ke_cheng_biao);
                editor.apply();
                Log.d("1", "save ke cheng biao ");
            }
        }).start();
    }

    // 获得相应课程的人数信息
    private String getRequest(String keyword) {
        try {
            String get_result = url + keyword;
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


    /*
     * 菜单、下拉列表、选项卡等组件的初始化
     * 一些方法的重写
     */
    // 创建选项卡
    private void initial_tabHost() {
        final TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(lam);
        // 第1个页面
        TabHost.TabSpec tab1 = tabHost.newTabSpec("tab1")
                .setIndicator("课程查询")
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
        // 第4个页面
        TabHost.TabSpec tab4 = tabHost.newTabSpec("tab4")
                .setIndicator("学分查询")
                .setContent(R.id.tab_4);
        tabHost.addTab(tab4);
        /* 点击选项卡刷新选课结果和课程表 */
        tabHost.getTabWidget().getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabHost.setCurrentTab(1);
                xuan_ke = "";
                ke_cheng_biao = "";
                get_course_table();
                while (xuan_ke.equals("")) {}         // 等待页面获取
                web2.loadData(xuan_ke, "text/html; charset=UTF-8", null);
                while (ke_cheng_biao.equals("")) {}   // 等待页面获取
                web3.loadData(ke_cheng_biao, "text/html; charset=UTF-8", null);
                Toast.makeText(MainActivity.this, "页面刷新", Toast.LENGTH_SHORT).show();
            }
        }); // 设置监听事件
        tabHost.getTabWidget().getChildAt(2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabHost.setCurrentTab(2);
                xuan_ke = "";
                ke_cheng_biao = "";
                get_course_table();
                while (xuan_ke.equals("")) {}         // 等待页面获取
                web2.loadData(xuan_ke, "text/html; charset=UTF-8", null);
                while (ke_cheng_biao.equals("")) {}   // 等待页面获取
                web3.loadData(ke_cheng_biao, "text/html; charset=UTF-8", null);
                Toast.makeText(MainActivity.this, "页面刷新", Toast.LENGTH_SHORT).show();
            }
        }); // 设置监听事件
        /* 点击选项卡刷新选课结果和课程表 */
    }

    // 创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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

    // 创建下拉菜单选项
    private void setSpinner() {
        type = 0;
        // 注册事件
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                type = position + 1;
                if (position == 2)
                    type++;
                Log.d("1", "onItemSelected type: " + type);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onPause() {
        // 重写的OnPause方法必须有，漏掉会错
        lam.dispatchPause(isFinishing());
        super.onPause();
    }

    @Override
    protected void onResume() {
        // 同上
        lam.dispatchResume();
        super.onResume();
    }

    // 点击事件
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fb_add) {
            // Toast.makeText(MainActivity.this, "选课按钮,敬请期待...", Toast.LENGTH_SHORT).show();
            /* 选课功能 */
            // 跳转到下个活动
            Intent intent = new Intent(MainActivity.this, Choose_Course.class);
            intent.putExtra("cookie", cookie);
            intent.putExtra("url", url);
            startActivity(intent);
            /* 选课功能end */
        } else if (v.getId() == R.id.fb_delete) {
            // Toast.makeText(MainActivity.this, "退课按钮,敬请期待...", Toast.LENGTH_SHORT).show();
            /* 退课功能 */
            Intent intent = new Intent(MainActivity.this, Quit_Course.class);
            intent.putExtra("cookie", cookie);
            intent.putExtra("url", url);
            startActivity(intent);
            /* 退课功能 */
        } else if (v.getId() == R.id.send) {
            // 清除之前储存的信息
            main_limit.clear();
            main_chosen.clear();
            limit.clear();
            chosen.clear();
            QueryStr = input.getText().toString();
            // 获取查询结果
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
                    // 修改表格
                    modify_table();
                    // 显示新设计好的表格
                    web.post(new Runnable() {
                        @Override
                        public void run() {
                            web.getSettings().setDefaultTextEncodingName("UTF-8");// 设置默认为utf-8
                            web.loadData(query, "text/html; charset=UTF-8", null);// 这种写法可以正确解码
                        }
                    });
                }
            }).start();
            // 储存查询词
            if (history.indexOf(QueryStr) == -1) {
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
    }
}
