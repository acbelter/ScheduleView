package com.acbelter.scheduleviewdemo;

import android.os.Parcel;
import android.os.Parcelable;
import com.acbelter.scheduleview.GeneralScheduleItem;

public class MyScheduleItem implements GeneralScheduleItem {
    protected long mId;
    protected long mStart;
    protected long mEnd;

    public MyScheduleItem(long id, long start, long end) {
        mId = id;
        mStart = start;
        mEnd = end;
    }

    private MyScheduleItem(Parcel in) {
        mId = in.readLong();
        mStart = in.readLong();
        mEnd = in.readLong();
    }

    @Override
    public long getId() {
        return mId;
    }

    @Override
    public long getStartTime() {
        return mStart;
    }

    @Override
    public long getEndTime() {
        return mEnd;
    }

    public static final Parcelable.Creator<MyScheduleItem> CREATOR =
            new Parcelable.Creator<MyScheduleItem>() {
                @Override
                public MyScheduleItem createFromParcel(Parcel in) {
                    return new MyScheduleItem(in);
                }

                @Override
                public MyScheduleItem[] newArray(int size) {
                    return new MyScheduleItem[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeLong(mStart);
        out.writeLong(mEnd);
    }
}

