/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 * 按位或，逻辑或 |  ||
 * 按位与，逻辑与 &  &&
 * Integer.parseInt(),转换成基本数据类型int,转换结果不具备属性和方法。
 * Integer.valueOf(),转换成Integer类（对象），具备该类属性和方法。与String.valueOf()的作用结果一致，结果为String对象
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private Button registerButton, timeButton, testButton, fontButton,M320Button;
    private String mDeviceAddress;
    private String str;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;//自定义的一个继承自Service的服务

    private boolean mConnected = false;
    private BluetoothGattCharacteristic mCharacteristic, mCharacteristicNotify;
    private BluetoothGattService mnotyGattService;//三个长得很像，由大到小的对象BluetoothGatt、
    //BluetoothGattService、BluetoothGattCharacteristic、



/*
     飞易通蓝牙模块的
    SERVICE =  49535343-FE7D-4AE5-8FA9-9FAFD205E455
     WRITE  =  49535343-8841-43F4-A8D4-ECBE34729BB3
     NOTIFY =  49535343-1E4D-4BD9-BA61-23C647249616
*/


    //蓝牙模块的服务和特征值
    private static final UUID uuid = UUID
            .fromString(SampleGattAttributes.YJ_BLE_Service);
    private static final UUID UUID_READ_WRITE = UUID
            .fromString(SampleGattAttributes.YJ_BLE_READ_WRITE);
    private static final UUID UUID_NOTIFY = UUID
            .fromString(SampleGattAttributes.YJ_BLE_NOTIFY);

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        //服务连接建立之后的回调函数。
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;

        }
    };

    // Handles various events fired by the Service.    
    // data:This can be a result of read or notification operations.
    // 接受来自设备的数据，可以通过读或通知操作获得。
    // 通过服务控制不同的事件
    // ACTION_GATT_CONNECTED: 连接到GATT服务端
    // ACTION_GATT_DISCONNECTED: 未连接GATT服务端.
    // ACTION_GATT_SERVICES_DISCOVERED: 未发现GATT服务.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //通过intent获得的不同action，来区分广播该由谁接收(只有action一致,才能接收)。
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateConnectionState(R.string.connected);

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;

                updateConnectionState(R.string.disconnected);

                //clearUI();
            }else if (BluetoothLeService.ACTION_GATT_CONNECTING.equals(action)) {


                updateConnectionState(R.string.connecting);

                //clearUI();
                //发现服务后，自动执行回调方法onServicesDiscovered(),发送一个action=ACTION_GATT_SERVICES_DISCOVERED的广播，其他情况同理
            }else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                mnotyGattService = mBluetoothLeService.getSupportedGattServices(uuid);//找特定的某个服务
                mCharacteristic = mnotyGattService.getCharacteristic(UUID_READ_WRITE);//获取可读写的特征值
                mCharacteristicNotify = mnotyGattService.getCharacteristic(UUID_NOTIFY);//获取有通知特性的特征值

            }
            // ACTION_DATA_AVAILABLE: 接受来自设备的数据，可以通过读或通知操作获得。
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                str = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);   //接收广播发送的数据，不管读写，一律接收
                displayData(mCharacteristic.getProperties() + "\n" + mCharacteristic.getPermissions() + "\n" + str);    //必须偶数个16进制字符发送，否则易丢失
            }
        }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        init();
        mConnectionState.setText("连接中");
        new DataThread().start();
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            mConnectionState.setText((String)msg.obj);
        };
    };
    private class DataThread extends Thread {
        @Override
        public void run() {

                if( mConnectionState.getText()=="已连接(微信蓝牙)"){
                    final String data1 = "已连接(微信蓝牙)";
                    mHandler.sendMessage(mHandler.obtainMessage(0, data1));// 只能在主线程中修改ui控件

                }else
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                    }
                if(mConnectionState.getText()=="连接中"){
                    final String data2 = "已断开连接(普通蓝牙)";
                    mHandler.sendMessage(mHandler.obtainMessage(0, data2));// 只能在主线程中修改ui控件
                }


        }
    }

    private void init() {
        // TODO Auto-generated method stub
        registerButton = (Button) findViewById(R.id.register);
        registerButton.setOnClickListener(listener);

        timeButton = (Button) findViewById(R.id.name);
        timeButton.setOnClickListener(listener);


        fontButton = (Button) findViewById(R.id.fontbutton);
        fontButton.setOnClickListener(listener);

        testButton = (Button) findViewById(R.id.testbutton);
        testButton.setOnClickListener(listener);


        M320Button = (Button) findViewById(R.id.M320BUTTON);
        M320Button.setOnClickListener(listener);


        mDataField = (TextView) findViewById(R.id.data_value);
        mDataField.setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);

    }

        private OnClickListener listener = new OnClickListener() {
            @Override

                    public void onClick (View v){
                        // TODO Auto-generated method stub
                        switch (v.getId()) {
                            case R.id.register:
                                try {
                                    write();
                                } catch (Exception E) {
                                    E.printStackTrace();
                                    Toast.makeText(getApplication(), "此蓝牙模块不支持BLE 连接打印", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case R.id.name:
                                try {
                                    write2();
                                } catch (Exception E) {
                                    E.printStackTrace();
                                    Toast.makeText(getApplication(), "此蓝牙模块不支持BLE连接打印", Toast.LENGTH_SHORT).show();
                                }
                                ;

                                break;
                            case R.id.fontbutton:
                                try {
                                write3(Contant.cpclFont);
                                } catch (Exception E) {
                                    E.printStackTrace();
                                    Toast.makeText(getApplication(), "此蓝牙模块不支持BLE连接打印", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case R.id.testbutton:
                                try {
                                write3(Contant.cpclTest);
                                } catch (Exception E) {
                                    E.printStackTrace();
                                    Toast.makeText(getApplication(), "此蓝牙模块不支持BLE连接打印", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case R.id.M320BUTTON:
                                try {

                                    String s = FileUtils.strToBinary("SELFTEST\n");

                                    write3(s);

                                } catch (Exception E) {
                                    E.printStackTrace();

                                }
                                break;
                            default:
                                break;
                        }
                    }
        };

    /*
     * **************************************************************
	 * *****************************写函数*****************************
	 */
    private void write() {
        // TODO Auto-generated method stub
        String adress = "SELFTEST\n";
        byte[] send = adress.getBytes();

        if (mBluetoothLeService.writeCharacteristic(mCharacteristic, send)) {
            //此处执行的写操作，该工程中mCharacteristic可读可写，此处演示用的同一个特征值，由于硬件方面没有对写操作做处理，所以只写进去了一个字节。实际上是写进了2个字节。
            mBluetoothLeService.setCharacteristicNotification(mCharacteristicNotify, true); //项目中用来接收的特征值，具有通知特性，可监听特征值的变化。一有改变，立刻通知。具体到自己项目可不予考虑
            Toast.makeText(DeviceControlActivity.this, "写成功", Toast.LENGTH_SHORT).show();
        } else {


        }
    }


    public void write2() {
        // TODO Auto-generated method stub
        //测试数据
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();//.getExtras()得到intent所附带的额外数据
        String DeviceName = bundle.getString("BlueName");


        String data = "SIZE 76 mm,30 mm\n" +
                "GAP 0.00 mm,0.00 mm\n" +
                "DENSITY 8\n" + "SPEED 3\n" + "CLS\n"
                + "TEXT 50,20,\"TSS24.BF2\",0,2,2,\"使用打标模板扫描此码\"\n" +
                "BARCODE 50,100,\"128\",48,1,0,2,2," + "\"" + DeviceName + "\"" + "\n" +
                "PRINT 1,1\n";


        //  String data =getPrintDataCeshi2;

        Log.e("发送到打印机数据长度:", String.valueOf(Utils.length(data)));

        int tmpLen = Utils.length(data);
        ;
        int start = 0;

        int end = 0;
        while (tmpLen > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] sendData = new byte[20];
            if (tmpLen >= 20) {
                end += 20;
                try {
                    sendData = copyOfRange(data.getBytes("GBK"), start, end);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                start += 20;
                tmpLen -= 20;
            } else {
                end += tmpLen;
                try {
                    sendData = copyOfRange(data.getBytes("GBK"), start, end);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                tmpLen = 0;
            }

            byte[] send = sendData;

            // System.out.printf("有没有数据",send);
            if (mBluetoothLeService.writeCharacteristic(mCharacteristic, send)) {
                //此处执行的写操作，该工程中mCharacteristic可读可写
                mBluetoothLeService.setCharacteristicNotification(mCharacteristicNotify, true); //项目中用来接收的特征值，具有通知特性，可监听特征值的变化。一有改变，立刻通知。具体到自己项目可不予考虑
                Toast.makeText(DeviceControlActivity.this, "写成功", Toast.LENGTH_SHORT).show();

            } else {
                Log.e("错误", "send为空");

            }

        }


    }


    public void write3(String data) {

        int tmpLen = Utils.length(data);
        ;
        int start = 0;

        int end = 0;
        while (tmpLen > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] sendData = new byte[20];
            if (tmpLen >= 20) {
                end += 20;
                try {
                    sendData = copyOfRange(data.getBytes("GBK"), start, end);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                start += 20;
                tmpLen -= 20;
            } else {
                end += tmpLen;
                try {
                    sendData = copyOfRange(data.getBytes("GBK"), start, end);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                tmpLen = 0;
            }

            byte[] send = sendData;

            // System.out.printf("有没有数据",send);
            if (mBluetoothLeService.writeCharacteristic(mCharacteristic, send)) {
                //此处执行的写操作，该工程中mCharacteristic可读可写
                mBluetoothLeService.setCharacteristicNotification(mCharacteristicNotify, true); //项目中用来接收的特征值，具有通知特性，可监听特征值的变化。一有改变，立刻通知。具体到自己项目可不予考虑
                Toast.makeText(DeviceControlActivity.this, "写成功", Toast.LENGTH_SHORT).show();

            } else {
                Log.e("错误", "send为空");

            }

        }

    }


    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0,
                Math.min(original.length - from, newLength));
        return copy;
    }


    /*
	 * *****************************读函数*****************************
	 */
    private void read() {
        // TODO Auto-generated method stub
        mBluetoothLeService.readCharacteristic(mCharacteristic);
        Toast.makeText(DeviceControlActivity.this, "读成功", Toast.LENGTH_SHORT).show();
    }

    /*
	 * *********************十六进制字符串(2位)转换为字节数组******************
	 */
    public synchronized static byte[] hexStringToByte(String hex) {
        hex = hex.toUpperCase();
        int len = hex.length() / 2;
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    /*
     * *********************字符转换为字节********************************
     */
    private synchronized static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Register a BroadcastReceiver to be run in the main activity thread.
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    @Override
    protected void onStop() {
        super.onStop();
        mBluetoothLeService.disconnect();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                mBluetoothLeService.disconnect();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mConnectionState.setText(resourceId);

            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
