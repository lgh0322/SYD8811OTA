package com.example.jackhsueh.ble_ota;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.example.jackhsueh.ble_ota.BleService.bytes2ascii;

public class Blelog_Active extends Activity {

    public static final String TAG = "BLE Test";
    private BleService bleService = null;

    private TextView Status_textView;
    private TextView log_textview;

    private String DeviceMac = null;
    private String DeviceName = null;

    private Button Save_button;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BleService.GATT_CONNECTED.equals(action)) {
                ShowStatus("设备已连接");
            } else if (BleService.GATT_DISCONNECTED.equals(action)) {
                ShowStatus("设备已断开");
            } else if (BleService.ACTION_RSSI_READ.equals(action)) {
                String data = intent.getStringExtra("value");
//              updateDeviceRssi(data);
            } else if (BleService.ACTION_DATA_WRITE.equals(action)) {
                int status = intent.getIntExtra("value", 0);
            } else if (BleService.ACTION_DATA_READ.equals(action)) {
            }else if (BleService.ACTION_DATA_CHANGED.equals(action)) {
                int status = intent.getIntExtra("value", 0);
                byte[] data = intent.getByteArrayExtra("data");
                if(data.length !=0) {
                    append_log(bytes2ascii(data, 0, data.length));
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.GATT_CONNECTED);
        intentFilter.addAction(BleService.GATT_DISCONNECTED);
        intentFilter.addAction(BleService.GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_CHANGE);
        intentFilter.addAction(BleService.ACTION_DATA_READ);
        intentFilter.addAction(BleService.ACTION_DATA_WRITE);
        intentFilter.addAction(BleService.ACTION_RSSI_READ);
        intentFilter.addAction(BleService.ACTION_DATA_CHANGED);
        return intentFilter;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("SYD_Test", "DisplayCtrl service onServiceConnected");
            bleService = ((BleService.LoadcalBinder) service).getService();

            if (!bleService.BlutoothConnectStatue) {
                if (DeviceMac != null) {
                    bleService.connectDevice(DeviceMac);
                    ShowStatus("设备连接中");
                } else {
                    ShowStatus("设备未选中");
                }

            } else {
                ShowStatus("设备已连接");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("SYD_Test", "service onServiceDisconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_blelog);

        Intent intent = getIntent();
        DeviceMac = intent.getStringExtra("DEVICE_MAC");
        DeviceName = intent.getStringExtra("DEVICE_NAME");

        Status_textView = (TextView) findViewById(R.id.Status_textView);
        log_textview= (TextView) findViewById(R.id.log_textview);
        log_textview.setMovementMethod(ScrollingMovementMethod.getInstance());

        intent = new Intent(this, BleService.class);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        boolean a = bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        Log.i("SYD_Test", "onCreate bindService end:" + a);

        Save_button = (Button) findViewById(R.id.Save_button);
        Save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
//                        Send_Ac_commander();
                    }
                }).start();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

//    public void Send_Ac_commander()
//    {
//        byte len=0;
//        byte [] WriteData = new byte[20];
//        byte[] parm1=EditText_parm1.getText().toString().getBytes();
//        byte[] parm2=EditText_parm2.getText().toString().getBytes();
//        byte check_sum=0;
//        WriteData[0] = (byte)0xAC;
//        len +=2;
//        System.arraycopy( parm1, 0,WriteData, len, parm1.length);
//        len += parm1.length;
//        WriteData[len] = 0x7C;
//        len +=1;
//        System.arraycopy(parm2, 0,WriteData, len,  parm2.length);
//        len += parm2.length;
//        WriteData[1] = (byte)(len-2);
//        for(byte i=2;i<len;i++)
//        {
//            check_sum ^= WriteData[i];
//        }
//        WriteData[len] = check_sum;
//        Log.i("SYD_Test","Send_Ac_commander");
//        bleService.sendUartData(WriteData);
//    }

    void ShowStatus(final String status)
    {
        runOnUiThread( new Runnable( )   // 這個執行緒是為了 UI 畫面顯示
        {    @Override
        public void run( )
        {
            Status_textView.setText(status);
        }
        });
    }
    void append_log(final String status)
    {
        runOnUiThread( new Runnable( )   // 這個執行緒是為了 UI 畫面顯示
        {    @Override
        public void run( )
        {
            log_textview.append(status);
            int offset=log_textview.getLineCount()*log_textview.getLineHeight();
            if(offset>(log_textview.getHeight()-log_textview.getLineHeight()))
            {
                log_textview.scrollTo(0,offset-log_textview.getHeight()+log_textview.getLineHeight());
            }
        }
        });
    }

}
