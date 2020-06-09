package com.example.capstone_car;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<ReceiverViewHolder>{
    private Activity activity;
    private List<Receiver> receiver;
    private ReceiverClickListener listener;

    public RecyclerViewAdapter(Activity activity, List<Receiver> receiver, ReceiverClickListener listener){
        this.activity = activity;
        this.receiver = receiver;
        this.listener = listener;
    }

    public int getItemCount(){
        if(receiver == null){
            return 0;
        }else{
            return receiver.size();
        }
    }



    public ReceiverViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.receiver_list, parent, false);
        return new ReceiverViewHolder(view);
    }

    public void onBindViewHolder(final ReceiverViewHolder holder, final int position){
        Receiver rec = receiver.get(position);

        StringBuilder builder = new StringBuilder(rec.getNumber());
        builder.setCharAt(11, 'X');
        builder.setCharAt(12, 'X');

        holder.name.setText(rec.getName());
        holder.phone.setText(builder);

        final int index = position;
        final Receiver data = rec;
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onReceiverClick(v, data);
            }
        });
    }
}
