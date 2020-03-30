package com.cys.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.cys.R;


public class DeviceViewHolder extends RecyclerView.ViewHolder {

    public TextView textViewByName;
    public TextView textViewByMac;

    public DeviceViewHolder(View itemView) {
        super(itemView);
        textViewByName = itemView.findViewById(R.id.id_tv_name);
        textViewByMac = itemView.findViewById(R.id.id_tv_mac);
    }
}
