package com.acbelter.scheduleview;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static ScheduleAdapter sAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ScheduleView schedule = (ScheduleView) findViewById(R.id.schedule);

        List<String> data = new ArrayList<String>();
        for (int i = 0; i < 4; i++) {
            data.add("item " + i);
        }

        if (sAdapter == null) {
            sAdapter = new ScheduleAdapter(this, data);
        }
        schedule.setAdapter(sAdapter);
    }

    public void addTestItems(View view) {
        sAdapter.add("new item 1");
        sAdapter.add("new item 2");
        sAdapter.notifyDataSetChanged();
    }
}
