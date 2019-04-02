package com.example.bluetooth.le;

/**
 * Created by Administrator on 2018/8/7.
 */

public class FileUtils {


    public static String strToBinary(String str) {
        char[] c = str.toCharArray();
        String s = "";
        for (int i = 0; i < c.length; i++) {
            int temp = c[i];
            s += Integer.toBinaryString(temp) + " ";
        }
        return s;

    }
}
