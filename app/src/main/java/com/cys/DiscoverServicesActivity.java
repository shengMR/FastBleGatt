package com.cys;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cys.basis.view.helper.adapter.BaseViewHolder;
import com.cys.fastblegatt.FastBleGatt;
import com.cys.fastblegatt.FastBleManager;
import com.cys.fastblegatt.callback.BleCallback;
import com.cys.fastblegatt.callback.RequestCallback;
import com.cys.fastblegatt.request.Request;
import com.cys.fastblegatt.util.Logger;
import com.cys.fastblegatt.util.PrintHelpper;
import com.cys.fastblescan.bean.ScanDevice;

import java.util.ArrayList;
import java.util.List;

public class DiscoverServicesActivity extends AppCompatActivity {

    private ExpandableListView idElvService;
    private ElvAdapter adapterService;
    private FastBleGatt currentGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_services);


        if (getIntent().hasExtra("CurrentDevice")) {
            final ScanDevice scanDevice = getIntent().getParcelableExtra("CurrentDevice");
            currentGatt = FastBleManager.getInstance().with(scanDevice.device);
            currentGatt
                    .setBleCallback(new BleCallback() {
                        @Override
                        public void onConnecting(BluetoothDevice device) {
                            Logger.d("连接中 = " + device.getAddress());
                        }

                        @Override
                        public void onConnected(BluetoothDevice device) {
                            Logger.d("连接成功 = " + device.getAddress());
                        }

                        @Override
                        public void onDeviceReady(BluetoothDevice device) {
                            Logger.d("设备准备完毕 = " + device.getAddress());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (currentGatt == null) {
                                        return;
                                    }
                                    if (adapterService != null) {
                                        adapterService.replaceData(currentGatt.getGattServices());
                                    }
                                }
                            });
                        }

                        @Override
                        public void onDisconnecting(BluetoothDevice device) {
                            Logger.d("设备断开中 = " + device.getAddress());
                        }

                        @Override
                        public void onDisconnectByUser(BluetoothDevice device) {
                            Logger.d("设备主动断开 = " + device.getAddress());
                        }

                        @Override
                        public void onDisconnected(BluetoothDevice device) {
                            Logger.d("设备被动断开 = " + device.getAddress());
                            finish();
                        }

                        @Override
                        public void onConnectTimeout(BluetoothDevice device) {
                            finish();
                        }
                    })
                    .connect();
        }

        idElvService = findViewById(R.id.id_elv);
        idElvService.setGroupIndicator(null);
        adapterService = new ElvAdapter(this);
        idElvService.setAdapter(adapterService);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentGatt != null) {
            currentGatt.disconnect();
        }
    }

    class ElvAdapter extends BaseExpandableListAdapter {

        public Context mContext;
        public LayoutInflater mInflater;
        public List<BluetoothGattService> mService = new ArrayList<>();

        public void replaceData(List<BluetoothGattService> services) {
            if (mService != services) {
                mService.clear();
                mService.addAll(services);
            }
            notifyDataSetChanged();
        }

        public ElvAdapter(Context context) {
            this.mContext = context.getApplicationContext();
            this.mInflater = LayoutInflater.from(this.mContext);
        }

        @Override
        public int getGroupCount() {
            return mService.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mService.get(groupPosition).getCharacteristics().size();
        }

        @Override
        public BluetoothGattService getGroup(int groupPosition) {
            return mService.get(groupPosition);
        }

        @Override
        public BluetoothGattCharacteristic getChild(int groupPosition, int childPosition) {
            return mService.get(groupPosition).getCharacteristics().get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            BaseViewHolder viewHolder = BaseViewHolder.getViewHolder(this.mContext, R.layout.adapter_item_service, groupPosition, convertView, parent);
            viewHolder.setText(R.id.id_tv_service_uuid, "服务 -> " + adapterService.mService.get(groupPosition).getUuid().toString());
            if (isExpanded) {
                viewHolder.setImageResource(R.id.id_iv_arrow, R.drawable.ic_arrow_down);
            } else {
                viewHolder.setImageResource(R.id.id_iv_arrow, R.drawable.ic_arrow_up);
            }
            return viewHolder.getItemView();
        }

        @SuppressLint("SetTextI18n")
        @Override
        public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            final BluetoothGattCharacteristic bean = adapterService.mService.get(groupPosition).getCharacteristics().get(childPosition);
            BaseViewHolder viewHolder = BaseViewHolder.getViewHolder(this.mContext, R.layout.adapter_item_characteristic, childPosition, convertView, parent);
            viewHolder.setText(R.id.id_tv_characteritic_uuid, "特征 -> " + bean.getUuid().toString());
            TextView readTv = viewHolder.getView(R.id.id_tv_read);
            Button readBt = viewHolder.getView(R.id.id_read);
            Button writeBt = viewHolder.getView(R.id.id_write);
            Button writeNRBt = viewHolder.getView(R.id.id_write_no_response);
            Button notifyBt = viewHolder.getView(R.id.id_notify);
            readTv.setVisibility(View.GONE);
            readBt.setVisibility(View.GONE);
            writeBt.setVisibility(View.GONE);
            writeNRBt.setVisibility(View.GONE);
            notifyBt.setVisibility(View.GONE);

            // 读按钮
            readBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Request request = Request.newReadRequest(adapterService.mService.get(groupPosition).getUuid(), bean.getUuid(), new RequestCallback() {
                        @Override
                        public void success(FastBleGatt baseFastGatt, Request request, Object data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DiscoverServicesActivity.this, "读取成功", Toast.LENGTH_SHORT).show();
                                    adapterService.notifyDataSetChanged();
                                }
                            });
                        }

                        @Override
                        public void error(FastBleGatt baseFastGatt, Request request, String errorMsg) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DiscoverServicesActivity.this, "读取失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public boolean timeout(FastBleGatt baseFastGatt, Request request) {
                            return false;
                        }
                    });
                    currentGatt.sendRequest(request);
                }
            });

            writeBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    byte[] data = new byte[]{
                            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                            0x00/*, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06*/
                    };
                    List<Request> requests = FastBleManager.getInstance().calculRequest(
                            adapterService.mService.get(groupPosition).getUuid(),
                            bean.getUuid(),
                            data,
                            currentGatt.getBleDataSize(),
                            new RequestCallback() {
                                @Override
                                public void success(FastBleGatt baseFastGatt, Request request, Object data) {
                                    Logger.d("发送成功:" + "Tag = " + request.tag + " " + PrintHelpper.bytesToHexString(request.data, ","));
                                }

                                @Override
                                public void error(FastBleGatt baseFastGatt, Request request, String errorMsg) {

                                }

                                @Override
                                public boolean timeout(FastBleGatt baseFastGatt, Request request) {
                                    return false;
                                }
                            });
                    Logger.d("request length = " + requests.size());
                    for (int i = 0; i < requests.size(); i++) {
                        Logger.d(String.format("request data[%d] = %s", i, PrintHelpper.bytesToHexString(requests.get(i).data, ",")));
                    }

                    currentGatt.sendRequests(requests);
                }
            });

            // 通知按钮
            notifyBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Request request = Request.newEnableNotifyRequest(adapterService.mService.get(groupPosition).getUuid(), bean.getUuid(), new RequestCallback() {
                        @Override
                        public void success(FastBleGatt baseFastGatt, Request request, Object data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DiscoverServicesActivity.this, "成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void error(FastBleGatt baseFastGatt, Request request, String errorMsg) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DiscoverServicesActivity.this, "失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public boolean timeout(FastBleGatt baseFastGatt, Request request) {
                            return false;
                        }
                    });
                    currentGatt.sendRequest(request);
                }
            });


            // 解析属性
            int properties = bean.getProperties();
            if ((properties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) == BluetoothGattCharacteristic.PROPERTY_BROADCAST) {

            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ) {
                readBt.setVisibility(View.VISIBLE);
                if (bean.getValue() != null) {
                    readTv.setVisibility(View.VISIBLE);

                    readTv.setText("[" + PrintHelpper.bytesToHexString(bean.getValue(), ",") + "]" + new String(bean.getValue()));
                }
            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
                writeNRBt.setVisibility(View.VISIBLE);
            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE) {
                writeBt.setVisibility(View.VISIBLE);
            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                notifyBt.setVisibility(View.VISIBLE);
            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE) {

            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) {

            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) == BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) {

            }


            return viewHolder.getItemView();
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
