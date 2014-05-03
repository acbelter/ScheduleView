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
import java.util.Calendar;
import java.util.Collections;

public class ScheduleAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    protected ArrayList<BaseScheduleItem> mItems;
    private int mStartHour;
    private int mEndHour;
    private SimpleDate mDate;

    public ScheduleAdapter(Context context, ArrayList<BaseScheduleItem> items,
                           int startHour, int endHour) throws InvalidScheduleException {
        super();
        mStartHour = startHour;
        mEndHour = endHour;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (items != null) {
            mItems = items;
        } else {
            mItems = new ArrayList<BaseScheduleItem>(0);
        }

        prepareAndCheckSchedule();
    }

    private void prepareAndCheckSchedule() throws InvalidScheduleException {
        if (mItems.isEmpty()) {
            return;
        }

        Collections.sort(mItems);
        mDate = new SimpleDate(mItems.get(0).start);
        for (int i = 0; i < mItems.size(); i++) {
            SimpleDate start = new SimpleDate(mItems.get(i).start);
            SimpleDate end = new SimpleDate(mItems.get(i).end);
            if (!mDate.equals(start)) {
                throw new InvalidScheduleException("Different start dates.");
            }
            if (!mDate.equals(end)) {
                throw new InvalidScheduleException("Different start and end dates.");
            }
        }

        if (mStartHour != 0 && getHour(mItems.get(0).start) < mStartHour) {
            throw new InvalidScheduleException("Incorrect first time mark.");
        }

        if (mEndHour != 0 && getHour(mItems.get(mItems.size() - 1).end) > mEndHour) {
            throw new InvalidScheduleException("Incorrect last time mark.");
        }


        for (int i = 0; i < mItems.size() - 1; i++) {
            if (mItems.get(i).end > mItems.get(i+1).start) {
                throw new InvalidScheduleException("Intersecting items.");
            }
        }
    }

    private int getHour(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public boolean add(BaseScheduleItem newItem) {
        if (mItems.isEmpty()) {
            mItems.add(newItem);
            return true;
        }

        SimpleDate start = new SimpleDate(newItem.start);
        SimpleDate end = new SimpleDate(newItem.end);
        if (!mDate.equals(start) || !mDate.equals(end)) {
            return false;
        }

        BaseScheduleItem item;
        for (int i = 0; i < mItems.size(); i++) {
            item = mItems.get(i);
            if (newItem.start == item.start || newItem.end == item.end) {
                return false;
            }
            if (newItem.start > item.start && newItem.start < item.end) {
                return false;
            }
            if (newItem.end > item.start && newItem.end < item.end) {
                return false;
            }
        }

        mItems.add(newItem);
        return true;
    }

    public int removeForId(Long id) {
        int removed = 0;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).id == id) {
                mItems.remove(i);
                i--;
                removed++;
            }
        }
        return removed;
    }

    public ArrayList<BaseScheduleItem> getItems() {
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
    public BaseScheduleItem getItem(int position) {
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

        holder.text.setText("id " + getItem(position).id);
        return convertView;
    }

    public static class InvalidScheduleException extends Exception {
        public InvalidScheduleException(String message) {
            super(message);
        }
    }

    private static class SimpleDate {
        int day;
        int month;
        int year;

        SimpleDate(long millis) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(millis);
            day = c.get(Calendar.DAY_OF_YEAR);
            month = c.get(Calendar.MONTH);
            year = c.get(Calendar.YEAR);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleDate that = (SimpleDate) o;

            if (day != that.day) return false;
            if (month != that.month) return false;
            if (year != that.year) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = day;
            result = 31 * result + month;
            result = 31 * result + year;
            return result;
        }
    }
}
