/*
 * Copyright 2014 acbelter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acbelter.scheduleview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class ScheduleAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    protected ArrayList<ScheduleItem> mItems;

    public ScheduleAdapter(Context context, ArrayList<ScheduleItem> items) throws InvalidScheduleException {
        super();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (items != null) {
            mItems = items;
        } else {
            mItems = new ArrayList<ScheduleItem>(0);
        }
        Collections.sort(mItems);
        if (!checkSchedule(mItems)) {
            throw new InvalidScheduleException("Invalid schedule.");
        }
    }

    private boolean checkSchedule(ArrayList<ScheduleItem> items) {
        if (items == null) {
            return true;
        }

        for (int i = 0; i < items.size() - 1; i++) {
            if (items.get(i).end > items.get(i+1).start ||
                    items.get(i).end - items.get(i+1).start > 24*60*60*10000) {
                return false;
            }
        }

        return true;
    }

    public void add(ScheduleItem item) {
        mItems.add(item);
    }

    public void removeForId(Long id) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).id == id) {
                mItems.remove(i);
                i--;
            }
        }
    }

    public ArrayList<ScheduleItem> getItems() {
        return mItems;
    }

    static class ViewHolder {
        TextView text;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public ScheduleItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).id;
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

        holder.text.setText(getItem(position).text);
        return convertView;
    }

    public static class InvalidScheduleException extends Exception {
        public InvalidScheduleException(String message) {
            super(message);
        }
    }
}
