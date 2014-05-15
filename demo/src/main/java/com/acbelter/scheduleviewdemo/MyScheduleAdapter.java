package com.acbelter.scheduleviewdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.acbelter.scheduleview.GeneralScheduleItem;
import com.acbelter.scheduleview.ScheduleAdapter;

import java.util.ArrayList;

public class MyScheduleAdapter extends ScheduleAdapter {
    private LayoutInflater mInflater;

    public MyScheduleAdapter(Context context, ArrayList<GeneralScheduleItem> items,
                             int startHour, int endHour, int timeZoneOffset)
            throws InvalidScheduleException {
        super(context, items, startHour, endHour, timeZoneOffset);
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    static class ViewHolder {
        TextView text;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_schedule, null);

            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText("id " + getItem(position).getId());
        return convertView;
    }
}
