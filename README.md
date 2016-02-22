# QwmLockDemo
电子锁的操作
#android串口通信——电子锁


[TOC]

##本文解决的问题

1.如何打开电子锁

2.如何判断电子锁是否关闭

##一、锁孔板基础
*锁孔板的图*
![](index_files/12-04-24-80-498589.jpg)

锁孔板中主要说一哈图示中的1和2两个部分：

| 图中编号        | 说明           |
| ------------- |:-------------:|
| 1           | 拨码开关（设置板地址）         |
| 2           | 锁的接口，这个是确定锁的地址的             |

###1.拨码开关（设置板地址）
板地址的设置：拨码开关从 8 到 1 表示从高位到低位，拨到数字端为 1，另一端为 0。 00000001 表示板地址为 1,00000010 表示板地址为 2,00000011 表示板子地址为 3，依次类推。
###3.锁地址
图中2的，每个接口都会有对应的编号，这个就做锁的编号。
那么我们想要操作一把锁，需要知道的是：板编号和锁编号。因为我们是可以接入多个板的。
例如：现在板的的编号是：00000001，锁的编号是 1 ，那么我们想要操作这个锁，就需要使用到 板1锁1来控制。

##二、锁孔板的基本指令
###1、板地址查询0x80： 
```java
命令头 板地址 状态 校验码 (异或)
0X80 0X01 0X00 0X99 0X18
返回:
命令头 固定 从机板地址 固定 校验位
0X80 0X01 0X01到0X40 0X99 XXXX
```


###2、开锁命令如下0x8A： 
```java
命令 板地址 锁地址 状态 校验码 (异或)
0X8A 0X01-0XC8 0X01—18 0X11 xx

如：上位机发 0X8A 0X01 0X01 0X11 0X9B （ 16 进制）， 1 秒后返回
命令 板地址 锁地址 状态 校验码
0X8A 0X01 0X01 0X11 0X9B (锁为开)
0X8A 0X01 0X01 0X00 0X8A (锁为关)

如：上位机发 0X8A 0X02 0X01 0X11 0X98 （ 16 进制），开从控制柜柜门, 1 秒后返回： 
命令 板地址 锁地址 状态 校验码
0X8A 0X02 0X01 0X11 0X98 (锁为开)
0X8A 0X02 0X01 0X00 0X89 (锁为关)
```

###3、读锁状态命令 0X80（门开关状态反馈）：
```java
起始 板地址 锁地址 命令 校验码 (异或)
0X80 0X01-0XC8 0X00—18 0X33 XX

如：上位机发 0X80 0X01 0X01 0X33 0XB3 （ 16 进制），返回
命令 板地址 锁地址 状态 校验码
0X80 0X01 0X01 0X11 0X91 (锁为开)
0X80 0X01 0X01 0X00 0X80 (锁为关)

如：上位机发 0X80 0X01 0X00 0X33 0XB2 （ 16 进制），返回
起始 板地址 状态 1 状态 2 状态 3 状态 4 命令 校验码
0X80 0X01 0XFF 0XFF 0XFF 0XFF 0X33 0XB2
状态:从状态 4 开始到状态 1 低位到高位对应的锁为 1—32.

如：上位机发 0X80 0X02 0X01 0X33 0XB0 （ 16 进制），读从控制柜柜门，返回
命令 板地址 锁地址 状态 校验码
0X80 0X02 0X01 0X11 0X92 (锁为开)
0X80 0X02 0X01 0X00 0X83 (锁为关)

如：上位机发 0X80 0X02 0X00 0X33 0XB1 （ 16 进制），读取从控制柜所有柜门，返回 
起始 板地址 状态 1 状态 2 状态 3 状态 4 命令 校验码
0X80 0X02 0XFF 0XFF 0XFF 0XFF 0X33 0XB1
状态:从状态 4 开始到状态 1 低位到高位对应的锁为 1—32.
```

##三、开锁的控制和关锁的监听
波特率默认是：9600
###1.DevicesUtils 硬件操作类的github地址
硬件的操作类和前面的几篇文章所描述的是一样，获取可以查看项目中的 DevicesUtils：
https://github.com/qiwenming/QwmLockDemo/blob/master/app/src/main/java/com/qwm/qwmlockdemo/utils/DevicesUtils.java

