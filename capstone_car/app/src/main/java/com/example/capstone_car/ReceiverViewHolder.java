package com.example.capstone_car;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

class ReceiverViewHolder extends RecyclerView.ViewHolder{
    TextView name;
    TextView phone;

    public ReceiverViewHolder(View itemView){
        super(itemView);
        name = (TextView)itemView.findViewById(R.id.receiver_name);
        phone = (TextView)itemView.findViewById(R.id.receiver_phone);
    }
}