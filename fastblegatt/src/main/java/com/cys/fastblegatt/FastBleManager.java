package com.cys.fastblegatt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.cys.fastblegatt.callback.RequestCallback;
import com.cys.fastblegatt.request.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FastBleManager {

    public static String TAG = FastBleManager.class.getSimpleName();

    private static FastBleManager mThis;
    private Map<String, FastBleGatt> mFastBleGattMap = new HashMap<>();
    private Context mContext;

    public static FastBleManager getInstance() {
        if (mThis == null) {
            synchronized (FastBleManager.class) {
                if (mThis == null) {
                    mThis = new FastBleManager();
                }
            }
        }
        return mThis;
    }

    public void init(Context context) {
        this.mContext = context;
    }

    /**
     * 获取操作中的蓝牙设备集合（包括之前连接过断开的）
     *
     * @return
     */
    public Map<String, FastBleGatt> getFastBleGattMap() {
        return Collections.unmodifiableMap(mFastBleGattMap);
    }

    /**
     * 关闭所有蓝牙连接
     */
    public void closeAllConnects() {
        for (String s : mFastBleGattMap.keySet()) {
            mFastBleGattMap.get(s).disconnect();
        }
        mFastBleGattMap.clear();
    }

    /**
     * 关闭指定的蓝牙连接
     *
     * @param device 指定的蓝牙设备
     */
    public void closeConnect(BluetoothDevice device) {
        if (mFastBleGattMap.containsKey(device.getAddress())) {
            mFastBleGattMap.get(device.getAddress()).disconnect();
            mFastBleGattMap.remove(device.getAddress());
        }
    }

    /**
     * 操作一个蓝牙设备
     *
     * @param device
     * @return
     */
    public FastBleGatt with(BluetoothDevice device) {
        if (mFastBleGattMap.containsKey(device.getAddress())) {
            return mFastBleGattMap.get(device.getAddress());
        }
        FastBleGatt gatt = new FastBleGatt(this.mContext, device);
        mFastBleGattMap.put(device.getAddress(), gatt);
        return gatt;
    }


    /**
     * 分包请求消息（不足一个包消息，补0x00在数据后面）
     *
     * @param serviceUuid        服务UUID
     * @param characteristicUuid 特征UUID
     * @param data               数据
     * @param lineSize           每个消息的包大小
     * @param callback           消息回调
     * @return 消息列表
     */
    public List<Request> calculRequest(UUID serviceUuid, UUID characteristicUuid, byte[] data, int lineSize, RequestCallback callback) {

        final byte[] sendData = data;
        int mtuSize = lineSize;
        if (mtuSize < Request.MTU_MIN - 3) {
            mtuSize = Request.MTU_MIN - 3;
        }
        int packLength = 0;
        if (sendData.length % mtuSize == 0) {
            packLength = sendData.length / mtuSize;
        } else {
            packLength = sendData.length / mtuSize + 1;
        }
        List<Request> requests = new ArrayList<>();
        int dataPosition = 0;
        int dataTag = 0;
        while (packLength > 1) {
            byte[] newData = new byte[mtuSize];
            System.arraycopy(sendData, dataPosition, newData, 0, mtuSize);
            Request request = Request.newWriteRequest(serviceUuid, characteristicUuid, newData, callback);
            request.tag = "Multipack[" + dataTag++ + "]";
            requests.add(request);
            packLength--;
            dataPosition += mtuSize;
        }
        int offsetSize = sendData.length % mtuSize;
        if (offsetSize != 0) {
            byte[] newData = new byte[mtuSize];
            System.arraycopy(sendData, dataPosition, newData, 0, offsetSize);
            Request request = Request.newWriteRequest(serviceUuid, characteristicUuid, newData, callback);
            request.tag = "Multipack[" + dataTag + "]";
            requests.add(request);
        } else {
            byte[] newData = new byte[mtuSize];
            System.arraycopy(sendData, dataPosition, newData, 0, mtuSize);
            Request request = Request.newWriteRequest(serviceUuid, characteristicUuid, newData, callback);
            request.tag = "Multipack[" + dataTag + "]";
            requests.add(request);
        }

        return requests;
    }

}
