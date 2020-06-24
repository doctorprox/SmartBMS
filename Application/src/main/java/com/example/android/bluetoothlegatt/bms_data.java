package com.example.android.bluetoothlegatt;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class bms_data extends RecyclerView.Adapter<bms_data.ViewHolder> {

    private List<String> mData;
    private List<String> mData2;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;


    // data is passed into the constructor
    bms_data(Context context, List<String> data, List<String> data2) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.mData2 = data2;
    }

    public void setSoC(byte soc){
        mData2.set(1,Byte.toString(soc) + "%");
        return;
    }

    public void setCurrent(float remcap){
        mData2.set(3,Float.toString(remcap) + " A");
        return;
    }

    public void setPower(int remcap){
        mData2.set(4,Integer.toString(remcap) + " W");
        return;
    }

    public void setRemCap(float remcap){
        mData2.set(2,Float.toString(remcap) + " Ah");
        return;
    }

    public void setV(float voltage){
        mData2.set(0,Float.toString(voltage) + "V");
        return;
    }

    public void setTemp1(float temp){
        mData2.set(5,Float.toString(temp) + "°C");
        return;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.statsview, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String animal = mData.get(position);
        holder.myTextView.setText(animal);
        holder.myTextVal.setText((mData2.get(position)));
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;
        TextView myTextVal;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.data_label);
            myTextVal = itemView.findViewById(R.id.data_value);
            //itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}