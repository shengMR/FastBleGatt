package com.cys.fastblegatt.request;

import androidx.annotation.IntDef;


import com.cys.fastblegatt.callback.RequestCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

public class Request {

    @IntDef({WRITE, READ, MTU, ENABLE_NOTIFY, DISABLE_NOTIFY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestType {
    }

    public static final int WRITE = 0x01;
    public static final int READ = 0x02;
    public static final int MTU = 0x04;
    public static final int ENABLE_NOTIFY = 0x08;
    public static final int DISABLE_NOTIFY = 0x10;

    public static final int MTU_MAX = 517;
    public static final int MTU_MIN = 23;

    public int requestType; // 对蓝牙进行操作的类型
    public UUID serviceUuid; // 操作的服务UUID
    public UUID characteristicUuid; // 操作的特征UUID
    public byte[] data;
    public RequestCallback callback;
    public int mtu;
    public int delay;
    public Object tag;

    public Request(UUID serviceUuid, UUID characteristicUuid, @RequestType int type, RequestCallback callback) {
        this(serviceUuid, characteristicUuid, type, null, 0, callback, 0);
    }

    public Request(UUID serviceUuid, UUID characteristicUuid, @RequestType int type, RequestCallback callback, int delay) {
        this(serviceUuid, characteristicUuid, type, null, 0, callback, delay);
    }

    public Request(UUID serviceUuid, UUID characteristicUuid, @RequestType int type, byte[] data, RequestCallback callback) {
        this(serviceUuid, characteristicUuid, type, data, 0, callback, 0);
    }

    public Request(UUID serviceUuid, UUID characteristicUuid, @RequestType int type, byte[] data, RequestCallback callback, int delay) {
        this(serviceUuid, characteristicUuid, type, data, 0, callback, delay);
    }

    public Request(UUID serviceUuid, UUID characteristicUuid, @RequestType int type, int mtu, RequestCallback callback) {
        this(serviceUuid, characteristicUuid, type, null, mtu, callback, 0);
    }

    public Request(UUID serviceUuid, UUID characteristicUuid, @RequestType int type, int mtu, RequestCallback callback, int delay) {
        this(serviceUuid, characteristicUuid, type, null, mtu, callback, delay);
    }

    public Request(UUID serviceUuid, UUID characteristicUuid, @RequestType int type, byte[] data, int mtu, RequestCallback callback, int delay) {
        this.requestType = type;
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.data = data;
        this.callback = callback;
        this.mtu = mtu;
        // 最小23个字节
        // 最大517个字节
        if (this.mtu < MTU_MIN) {
            this.mtu = MTU_MIN;
        } else if (this.mtu > MTU_MAX) {
            this.mtu = MTU_MAX;
        }
        this.delay = delay;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return this.tag;
    }

    public static Request newWriteRequest(UUID serviceUuid, UUID characteristicUuid, byte[] data, RequestCallback callback) {
        return new Request(serviceUuid, characteristicUuid, WRITE, data, callback);
    }

    public static Request newWriteRequest(UUID serviceUuid, UUID characteristicUuid, byte[] data, RequestCallback callback, int delay) {
        return new Request(serviceUuid, characteristicUuid, WRITE, data, callback, delay);
    }

    public static Request newReadRequest(UUID serviceUuid, UUID characteristicUuid, RequestCallback callback) {
        return new Request(serviceUuid, characteristicUuid, READ, callback);
    }

    public static Request newReadRequest(UUID serviceUuid, UUID characteristicUuid, RequestCallback callback, int delay) {
        return new Request(serviceUuid, characteristicUuid, READ, callback, delay);
    }

    public static Request newMtuRequest(UUID serviceUuid, UUID characteristicUuid, int mtu, RequestCallback callback) {
        return new Request(serviceUuid, characteristicUuid, MTU, mtu, callback);
    }

    public static Request newMtuRequest(UUID serviceUuid, UUID characteristicUuid, int mtu, RequestCallback callback, int delay) {
        return new Request(serviceUuid, characteristicUuid, MTU, mtu, callback, delay);
    }

    public static Request newEnableNotifyRequest(UUID serviceUuid, UUID characteristicUuid, RequestCallback callback) {
        return new Request(serviceUuid, characteristicUuid, ENABLE_NOTIFY, callback);
    }

    public static Request newEnableNotifyRequest(UUID serviceUuid, UUID characteristicUuid, RequestCallback callback, int delay) {
        return new Request(serviceUuid, characteristicUuid, ENABLE_NOTIFY, callback, delay);
    }

    public static Request newDisableNotifyRequest(UUID serviceUuid, UUID characteristicUuid, RequestCallback callback) {
        return new Request(serviceUuid, characteristicUuid, DISABLE_NOTIFY, callback);
    }

    public static Request newDisableNotifyRequest(UUID serviceUuid, UUID characteristicUuid, RequestCallback callback, int delay) {
        return new Request(serviceUuid, characteristicUuid, DISABLE_NOTIFY, callback, delay);
    }

}
