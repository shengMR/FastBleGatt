package com.cys.adapter;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cys.R;
import com.cys.fastblescan.bean.ScanDevice;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

    public List<ScanDevice> mDatas = new ArrayList<>();

    public interface OnItemClickListener{
        void onItemClick(ScanDevice scanDevice);
    }

    public OnItemClickListener mItemClickListener;

    public void setOnItemClickListener(OnItemClickListener clickListener){
        this.mItemClickListener = clickListener;
    }

    public void replaceData(List<ScanDevice> datas) {
        this.mDatas.clear();
        this.mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_item_device, parent, false);
        return new DeviceViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        final ScanDevice scanDevice = mDatas.get(position);
        BluetoothDevice device = scanDevice.device;
        holder.textViewByName.setText((TextUtils.isEmpty(scanDevice.deviceName) ? device.getAddress() : scanDevice.deviceName)
                + "( rssi " + scanDevice.rssi + " ) ");
        holder.textViewByMac.setText("Mac : "  + device.getAddress());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mItemClickListener != null){
                    mItemClickListener.onItemClick(scanDevice);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }
}
