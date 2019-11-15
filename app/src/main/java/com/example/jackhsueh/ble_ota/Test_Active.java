package com.example.jackhsueh.ble_ota;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.example.jackhsueh.ble_ota.BleService.bytes2ascii;
import android.widget.Toast;

public class Test_Active extends Activity {

    public static final String TAG = "BLE Test";
    private BleService bleService = null;

    private TextView Status_textView;
    private EditText editText_data;
    private TextView log_textview;

    private String DeviceMac = null;
    private String DeviceName = null;

    private Button Send_button;

    private Spinner spinner_datatype = null;
    private int spinner_datatype_sel = 0;
    private boolean spinner_datatype_isInitial = true;


    byte[] ReadData = null;
    private int SendPacketID = 0;
    private int SendPacketAllNum = 0;
    private int actionType = 0;
    final byte ACTIONTYPE_SendData_Doing = 0x01;
    final byte ACTIONTYPE_SendData_End = 0x01;

    private ClipboardManager cm;
    private ClipData mClipData;

    private static String byte2hexstr(byte [] buffer,String separator){
        String h = "";

        for(int i = 0; i < buffer.length; i++){
            String temp = Integer.toHexString(buffer[i] & 0xFF);
            if(temp.length() == 1){
                temp = "0" + temp;
            }
            if(i==0)
            {
                h = h + temp;
            }
            else
            {
                h = h + separator+ temp;
            }
        }

        return h;

    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Log.i("SYD_OTA","BroadcastReceiver:" +action);
            if (BleService.GATT_CONNECTED.equals(action)) {
                ShowStatus("设备已连接");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");// HH:mm:ss
                //获取当前时间
                Date date = new Date(System.currentTimeMillis());
                append_log(simpleDateFormat.format(date)+" " + DeviceName+" ("+DeviceMac+") ");
            } else if (BleService.GATT_DISCONNECTED.equals(action)) {
                ShowStatus("设备已断开");
            } else if (BleService.ACTION_RSSI_READ.equals(action)) {
                String data = intent.getStringExtra("value");
//              updateDeviceRssi(data);
            } else if (BleService.ACTION_DATA_WRITE.equals(action)) {
                int status = intent.getIntExtra("value", 0);
                if(actionType==ACTIONTYPE_SendData_Doing)
                {
                    int srcPos = SendPacketID * 20;

                    if (status == 0) {
                        if (SendPacketID == SendPacketAllNum) {
                            actionType = 0;
                        } else {
                            byte[] dataPacket = null;
                            if (SendPacketID == (SendPacketAllNum - 1)) {
                                dataPacket=new byte[ReadData.length - srcPos];
                                System.arraycopy(ReadData, srcPos, dataPacket, 0, (ReadData.length - srcPos));//last a packet
                                actionType = ACTIONTYPE_SendData_End;//发送完最后一包了
                            } else {
                                dataPacket=new byte[20];
                                System.arraycopy(ReadData, srcPos, dataPacket, 0, 20);//other packet except first and last packet
                            }
                            bleService.sendUartData(dataPacket);
                            SendPacketID += 1;
                        }
                    }
                }
            } else if (BleService.ACTION_DATA_READ.equals(action)) {
            } else if (BleService.ACTION_DATA_CHANGED.equals(action)) {
                int status = intent.getIntExtra("value", 0);
                byte[] data = intent.getByteArrayExtra("data");
                if(data.length !=0) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss:SSS");// HH:mm:ss
                    //获取当前时间
                    Date date = new Date(System.currentTimeMillis());
                    append_log(simpleDateFormat.format(date) +" \"(0x) "+ byte2hexstr(data,"-") +"\"");
                }
                Log.i("SYD_OTA","received"+data);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.GATT_CONNECTED);
        intentFilter.addAction(BleService.GATT_DISCONNECTED);
        intentFilter.addAction(BleService.GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_CHANGE);
        intentFilter.addAction(BleService.ACTION_DATA_CHANGED);
        intentFilter.addAction(BleService.ACTION_DATA_READ);
        intentFilter.addAction(BleService.ACTION_DATA_WRITE);
        intentFilter.addAction(BleService.ACTION_RSSI_READ);
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

        /**
         * 屏幕常亮需要 申请屏幕 WAKE_LOCK 唤醒锁 权限
         *  <uses-permission android:name="android.permission.WAKE_LOCK" />
         * **/
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_test);

        Intent intent = getIntent();
        DeviceMac = intent.getStringExtra("DEVICE_MAC");
        DeviceName = intent.getStringExtra("DEVICE_NAME");

        Status_textView = (TextView) findViewById(R.id.Status_textView);
        editText_data=(EditText) findViewById(R.id.editText_data);

        intent = new Intent(this, BleService.class);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        boolean a = bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        Log.i("SYD_Test", "onCreate bindService end:" + a);

        Send_button = (Button) findViewById(R.id.Send_button);
        Send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        Send_Ac_commander();
                    }
                }).start();
            }
        });

        String[] ctype = new String[]{"Select special data","点灯:FC010303", "关灯:FC010404","Log:A500A5A5","Version:FB010101","Reset:FF010202","查找:F3010101"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ctype);  //创建一个数组适配器
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式

        spinner_datatype = (Spinner) findViewById(R.id.spinner_datatype);
        spinner_datatype.setAdapter(adapter);
        spinner_datatype.setSelection(spinner_datatype_sel);
        spinner_datatype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spinner_datatype_isInitial) {
                    spinner_datatype_isInitial = false;
                    return;
                }
                spinner_datatype_sel = i;
                if(spinner_datatype_sel==1)
                {
                    editText_data.setText("FC010303");
                }
                else if(spinner_datatype_sel==2)
                {
                    editText_data.setText("FC010404");
                }
                else if(spinner_datatype_sel==3)
                {
                    editText_data.setText("A500A5A5");
                }
                else if(spinner_datatype_sel==4)
                {
                    editText_data.setText("FB010101");
                }
                else if(spinner_datatype_sel==5)
                {
                    editText_data.setText("FF010202");
                }
                else if(spinner_datatype_sel==6)
                {
                    editText_data.setText("F3010101");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        log_textview= (TextView) findViewById(R.id.log_textview);
        log_textview.setMovementMethod(ScrollingMovementMethod.getInstance());
        log_textview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //获取剪贴板管理器：
                cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                // 创建普通字符型ClipData
                mClipData = ClipData.newPlainText("Label", log_textview.getText());
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
                Toast.makeText(getApplicationContext(),"复制成功",Toast.LENGTH_LONG).show();
                return  true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public static byte[] hexStr2Bytes(String str) {
        if(str == null || str.trim().equals("")) {
            return new byte[0];
        }

        byte[] bytes = new byte[str.length() / 2];
        for(int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }

        return bytes;
    }

    public void Send_Ac_commander()
    {
        byte[] dataPacket = new byte[20];
        String str=editText_data.getText().toString();
        ReadData = hexStr2Bytes(str);
        SendPacketID = 0;
        SendPacketAllNum = ReadData.length/20;
        if (ReadData.length % 20 != 0)
            SendPacketAllNum += 1;
        if(ReadData.length<20) {
            System.arraycopy(ReadData, 0, dataPacket, 0, ReadData.length);
        }
        else {
            System.arraycopy(ReadData, 0, dataPacket, 0, 20);
        }
        SendPacketID += 1;
        actionType=ACTIONTYPE_SendData_Doing;

        bleService.sendUartData(dataPacket);
    }

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
