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

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@SuppressLint("NewApi")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED           = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING          = "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED        = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE           = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA                      = "com.example.bluetooth.le.EXTRA_DATA";
    public final static String TEST_TIME                       = "com.example.bluetooth.le.TEST_TIME";
	
    //蓝牙的特征值UUID
    private static final UUID UUID_READ_WRITE = UUID
			.fromString(SampleGattAttributes.YJ_BLE_READ_WRITE);
     
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    // 这里有9个要实现的回调方法，看情况要实现那些，用到那些就实现那些
    // 如果只是在某个点接收(有客户端请求)，可以用读;如果要一直接收(无客户端请求)，要用notify

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        //连接或者断开蓝牙，方法一
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.尝试连接GATT服务");
                // Attempts to discover services after successful connection.
                // 函数调用之间存在先后关系。例如首先需要connect上才能discoverServices。    
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            }
            else if (newState == BluetoothProfile.STATE_CONNECTING) {

                intentAction = ACTION_GATT_CONNECTING;
                mConnectionState = STATE_CONNECTING;
                Log.i(TAG, "connecting from GATT server.");
                broadcastUpdate(intentAction);
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
        
        //发现服务的回调，发送广播，方法二
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
       
        //write的回调,发送广播，方法三
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) 
        {
        	Log.e("WRITE", "onCharacteristicWrite() - status: " + status + "  - UUID: " + characteristic.getUuid());            
        	// write回调失败 status=128，    read回调失败status=128.  	status=0,回调成功；=9，数组超长
        	if (status == BluetoothGatt.GATT_SUCCESS) 
            {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.e("WRITE SUCCESS", "回调成功" + status + "  - UUID: " + characteristic.getUuid());
            }else{
            	 Log.e("FAIL", "回调失败 " + status + "  - UUID: " + characteristic.getUuid());
            }
        }
        
        //read回调，方法四
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.e("READ SUCCESS", "onCharacteristicRead() - status: " + status + "  - UUID: " + characteristic.getUuid());
            }
        }

        //notification回调，方法五
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    
    //当一个特定的回调函数被触发，它会调用适当的broadcastUpdate()辅助方法并传递一个action。
    //这个broadcastUpdate方法，实现蓝牙状态（即方法一、二）的广播
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    
    //复写broadcastUpdate方法，来实现蓝牙其他状态的广播（方法三、四、五的回调，蓝牙最重要的三个方法）
    //注意,本节中的数据解析执行按照蓝牙心率测量概要文件规范    
    //Parameters intent ： The Intent to broadcast; 
    //all receivers matching this Intent will receive the broadcast.
    //an intent with a given action.
    private void broadcastUpdate( String action,
                                  BluetoothGattCharacteristic characteristic) {
    	 Intent intent = new Intent(action);   
        	// 配置文件，用十六进制发送和接收数据。    	 
		      byte[] data = characteristic.getValue();
		      StringBuilder stringBuilder = new StringBuilder(data.length);//StringBuilder非线程安全，执行速度最快	      
		      if (data != null && data.length > 0) {	    	  		     	  	            	 	             
    			  for(byte byteChar : data)
    				  stringBuilder.append(String.format("%02X", byteChar));//"%02x "以FF FF形式解析数据(注意有无空格)   			             		  
	              intent.putExtra(EXTRA_DATA,stringBuilder.toString());		            	  
	          }
		      
		   Log.e("广播发送","特征值长度"+characteristic.getValue().length+"  "+stringBuilder.toString());
    	   sendBroadcast(intent); 	  
    }     
    
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        // device.connectGatt连接到GATT Server,并返回一个BluetoothGatt实例.
        // mGattCallback为回调函数BluetoothGattCallback（抽象类）的实例。
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     * 
     * 为应用方便，复写了readCharacteristic()方法
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);        
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     * 
     * 复写setCharacteristicNotification()
     */

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // 设置characteristic的描述值。
        // 所有的服务、特征值、描述值都用UUID来标识，先根据characteristic的UUID找到characteristic，再根据BluetoothGattDescriptor的
        // UUID找到BluetoothGattDescriptor，然后设定其值。
        // 关于descriptor，可以通过getDescriptor()方法的返回值来理解,
        // Returns a descriptor with a given UUID out of the list of descriptors for this characteristic.
    }
    
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic,byte[]test){
    	boolean flag = false;  		
	    characteristic.setValue(test);
	    flag=mBluetoothGatt.writeCharacteristic(characteristic);
	    Log.e(TAG,"数组长度"+test.length+""+flag);
		return flag;    	
   }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public BluetoothGattService getSupportedGattServices(UUID uuid) {
    	BluetoothGattService mBluetoothGattService;
    	if (mBluetoothGatt == null) return null;
    	mBluetoothGattService=mBluetoothGatt.getService(uuid);
        return mBluetoothGattService;
    }  
}

