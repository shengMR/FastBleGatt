package com.cys.fastblegatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.cys.fastblegatt.callback.BleCallback;
import com.cys.fastblegatt.callback.BleCallbackAdapterListener;
import com.cys.fastblegatt.callback.RequestCallback;
import com.cys.fastblegatt.request.Request;
import com.cys.fastblegatt.util.Logger;
import com.cys.fastblegatt.util.PrintHelpper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class FastBleGatt {

    public static String TAG = FastBleGatt.class.getSimpleName();

    private Context mContext;
    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;
    private List<BluetoothGattService> mGattServices = new ArrayList<>();
    private int mConnectStatus = BluetoothAdapter.STATE_DISCONNECTED;
    private BluetoothGattCallback mGattCallback;
    private BleCallback mBleCallback;
    private BleCallbackAdapterListener mBleCallbackAdapterListener;
    private int mBleDataSize = 20;
    private long mDefaultTimeoutDelay = 10 * 1000L;
    private boolean mDescriptorNotify = false;

    private final LinkedBlockingDeque<Request> mInBufferedDeque = new LinkedBlockingDeque<Request>();
    private final LinkedBlockingDeque<Request> mOutBufferedDeque = new LinkedBlockingDeque<Request>();
    private AtomicBoolean isPostRequesting = new AtomicBoolean(false);
    private Handler mConnectTimeoutHandler = new Handler(Looper.getMainLooper());
    private Handler mPostTimeoutHandler = new Handler(Looper.getMainLooper());
    private Handler mPostDelayHandler = new Handler(Looper.getMainLooper());
    private final Object mStatusLock = new Object();

    //region Task
    public Runnable mConneciTimeoutTask = new Runnable() {
        @Override
        public void run() {
            if (!isConnected()) {
                onDeviceConnectTimeout(mDevice);
            }
        }
    };

    public Runnable mPostDelayTask = new Runnable() {
        @Override
        public void run() {
            synchronized (mOutBufferedDeque) {
                Request request = mOutBufferedDeque.peek();
                realSendRequest(request);
            }
        }
    };

    public Runnable mPostTimeoutTask = new Runnable() {

        @Override
        public void run() {
            synchronized (mOutBufferedDeque) {

                // 得到队头的对象
                Request request = mOutBufferedDeque.peek();
                if (request != null) {

                    Logger.d("send request timeout");
                    boolean retry = requestTimeout(request);

                    if (retry) {
                        realSendRequest(request);
                    } else {
                        mOutBufferedDeque.poll();
                        requestCompleted();
                    }
                }
            }
        }
    };
    //endregion

    public FastBleGatt(Context context, BluetoothDevice device) {
        this.mContext = context;
        this.mGattCallback = new BleGattCallback();
        this.mDevice = device;
    }

    public FastBleGatt setBleCallback(BleCallback callback) {
        this.mBleCallback = callback;
        return this;
    }

    public FastBleGatt setBleCallbackAdapterListener(BleCallbackAdapterListener listener) {
        this.mBleCallbackAdapterListener = listener;
        return this;
    }

    public FastBleGatt setDescriptorNotify(boolean mSetDescriptorNotify) {
        this.mDescriptorNotify = mSetDescriptorNotify;
        return this;
    }

    public boolean isConnected() {
        return mConnectStatus == BluetoothAdapter.STATE_CONNECTED;
    }

    public List<BluetoothGattService> getGattServices() {
        return mGattServices;
    }

    public int getBleDataSize() {
        return mBleDataSize;
    }

    public boolean connect() {
        return connect(mDefaultTimeoutDelay);
    }

    public boolean connect(long delay) {

        synchronized (mStatusLock) {
            if (mConnectStatus == BluetoothAdapter.STATE_CONNECTED) {
                return true;
            }
            mConnectStatus = BluetoothAdapter.STATE_CONNECTING;
        }

        close(mBluetoothGatt);

        onDeviceConnecting(mDevice);

        // 连接超时
        mConnectTimeoutHandler.removeCallbacks(mConneciTimeoutTask);
        mConnectTimeoutHandler.postDelayed(mConneciTimeoutTask, delay);
        mBluetoothGatt = this.mDevice.connectGatt(this.mContext, false, mGattCallback);
        return mBluetoothGatt != null;
    }

    public void disconnect() {
        boolean connected = isConnected();
        if (connected) {
            synchronized (mStatusLock) {
                mConnectStatus = BluetoothAdapter.STATE_DISCONNECTING;
            }
            onDeviceDisconnecting(mDevice);
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
            }
        } else {
            if (mConnectStatus == BluetoothAdapter.STATE_CONNECTING) {
                close(mBluetoothGatt);
                reset();
            }
        }
    }

    private void close(BluetoothGatt gatt) {
        if (gatt != null) {
            gatt.close();
        }
    }

    private void reset() {
        synchronized (mOutBufferedDeque) {
            mOutBufferedDeque.clear();
        }
        synchronized (mInBufferedDeque) {
            mInBufferedDeque.clear();
        }
        isPostRequesting.set(false);
        mConnectStatus = BluetoothAdapter.STATE_DISCONNECTED;
        mConnectTimeoutHandler.removeCallbacksAndMessages(null);
        mPostTimeoutHandler.removeCallbacksAndMessages(null);
        mPostDelayHandler.removeCallbacksAndMessages(null);
    }

    //region 蓝牙操作
    public void sendRequests(List<Request> requests) {
        for (int i = 0; i < requests.size(); i++) {
            sendRequest(requests.get(i));
        }
    }

    public void sendRequest(Request request) {

        if (request != null) {
            if (!isConnected()) {
                if (request.callback != null) {
                    request.callback.error(this, request, "The device is not connected");
                }
                return;
            }
            mInBufferedDeque.add(request);
            // 上一条还没完成，直接保存在队列中然后直接return
            if (isPostRequesting.get()) {
                return;
            }
            isPostRequesting.set(true);
            this.postRequest();
        }
    }

    private void postRequest() {

        Request request;

        synchronized (mInBufferedDeque) {
            if (mInBufferedDeque.isEmpty()) {
                isPostRequesting.set(false);
                return;
            }
            request = mInBufferedDeque.poll();
        }

        if (request == null) {
            isPostRequesting.set(false);
            return;
        }

        int requestType = request.requestType;
        // 识别只有读写操作才需要添加到输出队列
        if (mDescriptorNotify || requestType != Request.ENABLE_NOTIFY && requestType != Request.DISABLE_NOTIFY) {
            synchronized (mOutBufferedDeque) {
                mOutBufferedDeque.add(request);
            }
        }

        if (request.delay > 0) {
            mPostDelayHandler.postDelayed(mPostDelayTask, request.delay);
            return;
        }

        this.realSendRequest(request);
    }

    synchronized private void realSendRequest(Request request) {

        if (request == null) {
            this.isPostRequesting.set(false);
            return;
        }

        if (!isConnected()) {
            reset();
            return;
        }

        // 根据类型执行蓝牙操作
        int type = request.requestType;
        switch (type) {
            case Request.WRITE:
                setTimeoutFlag();
                writeCharacteristic(request);
                break;
            case Request.READ:
                setTimeoutFlag();
                readCharacteristic(request);
                break;
            case Request.ENABLE_NOTIFY:
                if (mDescriptorNotify) {
                    setTimeoutFlag();
                }
                enableNotification(request);
                break;
            case Request.DISABLE_NOTIFY:
                if (mDescriptorNotify) {
                    setTimeoutFlag();
                }
                disableNotification(request);
                break;
            case Request.MTU:
                setTimeoutFlag();
                mtuChange(request);
                break;
        }
    }

    private void setTimeoutFlag() {
        this.mPostTimeoutHandler.removeCallbacksAndMessages(null);
        this.mPostTimeoutHandler.postDelayed(this.mPostTimeoutTask, 2000);
    }

    private void clearTimeoutFlag() {
        this.mPostTimeoutHandler.removeCallbacksAndMessages(null);
    }

    private void writeCharacteristic(Request request) {

        final BluetoothGatt mGatt = this.mBluetoothGatt;
        boolean success = true;
        String errorMsg = "";

        final Request realRequest = request;

        if (mGatt != null) {

            BluetoothGattService service = mGatt.getService(realRequest.serviceUuid);

            if (service != null) {
                BluetoothGattCharacteristic characteristic = service
                        .getCharacteristic(realRequest.characteristicUuid);

                if (characteristic != null) {

                    characteristic.setValue(request.data);

                    if (!mGatt.writeCharacteristic(characteristic)) {
                        success = false;
                        errorMsg = "write characteristic error";
                    }

                } else {
                    success = false;
                    errorMsg = "device does not have this characteristic UUID";
                }
            } else {
                success = false;
                errorMsg = "device does not have this service UUID";
            }
        } else {
            success = false;
            errorMsg = "gatt does not exist";
        }

        if (!success) {
            this.requestError(errorMsg);
            this.requestCompleted();
        }
    }

    private void readCharacteristic(Request request) {

        final BluetoothGatt gatt = this.mBluetoothGatt;
        boolean success = true;
        String errorMsg = "";

        final Request realRequest = request;

        if (gatt != null) {
            BluetoothGattService service = gatt.getService(realRequest.serviceUuid);
            if (service != null) {

                BluetoothGattCharacteristic characteristic = service
                        .getCharacteristic(realRequest.characteristicUuid);

                if (characteristic != null) {

                    if (!gatt.readCharacteristic(characteristic)) {
                        success = false;
                        errorMsg = "read characteristic error";
                    }

                } else {
                    success = false;
                    errorMsg = "device does not have this characteristic UUID";
                }
            } else {
                success = false;
                errorMsg = "device does not have this service UUID";
            }
        } else {
            success = false;
            errorMsg = "gatt does not exist";
        }

        if (!success) {
            this.requestError(errorMsg);
            this.requestCompleted();
        }
    }

    private void enableNotification(Request request) {

        final BluetoothGatt gatt = this.mBluetoothGatt;
        boolean success = true;
        String errorMsg = "";

        final Request realRequest = request;

        if (gatt != null) {

            BluetoothGattService service = gatt.getService(realRequest.serviceUuid);

            if (service != null) {

                BluetoothGattCharacteristic characteristic = service
                        .getCharacteristic(realRequest.characteristicUuid);

                if (characteristic != null) {

                    if (!gatt.setCharacteristicNotification(characteristic,
                            true)) {
                        success = false;
                        errorMsg = "open notification error";
                    } else {
                        if (mDescriptorNotify) {
                            if (characteristic.getDescriptors().size() > 0) {
                                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                                for (int i = 0; i < descriptors.size(); i++) {
                                    BluetoothGattDescriptor descriptor = descriptors.get(i);
                                    if (descriptor != null) {
                                        //Write the description value
                                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        }
                                        gatt.writeDescriptor(descriptor);
                                    }
                                }
                            }
                        }
                    }


                } else {
                    success = false;
                    errorMsg = "device does not have this characteristic UUID";
                }

            } else {
                success = false;
                errorMsg = "device does not have this service UUID";
            }
        } else {
            success = false;
            errorMsg = "gatt does not exist";
        }

        if (!success) {
            if (mDescriptorNotify) {
                this.requestError(errorMsg);
            } else {
                this.requestError(request, errorMsg);
            }
            this.requestCompleted();
        }

        if (!mDescriptorNotify) {
            this.requestSuccess(request, "");
            this.requestCompleted();
        }
    }

    private void disableNotification(Request request) {
        final BluetoothGatt gatt = this.mBluetoothGatt;
        boolean success = true;
        String errorMsg = "";

        final Request realRequest = request;

        if (gatt != null) {

            BluetoothGattService service = gatt.getService(realRequest.serviceUuid);

            if (service != null) {

                BluetoothGattCharacteristic characteristic = service
                        .getCharacteristic(realRequest.characteristicUuid);

                if (characteristic != null) {
                    if (!gatt.setCharacteristicNotification(characteristic,
                            false)) {
                        success = false;
                        errorMsg = "close notification error";
                    } else {
                        if (mDescriptorNotify) {
                            if (characteristic.getDescriptors().size() > 0) {
                                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                                for (int i = 0; i < descriptors.size(); i++) {
                                    BluetoothGattDescriptor descriptor = descriptors.get(i);
                                    if (descriptor != null) {
                                        //Write the description value
                                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                                        }
                                        gatt.writeDescriptor(descriptor);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    success = false;
                    errorMsg = "device does not have this characteristic UUID";
                }
            } else {
                success = false;
                errorMsg = "device does not have this service UUID";
            }
        } else {
            success = false;
            errorMsg = "gatt does not exist";
        }

        if (!success) {
            if (mDescriptorNotify) {
                this.requestError(errorMsg);
            } else {
                this.requestError(request, errorMsg);
            }
            this.requestCompleted();
        }

        if (!mDescriptorNotify) {
            this.requestSuccess(request, "");
            this.requestCompleted();
        }

    }

    private void mtuChange(Request request) {

        final BluetoothGatt gatt = this.mBluetoothGatt;
        boolean success = true;
        String errorMsg = "";

        final Request realRequest = request;

        if (gatt != null) {

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                if (!gatt.requestMtu(realRequest.mtu)) {
                    success = false;
                    errorMsg = "request Mtu error";
                }
            }
        } else {
            success = false;
            errorMsg = "gatt does not exist";
        }

        if (!success) {
            this.requestError(errorMsg);
            this.requestCompleted();
        }

    }

    private void requestSuccess(Object data) {
        Request request = this.mOutBufferedDeque.poll();
        this.requestSuccess(request, data);
    }

    private void requestSuccess(Request request, Object data) {

        if (request != null) {
            RequestCallback callback = request.callback;
            if (callback != null) {
                callback.success(this, request,
                        data);
            }
        }
    }

    private void requestError(String errorMsg) {
        Request request = this.mOutBufferedDeque.poll();
        this.requestError(request, errorMsg);
    }

    private void requestError(Request request, String errorMsg) {
        if (request != null) {
            RequestCallback callback = request.callback;
            if (callback != null) {
                callback.error(this, request,
                        errorMsg);
            }
        }
    }

    private void requestCompleted() {

        synchronized (mInBufferedDeque) {
            if (this.mInBufferedDeque.isEmpty()) {
                this.isPostRequesting.set(false);
            } else {
                this.postRequest();
            }
        }
    }

    private boolean requestTimeout(Request request) {

        if (request != null) {
            RequestCallback callback = request.callback;
            if (callback != null) {
                return callback.timeout(this, request);
            }
        }

        return false;
    }
    //endregion

    /**
     * 蓝牙底层回调
     */
    public class BleGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Logger.d("onConnectionStateChange ----> " + " status = " + status + " newState = " + newState);
            // status    newState
            // 0             2                -> 连接成功
            // 0             0                -> 手动断开
            // 133           0                -> 异常断开 : 可能以达到手机连接蓝牙最大数量，再次连接则失败，请使用Gatt.close() ，释放连接
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                synchronized (mStatusLock) {
                    mConnectStatus = BluetoothAdapter.STATE_CONNECTED;
                }
                onDeviceConnected(mDevice);
                Logger.d("discover service...");
                gatt.discoverServices();
            } else {
                boolean disconnecting = mConnectStatus == BluetoothAdapter.STATE_DISCONNECTING;
                synchronized (mStatusLock) {
                    mConnectStatus = BluetoothAdapter.STATE_DISCONNECTED;
                }
                if (disconnecting) {
                    onDeviceDisconnectByUser(mDevice);
                } else {
                    onDeviceDisconnected(mDevice);
                }
                close(gatt);
                reset();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d("discover service success");
                List<BluetoothGattService> services = gatt.getServices();
                mGattServices.clear();
                mGattServices.addAll(services);
                onGattServicesDiscovered(services);
            } else {
                Logger.d("discover service failure");
                if (mBluetoothGatt != null) {
                    synchronized (mStatusLock) {
                        mConnectStatus = BluetoothGatt.STATE_DISCONNECTED;
                    }
                    onDeviceDisconnected(mDevice);
                    close(gatt);
                    reset();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();
            Logger.d("notify change:" + PrintHelpper.bytesToHexString(value, ","));
            onGattCharacteristicChanged(value);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            clearTimeoutFlag();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                Logger.d("read characteristic success:" + PrintHelpper.bytesToHexString(data, ","));
                requestSuccess(data);
                onGattCharacteristicRead(data);
            } else {
                Logger.d("read characteristic failure");
                requestError("read characteristic data error");
            }

            requestCompleted();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            clearTimeoutFlag();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d("write characteristic success");
                requestSuccess(null);
                onGattCharacteristicWrite(characteristic.getValue());
            } else {
                Logger.d("write characteristic failure");
                requestError("write characteristic data error");
            }

            requestCompleted();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

            clearTimeoutFlag();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = descriptor.getValue();
                Logger.d("read descriptor success:" + PrintHelpper.bytesToHexString(data, ","));
                requestSuccess(data);
                onGattDescriptorRead(descriptor.getValue());
            } else {
                Logger.d("read descriptor failure");
                requestError("read descriptor data error");
            }

            requestCompleted();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            clearTimeoutFlag();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d("write descriptor success");
                requestSuccess(null);
                onGattDescriptorWrite(descriptor.getValue());
            } else {
                Logger.d("write descriptor failure");
                requestError("write descriptor data error");
            }

            requestCompleted();
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            clearTimeoutFlag();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d("request mtu success:" + mtu);
                // 需要出去ATT的Opcode一个字节和ATT的handle的两个字节
                mBleDataSize = mtu - 3;
                requestSuccess(null);
                onGattMtuChange(mtu);
            } else {
                requestError("request mtu error");
            }

            requestCompleted();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d("read rssi success:" + rssi);
                onGattRssiChange(rssi);
            } else {
                Logger.d("read rssi failure");
            }
        }
    }

    protected void onGattServicesDiscovered(List<BluetoothGattService> services) {
        if (mBleCallback != null) {
            mBleCallback.onDeviceReady(mDevice);
        }
        if (mBleCallbackAdapterListener != null) {
            mBleCallbackAdapterListener.onDeviceReady(mDevice);
        }
    }

    protected void onGattCharacteristicChanged(byte[] value) {
        if (mBleCallbackAdapterListener != null) {
            mBleCallbackAdapterListener.onNotify(mDevice, value);
        }
    }

    protected void onGattCharacteristicRead(byte[] value) {

    }

    protected void onGattCharacteristicWrite(byte[] value) {

    }

    protected void onGattDescriptorRead(byte[] value) {

    }

    protected void onGattDescriptorWrite(byte[] value) {

    }

    protected void onGattMtuChange(int mtu) {

    }

    protected void onGattRssiChange(int rssi) {
    }

    protected void onDeviceConnecting(BluetoothDevice device) {
        if (mBleCallback != null) {
            mBleCallback.onConnecting(device);
        }
        if (mBleCallbackAdapterListener != null) {
            mBleCallbackAdapterListener.onConnecting(device);
        }
    }

    protected void onDeviceConnected(BluetoothDevice device) {
        if (mBleCallback != null) {
            mBleCallback.onConnected(device);
        }
        if (mBleCallbackAdapterListener != null) {
            mBleCallbackAdapterListener.onConnected(device);
        }
    }

    protected void onDeviceDisconnecting(BluetoothDevice device) {
        if (mBleCallback != null) {
            mBleCallback.onDisconnecting(device);
        }
        if (mBleCallbackAdapterListener != null) {
            mBleCallbackAdapterListener.onDisconnecting(mDevice);
        }
    }

    protected void onDeviceDisconnectByUser(BluetoothDevice device) {
        if (mBleCallback != null) {
            mBleCallback.onDisconnectByUser(device);
        }

        if (mBleCallbackAdapterListener != null) {
            mBleCallbackAdapterListener.onDisconnectByUser(mDevice);
        }
    }

    protected void onDeviceDisconnected(BluetoothDevice device) {
        if (mBleCallback != null) {
            mBleCallback.onDisconnected(device);
        }

        if (mBleCallbackAdapterListener != null) {
            mBleCallbackAdapterListener.onDisconnected(mDevice);
        }
    }

    protected void onDeviceConnectTimeout(BluetoothDevice device) {
        if (mBleCallback != null) {
            mBleCallback.onConnectTimeout(device);
        }

        if (mBleCallbackAdapterListener != null) {
            mBleCallbackAdapterListener.onConnectTimeout(mDevice);
        }
    }

}
