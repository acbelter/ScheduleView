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

public class BaseScheduleItem implements Parcelable, Comparable<BaseScheduleItem> {
    public long id;
    public long start;
    public long end;

    public BaseScheduleItem(long id, long start, long end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    private BaseScheduleItem(Parcel in) {
        id = in.readLong();
        start = in.readLong();
        end = in.readLong();
    }

    public static final Parcelable.Creator<BaseScheduleItem> CREATOR =
            new Parcelable.Creator<BaseScheduleItem>() {
                @Override
                public BaseScheduleItem createFromParcel(Parcel in) {
                    return new BaseScheduleItem(in);
                }

                @Override
                public BaseScheduleItem[] newArray(int size) {
                    return new BaseScheduleItem[size];
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
    }

    @Override
    public int compareTo(BaseScheduleItem another) {
        if (start < another.start) {
            return -1;
        }
        if (start > another.start) {
            return 1;
        }
        return 0;
    }
}
