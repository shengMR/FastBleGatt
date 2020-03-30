package com.cys.fastblegatt.callback;

import android.bluetooth.BluetoothDevice;

public interface BleCallback {

    void onConnecting(BluetoothDevice device);

    void onConnected(BluetoothDevice device);

    void onDeviceReady(BluetoothDevice device);

    void onDisconnecting(BluetoothDevice device);

    void onDisconnectByUser(BluetoothDevice device);

    void onDisconnected(BluetoothDevice device);

    void onConnectTimeout(BluetoothDevice device);

    interface NotifyCallback {
        void onNotify(BluetoothDevice device, byte[] data);
    }
}
