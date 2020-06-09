package com.example.capstone_car;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.socket.client.IO;
import io.socket.client.Socket;

public class ReceiveListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<List> mDataset;
    private Socket mSocket;

    public ReceiveListAdapter(ArrayList<List> myData) {
        this.mDataset = myData;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (viewType == ReceiveCode.ViewType.WAIT) {    // List when delivery status in on standby
            view = inflater.inflate(R.layout.listview_receive_wait, parent, false);
            return new WaitViewHolder(view);
        } else if (viewType == ReceiveCode.ViewType.DLVYWAIT) {     // List when delivery status in on delivery standby
            view = inflater.inflate(R.layout.listview_receive_dlvywait, parent, false);
            return new DlvyWaitViewHolder(view);
        } else {        // List when delivery status is being delivered
            view = inflater.inflate(R.layout.listview_receive, parent, false);
            return new DlvyViewHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof WaitViewHolder) {
            ((WaitViewHolder)holder).dlvy_num = mDataset.get(position).getDlvyNum();

            ((WaitViewHolder)holder).textView_wait_num.setText("대기 순위 " + (mDataset.get(position).getDlvyWaitNum() + "번째"));

            ((WaitViewHolder)holder).textView_userId.setText(mDataset.get(position).getUserName());
            ((WaitViewHolder)holder).textView_startPoint.setText(mDataset.get(position).getStartPoint());
            ((WaitViewHolder)holder).textView_endPoint.setText(mDataset.get(position).getEndPoint());
            ((WaitViewHolder)holder).textView_status.setText(mDataset.get(position).getStatus());

            ((WaitViewHolder)holder).button_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle( "대기 취소" );
                    builder.setMessage( "정말 대기를 취소하시겠습니까?" );
                    builder.setPositiveButton( "예", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            JSONObject data = new JSONObject();

                            try {
                                data.put("dlvy_num", ((WaitViewHolder)holder).dlvy_num);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            try {
                                mSocket = IO.socket( "https://733fafcacb7f.ngrok.io" );
                            } catch(URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                            mSocket.connect();

                            mSocket.emit("dlvy_wait_cancel", data);
                        }
                    } );
                    builder.setNegativeButton( "아니오", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    } );
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            });

        } else if (holder instanceof DlvyWaitViewHolder) {
            ((DlvyWaitViewHolder)holder).dlvy_num = mDataset.get(position).getDlvyNum();
            ((DlvyWaitViewHolder)holder).car_num = mDataset.get(position).getCarNum();

            ((DlvyWaitViewHolder)holder).textView_time.setText("차가 오기까지 약 " + (mDataset.get(position).getDlvyTime() + "분"));

            ((DlvyWaitViewHolder)holder).textView_userId.setText(mDataset.get(position).getUserName());
            ((DlvyWaitViewHolder)holder).textView_startPoint.setText(mDataset.get(position).getStartPoint());
            ((DlvyWaitViewHolder)holder).textView_endPoint.setText(mDataset.get(position).getEndPoint());
            ((DlvyWaitViewHolder)holder).textView_status.setText(mDataset.get(position).getStatus());

        } else {
            ((DlvyViewHolder)holder).dlvy_num = mDataset.get(position).getDlvyNum();
            ((DlvyViewHolder)holder).car_num = mDataset.get(position).getCarNum();
            ((DlvyViewHolder)holder).textView_userId.setText(mDataset.get(position).getUserName());
            ((DlvyViewHolder)holder).textView_startPoint.setText(mDataset.get(position).getStartPoint());
            ((DlvyViewHolder)holder).textView_endPoint.setText(mDataset.get(position).getEndPoint());
            ((DlvyViewHolder)holder).textView_time.setText("배달 소요 시간 : 약 " + (mDataset.get(position).getDlvyTime() + "분"));
            ((DlvyViewHolder)holder).textView_status.setText(mDataset.get(position).getStatus());

            ((DlvyViewHolder)holder).button_qr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), QRCodeActivity.class);
                    intent.putExtra("dlvy_num", Integer.toString(mDataset.get(position).getDlvyNum()));
                    intent.putExtra("car_num", Integer.toString(mDataset.get(position).getCarNum()));
                    intent.putExtra("user", "receiver");
                    v.getContext().startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).getViewType();
    }

    public class WaitViewHolder extends RecyclerView.ViewHolder {
        int dlvy_num;
        TextView textView_userId, textView_startPoint, textView_endPoint, textView_wait_num, textView_status;
        Button button_cancel;

        public WaitViewHolder(View view) {
            super(view);
            this.textView_userId = (TextView) view.findViewById(R.id.textView_userId);
            this.textView_startPoint = (TextView) view.findViewById(R.id.textView_startPoint);
            this.textView_endPoint = (TextView) view.findViewById(R.id.textView_endPoint);
            this.textView_wait_num = (TextView) view.findViewById(R.id.textView_wait_num);
            this.textView_status = (TextView) view.findViewById(R.id.textView_status);
            this.button_cancel = (Button) view.findViewById(R.id.button_cancel);
        }
    }

    public class DlvyWaitViewHolder extends RecyclerView.ViewHolder {
        int dlvy_num, car_num;
        TextView textView_userId, textView_startPoint, textView_endPoint, textView_time, textView_status;

        public DlvyWaitViewHolder(View view) {
            super(view);
            this.textView_userId = (TextView) view.findViewById(R.id.textView_userId);
            this.textView_startPoint = (TextView) view.findViewById(R.id.textView_startPoint);
            this.textView_endPoint = (TextView) view.findViewById(R.id.textView_endPoint);
            this.textView_time = (TextView) view.findViewById(R.id.textView_time);
            this.textView_status = (TextView) view.findViewById(R.id.textView_status);
        }
    }

    public class DlvyViewHolder extends RecyclerView.ViewHolder {
        int dlvy_num, car_num;
        TextView textView_userId, textView_startPoint, textView_endPoint, textView_time, textView_status;
        Button button_qr;

        public DlvyViewHolder(View view) {
            super(view);
            this.textView_userId = (TextView) view.findViewById(R.id.textView_userId);
            this.textView_startPoint = (TextView) view.findViewById(R.id.textView_startPoint);
            this.textView_endPoint = (TextView) view.findViewById(R.id.textView_endPoint);
            this.textView_time = (TextView) view.findViewById(R.id.textView_time);
            this.textView_status = (TextView) view.findViewById(R.id.textView_status);
            this.button_qr = (Button) view.findViewById(R.id.button_qr);
        }
    }
}