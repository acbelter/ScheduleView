package com.acbelter.scheduleview;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    private static final int START_HOUR = 8;
    private static final int END_HOUR = 0;
    private ScheduleAdapter mAdapter;
    private boolean mNewItemAdded;
    private ScheduleView mSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSchedule = (ScheduleView) findViewById(R.id.schedule);
        mSchedule.initTimeMarks(START_HOUR, END_HOUR, true);
        mSchedule.setOnItemClickListener(this);

        ArrayList<BaseScheduleItem> items;
        if (savedInstanceState == null) {
            items = new ArrayList<BaseScheduleItem>();
            items.add(new BaseScheduleItem(0, 1393912800000L, 1393917900000L));
            items.add(new BaseScheduleItem(1, 1393919100000L, 1393924200000L));
            items.add(new BaseScheduleItem(2, 1393930500000L, 1393941300000L));
            items.add(new BaseScheduleItem(3, 1393952400000L, 1393959600000L));
        } else {
            items = savedInstanceState.getParcelableArrayList("items");
            mNewItemAdded = savedInstanceState.getBoolean("new_item_added");
        }

        try {
            if (items != null) {
                mAdapter = new ScheduleAdapter(this, items, START_HOUR, END_HOUR);
            }
            mSchedule.setAdapter(mAdapter);
        } catch (ScheduleAdapter.InvalidScheduleException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putParcelableArrayList("items", mAdapter.getItems());
        }
        outState.putBoolean("new_item_added", mNewItemAdded);
    }

    public void addNewItem(View view) {
        if (!mNewItemAdded) {
            if (mAdapter != null) {
                mAdapter.add(new BaseScheduleItem(100500,
                        1393941300000L + 60 * 60 * 1000,
                        1393941300000L + 2 * 60 * 60 * 1000));
                mAdapter.notifyDataSetChanged();
                mNewItemAdded = true;
            } else {
                Toast.makeText(getApplicationContext(), "Adapter is null.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "New element already added.", Toast.LENGTH_SHORT).show();
        }
    }

    public void setNullAdapter(View view) {
        mAdapter = null;
        mSchedule.setAdapter(null);
        mNewItemAdded = false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(getApplicationContext(), "Single tap: id=" + id, Toast.LENGTH_SHORT).show();
    }
}
