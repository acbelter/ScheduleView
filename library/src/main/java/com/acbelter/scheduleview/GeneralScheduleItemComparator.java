package com.acbelter.scheduleview;

import java.util.Comparator;

public class GeneralScheduleItemComparator implements Comparator<GeneralScheduleItem> {
    @Override
    public int compare(GeneralScheduleItem lhs, GeneralScheduleItem rhs) {
        if (lhs.getStartTime() < rhs.getStartTime()) {
            return -1;
        }
        if (lhs.getStartTime() > rhs.getStartTime()) {
            return 1;
        }
        return 0;
    }
}
