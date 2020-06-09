package com.example.capstone_car;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EndListAdapter extends RecyclerView.Adapter<EndListAdapter.MyViewHolder> {
    private ArrayList<List> mDataset;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView textView_userId, textView_startPoint, textView_endPoint, textView_endDate, textView_status;

        // ViewHolder
        public MyViewHolder(View view) {
            super(view);
            textView_userId = (TextView) view.findViewById(R.id.textView_userId);
            textView_startPoint = (TextView) view.findViewById(R.id.textView_startPoint);
            textView_endPoint = (TextView) view.findViewById(R.id.textView_endPoint);
            textView_endDate = (TextView) view.findViewById(R.id.textView_endDate);
            textView_status = (TextView) view.findViewById(R.id.textView_status);
        }
    }

    public EndListAdapter(ArrayList<List> myData) {
        this.mDataset = myData;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_end, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EndListAdapter.MyViewHolder holder, int position) {
        holder.textView_userId.setText(mDataset.get(position).getUserName());
        holder.textView_startPoint.setText(mDataset.get(position).getStartPoint());
        holder.textView_endPoint.setText(mDataset.get(position).getEndPoint());
        holder.textView_endDate.setText(mDataset.get(position).getEndDate());
        holder.textView_status.setText(mDataset.get(position).getStatus());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}