###2.StringUtils 字符串的工具类
```java
package com.qwm.qwmlockdemo.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author qiwenming
 * @creation 2015-6-18 下午5:27:20
 * @instruction 字符串工具
 */
public class StringUtils {

    /**
     * byte数组转为对应的16进制字符串
     * 
     * @param src
     * @return
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * byte数组转为对应的16进制字符串
     * 
     * @param src
     * @return
     */
    public static String bytesToHexString(byte[] src, int length) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || length <= 0) {
            return null;
        }
        for (int i = 0; i < length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * 十六进制编码字符串转为对应的二进制数组
     * 
     * @param s
     * @return
     */
    public static byte[] hexStringToBytes(String s) {
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {

                baKeyword[i] = (byte) (Integer.parseInt(
                        s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return baKeyword;
    }

    /**
     * 十六进制转ascii
     * 
     * @param hex
     * @return
     */
    public static String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        // 49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            // grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            // convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            // convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }

        return sb.toString();
    }

    /**
     * 10进制字符串 转为16进制字符串
     * @param dec
     * @return
     */
    public static String convertDecToHexString(String s) {
        Log.i("", s);
        String str = Integer.toHexString(Integer.parseInt(s));
        if(str.length()%2==1){
            str = "0"+str;
        }
        return str;
    }
    
    /**
     * 通过做异或运算,求出校验码
     * @param cmd
     * @return
     */
    public static String xor(String cmd)
    {
        if(cmd.length()%2!=0){
            cmd = "0"+cmd;
        }
        int result = 0;
        for (int i = 0; i < cmd.length()-1; i=i+2) {
            //System.out.println(cmd.substring(i,i+2));
            result ^= Integer.valueOf(cmd.substring(i, i + 2), 16);
            System.out.println("16-->"+ Integer.valueOf(cmd.substring(i, i + 2), 16));
            System.out.println("result:"+result);
        }
        return Integer.toHexString(result);
    }
    
    /**
     * 以"-"拆分字符串
     * @param str
     * @return
     */
    public static String[] splitString(String str){
        return str.split("-");
    }
    
    public static String takeCity(String str){
        String nstr = null;
        if(str!=null){
            nstr=str.substring(0, str.length()-1);
        }
        return nstr;
    }
    
    /**
     * 时间戳转为日期
     * @param datestr
     * @return
     */
    public static String getSimpDate(String datestr){
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
        String date = sdf.format(new Date(Long.parseLong(datestr) ));
        return date;
    }
    /**
     * 时间戳转为日期
     * @param smdateint
     * @return
     */
    public static String getSimpDate(long smdateint){
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
        String date = sdf.format(new Date(smdateint));
        System.out.println(date);
        return date;
    }
}
```

