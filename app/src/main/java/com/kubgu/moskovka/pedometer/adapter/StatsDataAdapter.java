package com.kubgu.moskovka.pedometer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kubgu.moskovka.pedometer.R;

import java.util.List;
import java.util.zip.Inflater;

public class StatsDataAdapter extends ArrayAdapter<StatsItem> {
    private LayoutInflater inflater;
    private int layout;
    private List<StatsItem> statsItems;

    public StatsDataAdapter(Context context, int resource, List<StatsItem> statsItems)
    {
        super(context, resource, statsItems);
        this.statsItems = statsItems;
        this.layout = resource;
        this.inflater = LayoutInflater.from(context);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View view=inflater.inflate(this.layout, parent, false);

        TextView tvItemName = view.findViewById(R.id.item_name);
        TextView tvItemValue = view.findViewById(R.id.item_value);

        StatsItem state = statsItems.get(position);

        tvItemName.setText(state.getItemName());
        tvItemValue.setText(String.valueOf(state.getItemValue()));

        return view;
    }
}
