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
        titleTv = (TextView)findViewById(R.id.tv_board_lock);
        timeTv = (TextView)findViewById(R.id.tv_time);
        //获取传递过来的bean
        lockBean = (LockBean) getIntent().getSerializableExtra("lockBean");
        titleTv.setText("板" + lockBean.boardAddStr + "锁" + lockBean.lockAddStr +"打开");
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
