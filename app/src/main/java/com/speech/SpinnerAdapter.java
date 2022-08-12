package com.speech;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SpinnerAdapter extends ArrayAdapter<SpinnerItemData> {
    int groupid;
    Activity context;
    ArrayList<SpinnerItemData> list;
    LayoutInflater inflater;
    int selectedItem = 0;
    public SpinnerAdapter(Activity context, int groupid, int id, ArrayList<SpinnerItemData>
            list){
        super(context,id,list);
        this.context = context;
        this.list=list;
        inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.groupid=groupid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent ){
        View itemView=inflater.inflate(groupid, parent, false);
        LinearLayout outerLinearLayout = itemView.findViewById(R.id.outerLinearLayout);
        TextView textView = itemView.findViewById(R.id.spinnerTextview);
        textView.setText(list.get(position).getText());

        if ( (position & 1) == 0 ) {
            outerLinearLayout.setBackgroundColor(Color.rgb(230, 230, 230));
        } else {
            outerLinearLayout.setBackgroundColor(Color.rgb(255, 255, 255));
        }

        return itemView;
    }

    public View getDropDownView(int position,View convertView,ViewGroup parent) {
        return getView(position,convertView,parent);
    }

    public void setSelectedItem(int inputSelectedItem) {
        selectedItem = inputSelectedItem;
    }
}