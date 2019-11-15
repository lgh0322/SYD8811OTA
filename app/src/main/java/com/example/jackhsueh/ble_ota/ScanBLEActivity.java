package com.example.jackhsueh.ble_ota;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanBLEActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;

    private boolean mScanning;

    private ArrayList<String> deviceMac_list;
    private ArrayList<String> deviceName_list;

    List<Map<String, String>> DevicelistData = new ArrayList<Map<String, String>>();

    // 控件声明
    ListView blelistView = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private SimpleAdapter myBleDeviceAdapter = null;

    private Spinner spinner_mode = null;

    private int spinner_mode_sel = 0;

    private Button About_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_ble);
        blelistView = (ListView)findViewById(R.id.device_list);

        deviceMac_list = new ArrayList<>();
        deviceName_list = new ArrayList<>();

        myBleDeviceAdapter = new SimpleAdapter(ScanBLEActivity.this,
                DevicelistData,
                android.R.layout.simple_list_item_2,
                new String[]{"name", "mac"},
                new int[]{android.R.id.text1, android.R.id.text2});
        blelistView.setAdapter(myBleDeviceAdapter);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "手机不支持蓝牙BLE，请更换手机！", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String[] ctype = new String[]{"OTA Mode", "Test Mode", "BLE Log Mode"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ctype);  //创建一个数组适配器
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式

        SharedPreferences sPreferences=getSharedPreferences("svae_config", MODE_PRIVATE);
        spinner_mode_sel =sPreferences.getInt("spinner_mode_sel", 0);

        spinner_mode = (Spinner) findViewById(R.id.spinner_mode);
        spinner_mode.setAdapter(adapter);
        spinner_mode.setSelection(spinner_mode_sel);
        spinner_mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spinner_mode_sel = i;
                saveUserInfo(getApplicationContext(),spinner_mode_sel,"spinner_mode_sel");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        About_button = (Button) findViewById(R.id.about_button);

        About_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),ABOUT_Active.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                deviceMac_list.clear();
                deviceName_list.clear();
                DevicelistData.clear();
                myBleDeviceAdapter.notifyDataSetChanged();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("123", "MainActivity onStart");
        // 请求打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        blelistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                scanLeDevice(false);

                if(spinner_mode_sel==0) {
                    Intent intent = new Intent(getApplicationContext(), OTA_Active.class);
                    intent.putExtra("DEVICE_NAME",deviceName_list.get(position));
                    intent.putExtra("DEVICE_MAC", deviceMac_list.get(position));
                    startActivity(intent);
                }
                else if(spinner_mode_sel==1) {
                    Intent intent = new Intent(getApplicationContext(), Test_Active.class);
                    intent.putExtra("DEVICE_NAME",deviceName_list.get(position));
                    intent.putExtra("DEVICE_MAC", deviceMac_list.get(position));
                    startActivity(intent);
                }
                else if(spinner_mode_sel==2) {
                    Intent intent = new Intent(getApplicationContext(), Blelog_Active.class);
                    intent.putExtra("DEVICE_NAME",deviceName_list.get(position));
                    intent.putExtra("DEVICE_MAC", deviceMac_list.get(position));
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("123", "MainActivity onResume");
        if(mBluetoothAdapter.isEnabled()) {
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("123", "MainActivity onPause");
        scanLeDevice(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("123", "MainActivity onStop");
        scanLeDevice(false);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("123", "MainActivity onRestart");
    }

    @Override
    protected void onDestroy() {
        Log.i("123", "MainActivity onDestroy");
        super.onDestroy();
        scanLeDevice(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        // 未打开蓝牙
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "请先打开蓝牙!", Toast.LENGTH_SHORT).show();
            Log.i("123", "未打开蓝牙");
        } else {
            Log.i("123", "成功打开蓝牙");
            scanLeDevice(true);
        }
    }

    // 扫描函数
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // 扫描回调
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            //if(rssi > -70) {
            final String device_mac = device.getAddress();//.replace(":", "");
            final String device_name = device.getName();
            for (int i = 0; i < deviceMac_list.size(); i++) {
                if (0 == device_mac.compareTo(deviceMac_list.get(i))) {
                    return;
                }
            }
            if(device_name !=null) {
                deviceMac_list.add(device_mac);
                deviceName_list.add(device_name);
                Map<String, String> listem = new HashMap<String, String>();
                listem.put("name", device_name);
                listem.put("mac", device_mac);
                DevicelistData.add(listem);


            runOnUiThread(new Runnable() {
                @Override
                public void run(){
                    myBleDeviceAdapter.notifyDataSetChanged();
                }
            });
            }
        }
    };

    public void updateDialog(String string) {
        Dialog alertDialog = new AlertDialog.Builder(ScanBLEActivity.this)
                // 设置对话框图标
                // 设置标题
                .setMessage(string)
                .setPositiveButton("确认",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .create();
        alertDialog.show();
    }
    public  void saveUserInfo(Context context,int otaversions,String key){
        /**
         * SharedPreferences将用户的数据存储到该包下的shared_prefs/config.xml文件中，
         * 并且设置该文件的读取方式为私有，即只有该软件自身可以访问该文件
         */
        SharedPreferences sPreferences=context.getSharedPreferences("svae_config", context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sPreferences.edit();
        //当然sharepreference会对一些特殊的字符进行转义，使得读取的时候更加准确
        editor.putInt(key, otaversions);
        //这里我们输入一些特殊的字符来实验效果
//          editor.putString("specialtext", "hajsdh><?//");
//          editor.putBoolean("or", true);
//          editor.putInt("int", 47);
        //切记最后要使用commit方法将数据写入文件
        editor.commit();
    }

}