###3.OpenLockActivity 开锁和关锁监听的操作类
```java
package com.qwm.qwmlockdemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.qwm.qwmlockdemo.bean.LockBean;
import com.qwm.qwmlockdemo.bean.SerialPortSendData;
import com.qwm.qwmlockdemo.utils.DevicesUtils;
import com.qwm.qwmlockdemo.utils.StringUtils;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author qiwenming
 * @date 2016/2/22 0022 下午 1:48
 * @ClassName: OpenLockActivity
 * @ProjectName: 
 * @PackageName: com.qwm.qwmlockdemo
 * @Description: 开锁和关锁的界面
 * 开锁的步骤阐述：
 * 1.获取到需要打开的锁
 * 2.打开锁
 * 3.打开关锁的监听
 */
public class OpenLockActivity  extends AppCompatActivity {
    /**
     * 标题
     */
    private TextView titleTv;
    /**
     * 倒计时
     */
    private TextView timeTv;
    private LockBean lockBean;
    /**
     * 关闭的查询指令
     */
    public String closecmd = "";
    /**
     * 关闭的标志
     */
    private String closeStopStr1;
    private String openStopStr2;
    /**
     * 检查所状态
     */
    private SerialPortSendData data;
    /**
     * 硬件操作类
     */
    private DevicesUtils device;
    /**
     * 倒计时的时间 s
     */
    private int second = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_lock_layout);
//        setContentView(R.layout.open_lock_layout);
        titleTv = (TextView)findViewById(R.id.tv_title);
        timeTv = (TextView)findViewById(R.id.tv_time);
        //获取传递过来的bean
        lockBean = (LockBean) getIntent().getSerializableExtra("lockBean");
        titleTv.setText("板"+lockBean.boardAddStr+"锁"+lockBean.lockAddStr+"打开");
        openBox();
    }

    /**
     * 放回
     * @param view
     */
    public void black(View view){
        finish();
    }


    /**
     * 开锁
     * 读取数据时候  开头的8a 有时候会有 时候没有  这样的处理 我们只能根据结尾的  9b来判断了
     */
    private void openBox() {
        String opencmd = "";
        String comCloseStr = "";
        //拼接指令： 8a(80) + 板编号 + 锁编号 + 11(00)+亦或值
        opencmd = "8a"+StringUtils.convertDecToHexString(lockBean.boardAddStr)+ StringUtils.convertDecToHexString(lockBean.lockAddStr);
        comCloseStr = "80"+StringUtils.convertDecToHexString(lockBean.boardAddStr)+StringUtils.convertDecToHexString(lockBean.lockAddStr);
        closecmd = comCloseStr+"33";

        //计算关闭的校验码
        closeStopStr1 = "00"+StringUtils.xor(comCloseStr+"00");
        openStopStr2 = "11"+StringUtils.xor(comCloseStr+"11");
        Log.i("closeStopStr1------", closeStopStr1);
        Log.i("closeStopStr2------", openStopStr2);
        closecmd+=StringUtils.xor(closecmd);

        //开锁的数据封装
        final String openStopstr = "11"+StringUtils.xor(opencmd+"11");
        final String stopstr1 = "00"+StringUtils.xor(opencmd+"00");
        opencmd+=openStopstr;
        SerialPortSendData sendData = new SerialPortSendData(lockBean.addressStr, Integer.parseInt(lockBean.bauteRateStr), opencmd, "", "",openStopstr, false);
        sendData.stopStr = openStopstr;
        sendData.stopStr1 = stopstr1;

        //检查锁状态的数据封装
        data = new SerialPortSendData(lockBean.addressStr, Integer.parseInt(lockBean.bauteRateStr), closecmd, "", "", closeStopStr1, false);
        data.stopStr = closeStopStr1;
        data.stopStr1 = openStopStr2;
        device = new DevicesUtils();
        //发送指令AAA
        device.toSend(this, sendData, new DevicesUtils.ReciverListener() {
            @Override
            public void onReceived(String receviceStr) {
                Log.i("onReceived_receviceStr", receviceStr);
                countdown();
                if(receviceStr.length()<4){//代表开锁失败
                    mHandler.sendEmptyMessageDelayed(CLOSEBOX, 1000);
                    return;
                }
                String stop = receviceStr.substring(receviceStr.length()-4, receviceStr.length());
                if(stop.equals(openStopstr)){
                    mHandler.sendEmptyMessageDelayed(CLOSEBOX, 1000);
                }
            }

            @Override
            public void onFail(String fialStr) {
            }
            @Override
            public void onErr(Exception e) {
            }

        });
    }

    public final int CLOSEBOX = 101;
    public final int COUNTDOWN = 102;
    /**
     * 用户动态监听
     */
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CLOSEBOX:
                    if(second>1){//去监听锁的状态
                        closeLock();
                    }else{
                        mHandler.removeMessages(CLOSEBOX);
                        mHandler.removeMessages(COUNTDOWN);
                        finish();
                    }
                    break;
                case COUNTDOWN:
                    second--;
                    timeTv.setText(second+"s");
                    if(second<=0&& this!=null){
                        finish();
                    }else{
                        mHandler.sendEmptyMessageDelayed(COUNTDOWN, 1000);
                    }
                    break;
                default:
                    break;
            }
        };
    };

    /**
     * 倒计时
     */
    private void countdown(){
        final Handler handler=new Handler(){
            public void handleMessage(Message msg) {
                timeTv.setText(second+"s");
            };
        };
        final ScheduledExecutorService ses= Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                second-=1;
                handler.sendEmptyMessage(second);
                if(second==0){
                    ses.shutdown();
                    finish();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }


    /**
     * 关锁检测
     *    1.创建一个 handler 来发送数据 读取锁的状态
     *    2.app关箱子
     *    3.后台
     *
     */
    public void closeLock() {
        Log.i("closeBox", "closeBox--------");
        device = new DevicesUtils();
        device.toSend(this, data, new DevicesUtils.ReciverListener() {
            @Override
            public void onReceived(String receviceStr) {
                Log.i("closeBox_receviceStr", receviceStr);
                if (receviceStr.length() < 4) {
                    mHandler.sendEmptyMessageDelayed(CLOSEBOX, 1000);
                    return;
                }
                String stop = receviceStr.substring(receviceStr.length() - 4, receviceStr.length());
                if (stop.equals(closeStopStr1)) {
                    mHandler.removeMessages(CLOSEBOX);
                    mHandler.removeMessages(COUNTDOWN);
                    finish();
                } else {
                    mHandler.sendEmptyMessageDelayed(CLOSEBOX, 1000);
                }
            }

            @Override
            public void onFail(String fialStr) {
                mHandler.sendEmptyMessageDelayed(CLOSEBOX, 1000);
            }

            @Override
            public void onErr(Exception e) {
                mHandler.sendEmptyMessageDelayed(CLOSEBOX, 1000);
            }
        });
    }

    @Override
    protected void onDestroy() {
        mHandler.removeMessages(CLOSEBOX);
        mHandler.removeMessages(COUNTDOWN);
        super.onDestroy();
    }
}

```

##四、图示
![](index_files/1.png)
![](index_files/2.png)
![](index_files/3.png)
##五、源码下载
* https://github.com/qiwenming/QwmLockDemo *




