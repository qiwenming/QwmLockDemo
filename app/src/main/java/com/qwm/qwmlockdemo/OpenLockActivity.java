package com.qwm.qwmlockdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.TextView;

/**
 * @author qiwenming
 * @date 2016/2/22 0022 下午 1:48
 * @ClassName: OpenLockActivity
 * @ProjectName: 
 * @PackageName: com.qwm.qwmlockdemo
 * @Description: 开锁和关锁的界面
 */
public class OpenLockActivity  extends Activity{
    /**
     * 标题
     */
    private TextView titleTv;
    /**
     * 倒计时
     */
    private TextView timeTv;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.open_lock_layout);
         titleTv = (TextView)findViewById(R.id.tv_title);
         timeTv = (TextView)findViewById(R.id.tv_time);
    }
}
