package com.acbelter.scheduleview;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    private ScheduleAdapter mAdapter;
    private boolean mButtonClicked;
    private ScheduleView mSchedule;
    private Button mTestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSchedule = (ScheduleView) findViewById(R.id.schedule);
        mSchedule.initTimeMarks(8, 0, true);
        mTestButton = (Button) findViewById(R.id.test_button);

        ArrayList<ScheduleItem> items;
        if (savedInstanceState == null) {
            items = new ArrayList<ScheduleItem>();
            for (int i = 0; i < 1; i++) {
                items.add(new ScheduleItem(i, 1393912800000L, 1393917900000L, "item " + i));
            }
        } else {
            items = savedInstanceState.getParcelableArrayList("items");
            mButtonClicked = savedInstanceState.getBoolean("button_clicked");
            if (mButtonClicked) {
                mTestButton.setText("Clear");
            }
        }

        mAdapter = new ScheduleAdapter(this, items);
        mSchedule.setAdapter(mAdapter);
        mSchedule.setOnItemClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putParcelableArrayList("items", mAdapter.getItems());
        }
        outState.putBoolean("button_clicked", mButtonClicked);
    }

    public void onTest(View view) {
        if (!mButtonClicked) {
            mAdapter.add(new ScheduleItem(100500, 1393912800000L, 1393917900000L, "new item"));
            mAdapter.notifyDataSetChanged();
            mButtonClicked = true;
            mTestButton.setText("Clear");
        } else {
            mAdapter = null;
            mSchedule.setAdapter(null);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(getApplicationContext(), "single tap: " + position, Toast.LENGTH_SHORT).show();
    }
}
