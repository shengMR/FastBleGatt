package com.cys.fastblegatt.util;

import android.text.TextUtils;

import java.util.Formatter;

public class PrintHelpper {

    /**
     * 数组转十进制字符串
     *
     * @param array 数组
     * @return  字符串
     */
    public static String bytesToString(byte[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }

        StringBuffer sb = new StringBuffer(array.length * 6);
        sb.append("[");
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(" ");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 数组转十六进制字符串
     *
     * @param array 数组
     * @param separator 分隔符
     * @return 字符串
     */
    public static String bytesToHexString(byte[] array, String separator) {

        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("[");
        Formatter formatter = new Formatter(sb);
        formatter.format("%02X", array[0]);
        for (int i = 1; i < array.length; i++) {
            if (!TextUtils.isEmpty(separator)) {
                sb.append(separator);
            }
            formatter.format("%02X", array[i]);
        }
        sb.append("]");

        formatter.flush();
        formatter.close();

        return sb.toString();
    }

}
