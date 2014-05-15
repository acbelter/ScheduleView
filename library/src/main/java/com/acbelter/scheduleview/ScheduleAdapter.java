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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

public class ScheduleAdapter extends BaseAdapter {
    protected ArrayList<GeneralScheduleItem> mItems;
    protected int mStartHour;
    protected int mEndHour;
    protected int mTimeZoneOffset;
    private SimpleDate mDate;

    public ScheduleAdapter(Context context, ArrayList<GeneralScheduleItem> items,
                           int startHour, int endHour, int timeZoneOffset)
            throws InvalidScheduleException {
        super();
        mStartHour = startHour;
        mEndHour = endHour;
        mTimeZoneOffset = timeZoneOffset;

        if (items != null) {
            mItems = items;
        } else {
            mItems = new ArrayList<GeneralScheduleItem>(0);
        }

        prepareAndCheckSchedule();
    }

    private TimeZone getTimeZone() {
        if (mTimeZoneOffset > 0) {
            return TimeZone.getTimeZone("GMT+" + mTimeZoneOffset);
        }
        if (mTimeZoneOffset < 0) {
            return TimeZone.getTimeZone("GMT-" + Math.abs(mTimeZoneOffset));
        }
        return TimeZone.getTimeZone("GMT+0");
    }

    private void prepareAndCheckSchedule() throws InvalidScheduleException {
        if (mItems.isEmpty()) {
            return;
        }

        Collections.sort(mItems, new GeneralScheduleItemComparator());
        mDate = new SimpleDate(mItems.get(0).getStartTime());
        for (int i = 0; i < mItems.size(); i++) {
            SimpleDate start = new SimpleDate(mItems.get(i).getStartTime());
            SimpleDate end = new SimpleDate(mItems.get(i).getEndTime());
            if (!mDate.equals(start)) {
                throw new InvalidScheduleException("Different start dates.");
            }
            if (!mDate.equals(end)) {
                throw new InvalidScheduleException("Different start and end dates.");
            }
        }

        if (mStartHour != 0 && getHour(mItems.get(0).getStartTime()) < mStartHour) {
            throw new InvalidScheduleException("Incorrect first time mark.");
        }

        if (mEndHour != 0 && getHour(mItems.get(mItems.size() - 1).getEndTime()) > mEndHour) {
            throw new InvalidScheduleException("Incorrect last time mark.");
        }

        for (int i = 0; i < mItems.size() - 1; i++) {
            if (mItems.get(i).getEndTime() > mItems.get(i+1).getStartTime()) {
                throw new InvalidScheduleException("Intersecting items.");
            }
        }
    }

    private int getHour(long millis) {
        Calendar c = Calendar.getInstance(getTimeZone());
        c.setTimeInMillis(millis);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public boolean add(GeneralScheduleItem newItem) {
        if (mItems.isEmpty()) {
            mItems.add(newItem);
            return true;
        }

        SimpleDate start = new SimpleDate(newItem.getStartTime());
        SimpleDate end = new SimpleDate(newItem.getEndTime());
        if (!mDate.equals(start) || !mDate.equals(end)) {
            return false;
        }

        GeneralScheduleItem item;
        for (int i = 0; i < mItems.size(); i++) {
            item = mItems.get(i);
            if (newItem.getStartTime() == item.getStartTime() ||
                    newItem.getEndTime() == item.getEndTime()) {
                return false;
            }
            if (newItem.getStartTime() > item.getStartTime() &&
                    newItem.getStartTime() < item.getEndTime()) {
                return false;
            }
            if (newItem.getEndTime() > item.getStartTime() &&
                    newItem.getEndTime() < item.getEndTime()) {
                return false;
            }
        }

        mItems.add(newItem);
        return true;
    }

    public int removeForId(Long id) {
        int removed = 0;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).getId() == id) {
                mItems.remove(i);
                i--;
                removed++;
            }
        }
        return removed;
    }

    public ArrayList<GeneralScheduleItem> getItems() {
        return mItems;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public GeneralScheduleItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    public static class InvalidScheduleException extends Exception {
        public InvalidScheduleException(String message) {
            super(message);
        }
    }

    private class SimpleDate {
        int day;
        int month;
        int year;

        SimpleDate(long millis) {
            Calendar c = Calendar.getInstance(getTimeZone());
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
