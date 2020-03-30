package com.cys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cys.adapter.DeviceAdapter;
import com.cys.fastblegatt.FastBleManager;
import com.cys.fastblegatt.util.Logger;
import com.cys.fastblescan.FastBleScanner;
import com.cys.fastblescan.bean.ScanDevice;
import com.cys.fastblescan.callback.FastBleScanCallback;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        FastBleScanCallback, DeviceAdapter.OnItemClickListener {

    private RecyclerView idRcvDevice;
    private List<ScanDevice> mDeviceList = new ArrayList<>();
    private DeviceAdapter adapterDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.isDebug = true;

        if (!FastBleScanner.getInstance().isSupportBluetooth()) {
            Toast.makeText(this, "本设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }

        findViewById(R.id.id_bt_start_scan).setOnClickListener(this);
        findViewById(R.id.id_bt_stop_scan).setOnClickListener(this);
        findViewById(R.id.id_bt_disconnect).setOnClickListener(this);

        idRcvDevice = findViewById(R.id.id_rcv_device);
        idRcvDevice.setLayoutManager(new LinearLayoutManager(this));
        idRcvDevice.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterDevice = new DeviceAdapter();
        idRcvDevice.setAdapter(adapterDevice);
        adapterDevice.setOnItemClickListener(this);

        FastBleScanner.getInstance().setScanCallback(this);
        FastBleManager.getInstance().init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!FastBleScanner.getInstance().isBluetoothEnabled()) {
            FastBleScanner.getInstance().enableBluetooth();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.id_bt_start_scan) {
            if (FastBleScanner.getInstance().isBluetoothEnabled()) {
                mDeviceList.clear();
                adapterDevice.replaceData(mDeviceList);
                FastBleScanner.getInstance().setIgnoreSame(true).startScan();
            }
        } else if (v.getId() == R.id.id_bt_stop_scan) {
            if (FastBleScanner.getInstance().isBluetoothEnabled()) {
                FastBleScanner.getInstance().stopScan();
            }
        } else if (v.getId() == R.id.id_bt_disconnect) {

        }
    }

    @Override
    public void onStartScan() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "开始扫描", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLeScan(final ScanDevice scanDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("Sheng", "扫描 -> " + scanDevice.deviceName);
                mDeviceList.add(scanDevice);
                adapterDevice.replaceData(mDeviceList);
            }
        });
    }

    @Override
    public void onStopScan() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "停止扫描", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onScanFailure(int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "扫描出错", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(ScanDevice scanDevice) {
        if (FastBleScanner.getInstance().isBluetoothEnabled()) {
            FastBleScanner.getInstance().stopScan();
        }
        Intent intent = new Intent(this, DiscoverServicesActivity.class);
        intent.putExtra("CurrentDevice", scanDevice);
        startActivity(intent);
    }

    /*
    public void onDeviceReady() {
        Request requestEnableNotify = Request.newWriteRequest(mServiceUuid, mCharactiesticUuid, new byte[]{0x01}, new RequestCallback() {
            @Override
            public void success(BaseFastBleGatt baseFastGatt, Request request, Object data) {
                mLogger.d("开启通知 发送成功");
            }

            @Override
            public void error(BaseFastBleGatt baseFastGatt, Request request, String errorMsg) {
                mLogger.d("开启通知 发送失败");
            }

            @Override
            public boolean timeout(BaseFastBleGatt baseFastGatt, Request request) {
                mLogger.d("开启通知 发送超时");
                return false;
            }
        });

        mFastGatt.sendRequest(requestEnableNotify);

        Request request2EnableNotify = Request.newWriteRequest(mServiceUuid, mCharactiesticUuid, new byte[]{0x01}, new RequestCallback() {
            @Override
            public void success(BaseFastBleGatt baseFastGatt, Request request, Object data) {
                mLogger.d("开启通知 发送成功");
            }

            @Override
            public void error(BaseFastBleGatt baseFastGatt, Request request, String errorMsg) {
                mLogger.d("开启通知 发送失败");
            }

            @Override
            public boolean timeout(BaseFastBleGatt baseFastGatt, Request request) {
                mLogger.d("开启通知 发送超时");
                return false;
            }
        }, 4000);

        mFastGatt.sendRequest(request2EnableNotify);

        for (int i = 513; i < 530; i ++) {
            Request requestMtu = Request.newMtuRequest(
                    UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1910"),
                    UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1911"), i, new RequestCallback() {
                @Override
                public void success(BaseFastBleGatt baseFastGatt, Request request, Object data) {
                    Logger.d("Mtu 发送成功" + (int) request.getTag());
                }

                @Override
                public void error(BaseFastBleGatt baseFastGatt, Request request, String errorMsg) {
                    Logger.d("Mtu 发送失败");
                }

                @Override
                public boolean timeout(BaseFastBleGatt baseFastGatt, Request request) {
                    Logger.d("Mtu 发送超时" + (int) request.getTag());
                    return false;
                }
            });
            requestMtu.setTag(i);
            FastBleGatt.getInstance().sendRequest(requestMtu);
        }

        byte[] data = new byte[]{
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
        };
        List<Request> requests = FastBleGatt.getInstance().calculData(
                UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1910"),
                UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1911"),
                data, new RequestCallback() {
                    @Override
                    public void success(BaseFastBleGatt baseFastGatt, Request request, Object data) {
                        Logger.d("发送成功" + ArraysUtils.bytesToHexString(request.data, ","));
                    }

                    @Override
                    public void error(BaseFastBleGatt baseFastGatt, Request request, String errorMsg) {

                    }

                    @Override
                    public boolean timeout(BaseFastBleGatt baseFastGatt, Request request) {
                        return false;
                    }
                });
        Logger.d("request length = " + requests.size());
        for (int i = 0; i < requests.size(); i++) {
            Logger.d(String.format("request data[%d] = %s", i, ArraysUtils.bytesToHexString(requests.get(i).data, ",")));
        }

        FastBleGatt.getInstance().sendCalcuData(requests);

    }
     */

}
