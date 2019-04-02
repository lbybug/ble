package com.example.bluetooth.le;

/**
 * Created by Administrator on 2018/04/21.
 */

public class Utils {
   //将数据进行分包发送
    public static String stringSpilt(String str, int m) {

        for (int i = 0; i <= str.length() * 3; i++) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String str4 = str.substring(0, m);

            String str5 = str.substring(m, str.length());

            str = str5;

            System.out.println(str4);



        }

        if (str.length() >= m) {

            String str11 = str.substring(0, m);

            String str10 = str.substring(0, str11.length());

            System.out.println(str10);


        } else {
            System.out.println(str);
        }
        return str;


    }
  //字符数组转换为字符串
    public static String StringCharChangeString(String[] str) {
        //String[] str = {"abc", "bcd", "def"};
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length; i++) {
            sb.append(str[i]);
        }
        String s = sb.toString();

        return s;
    }
    //计算包含中文的字节的长度
    public static int length(String value) {
        int valueLength = 0;
        String chinese = "[\u0391-\uFFE5]";
        /* 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1 */
        for (int i = 0; i < value.length(); i++) {
            /* 获取一个字符 */
            String temp = value.substring(i, i + 1);
            /* 判断是否为中文字符 */
            if (temp.matches(chinese)) {
                /* 中文字符长度为2 */
                valueLength += 2;
            } else {
                /* 其他字符长度为1 */
                valueLength += 1;
            }
        }
        return valueLength;
    }
}
