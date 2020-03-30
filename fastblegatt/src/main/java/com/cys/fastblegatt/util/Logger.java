package com.cys.fastblegatt.util;

import android.util.Log;

public class Logger {

    public static boolean isDebug = false;
    public static String MAIN_TAG = "FastBleGatt-Log";

    public static void d(String message){
        if (isDebug){
            Log.d(MAIN_TAG, message);
        }
    }


}
