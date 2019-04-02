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

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    //这里赋了一个常量值
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static String YJ_BLE_Service = "49535343-FE7D-4AE5-8FA9-9FAFD205E455";
    public static String YJ_BLE_READ_WRITE = "49535343-8841-43F4-A8D4-ECBE34729BB3";
    public static String YJ_BLE_NOTIFY = "49535343-1E4D-4BD9-BA61-23C647249616";

/*
    public static String YJ_BLE_Service = "18F0";
    public static String YJ_BLE_READ_WRITE = "2AF1";
    public static String YJ_BLE_NOTIFY = "2AF0";
*/





    /*
       飞易通蓝牙模块的
      SERVICE =  49535343-FE7D-4AE5-8FA9-9FAFD205E455
       WRITE  =  49535343-8841-43F4-A8D4-ECBE34729BB3
       NOTIFY =  49535343-1E4D-4BD9-BA61-23C647249616
  */
    static {
        // Sample Services.给自己用到的服务命名
        attributes.put("49535343-FE7D-4AE5-8FA9-9FAFD205E455", "佳博蓝牙测试");
        //Sample Characteristics.给自己用到的特征值命名
        attributes.put(YJ_BLE_READ_WRITE, "READ_WRITE");
        attributes.put("00002a37-0000-1000-8000-00805f9b34fb", "YJ Name");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
