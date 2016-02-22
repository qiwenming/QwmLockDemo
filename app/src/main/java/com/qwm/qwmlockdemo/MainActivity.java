package com.qwm.qwmlockdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qwm.qwmlockdemo.bean.LockBean;
import com.qwm.qwmlockdemo.view.OneColumDialog;

import java.util.Arrays;
import java.util.List;

import android_serialport_api.SerialPortFinder;

/**
 * @author qiwenming
 * @date 2016/2/22 0022 上午 11:41
 * @ClassName: MainActivity
 * @ProjectName:
 * @PackageName: com.qwm.qwmlockdemo
 * @Description: 锁孔板的主Activity
 */
public class MainActivity extends AppCompatActivity {
    /**
     * 设备的地址
     */
    private TextView addressTv;
    /**
     * 设备的波特率
     */
    private TextView bauteRateTv;
    /**
     * 板地址
     */
    private EditText boardAddEdt;
    /**
     * 锁地址
     */
    private EditText lockAddEdt;
    /**
     * 串口
     */
    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        addressTv = (TextView) findViewById(R.id.tv_devices_address);
        bauteRateTv = (TextView) findViewById(R.id.tv_baute_rate);
        boardAddEdt = (EditText) findViewById(R.id.edt_board_address);
        lockAddEdt = (EditText) findViewById(R.id.edt_lock_address);
    }


    /**
     * 获取全部窗口地址
     *
     * @return
     */
    public List<String> getAllDevicesPath() {
        return Arrays.asList(mSerialPortFinder.getAllDevicesPath());
    }

    /**
     * 获取全部 波特率
     *
     * @return
     */
    public List<String> getAllBautRate() {
        return Arrays.asList(getResources().getStringArray(R.array.baudrates));
    }


    /**
     * 选择波特率
     *
     * @param view
     */
    public void selectBauteRate(View view) {
        OneColumDialog dialog = new OneColumDialog(this, getAllBautRate(), new OneColumDialog.SelectListener() {
            @Override
            public void selected(int position, String value) {
                bauteRateTv.setText(value);
            }
        });
        dialog.show();
    }

    /**
     * 选择设备地址
     *
     * @param view
     */
    public void selectAddress(View view) {
        List<String> list = getAllDevicesPath();
        if (list == null || list.size() <= 0) {
            Snackbar.make(view, "木有串口设备哦", Snackbar.LENGTH_SHORT).show();
//            Toast.makeText(this, "木有串口设备哦", Toast.LENGTH_SHORT).show();
            return;
        }
        OneColumDialog dialog = new OneColumDialog(this, list, new OneColumDialog.SelectListener() {
            @Override
            public void selected(int position, String value) {
                addressTv.setText(value);
            }
        });
        dialog.show();
    }


    /**
     * 开锁 调用开锁界面开锁
     * @param view
     */
    public void openLock(View view){
        //硬件地址
        String addressStr = addressTv.getText().toString().trim();
        //波特率
        String bauteRateStr = bauteRateTv.getText().toString().trim();
        //板地址
        String boardAddStr = boardAddEdt.getText().toString().trim();
        //锁地址
        String lockAddStr = lockAddEdt.getText().toString().trim();
        if("".equals(addressStr)){
            Snackbar.make(view,"硬件地址不能为空",Snackbar.LENGTH_SHORT).show();
            return;
        }
        if("".equals(bauteRateStr)){
            Snackbar.make(view,"波特率不能为空",Snackbar.LENGTH_SHORT).show();
            return;
        }
        if("".equals(boardAddStr)){
            Snackbar.make(view,"板地址不能为空",Snackbar.LENGTH_SHORT).show();
            return;
        }
        if("".equals(lockAddStr)){
            Snackbar.make(view,"锁地址不能为空",Snackbar.LENGTH_SHORT).show();
            return;
        }
        LockBean lockBean = new LockBean(addressStr,bauteRateStr,boardAddStr,lockAddStr);

        //打开开锁的Activity
        Intent intent = new Intent(this,OpenLockActivity.class);
        intent.putExtra("lockBean",lockBean);
        startActivity(intent);
    }
}
