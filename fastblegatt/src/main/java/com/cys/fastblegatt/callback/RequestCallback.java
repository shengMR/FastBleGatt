package com.cys.fastblegatt.callback;

import com.cys.fastblegatt.FastBleGatt;
import com.cys.fastblegatt.request.Request;

/**
 * 命令回调
 */
public interface RequestCallback {

    void success(FastBleGatt fastBleGatt, Request request, Object data);

    void error(FastBleGatt fastBleGatt, Request request, String errorMsg);

    boolean timeout(FastBleGatt fastBleGatt, Request request);
}
