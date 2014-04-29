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

import android.os.Parcel;
import android.os.Parcelable;

public class ScheduleItem implements Parcelable {
    public long id;
    public long start;
    public long end;
    public String text;

    public ScheduleItem(long id, long start, long end, String text) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.text = text;
    }

    private ScheduleItem(Parcel in) {
        id = in.readLong();
        start = in.readLong();
        end = in.readLong();
        text = in.readString();
    }

    public static final Parcelable.Creator<ScheduleItem> CREATOR =
            new Parcelable.Creator<ScheduleItem>() {
                @Override
                public ScheduleItem createFromParcel(Parcel in) {
                    return new ScheduleItem(in);
                }

                @Override
                public ScheduleItem[] newArray(int size) {
                    return new ScheduleItem[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeLong(start);
        out.writeLong(end);
        out.writeString(text);
    }
}
