package com.cys.fastblegatt.callback;

import android.bluetooth.BluetoothDevice;

public abstract class BleCallbackAdapterListener implements BleCallback, BleCallback.NotifyCallback {

    @Override
    public void onConnecting(BluetoothDevice device) {

    }

    @Override
    public void onConnected(BluetoothDevice device) {

    }

    @Override
    public void onDeviceReady(BluetoothDevice device) {

    }

    @Override
    public void onDisconnecting(BluetoothDevice device) {

    }

    @Override
    public void onDisconnectByUser(BluetoothDevice device) {

    }

    @Override
    public void onDisconnected(BluetoothDevice device) {

    }

    @Override
    public void onConnectTimeout(BluetoothDevice device) {

    }

    @Override
    public void onNotify(BluetoothDevice device, byte[] data) {

    }
}

