package com.acbelter.scheduleview;

import android.os.Parcelable;

public interface GeneralScheduleItem extends Parcelable {
    long getId();
    long getStartTime();
    long getEndTime();
}
