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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.OverScroller;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// TODO Remove non visible views
// TODO Add tests
public class ScheduleView extends AdapterView<ScheduleAdapter> {
    private static final boolean DEBUG = false;
    private static final String DEBUG_TAG = "ScheduleView";
    private static final long MILLIS_IN_HOUR = 60*60*1000;
    private static final int ALPHA_PERCENT = 50;
    // The distance between the first time mark and the top of the view
    private int mInternalPaddingTop;
    // The distance between the last time mark and the bottom of the view
    private int mInternalPaddingBottom;
    // The distance between adjacent time marks
    private int mTimeMarksDistance;

    private LayoutInflater mInflater;
    private ScheduleAdapter mAdapter;

    private View mTimeMark;
    private int mTimeMarkHeight;

    private int mHourHeight;
    private long mStartTime;
    private long mEndTime;

    private int mViewWidth;
    private int mViewHeight;
    // Width of the schedule items
    private int mItemWidth;
    private int mBackgroundHeight;
    private int mOldBackgroundHeight = -1;
    // The difference between the height of background and the height of the view
    private int mDeltaHeight;

    private int mLeftPadding;
    private int mRightPadding;
    private ArrayList<String> mTimeMarks;
    // Top of the list position
    private int mListY;
    private int mScrollDirection;

    private Rect mClipRect;
    private Rect mClickedViewBounds;

    private DataSetObserver mDataSetObserver;

    private GestureDetector mGestureDetector;
    private GestureDetector.OnGestureListener mGestureListener;
    private OverScroller mOverScroller;

    private EdgeEffectCompat mTopEdgeEffect;
    private EdgeEffectCompat mBottomEdgeEffect;

    private boolean mTopEdgeEffectActive;
    private boolean mBottomEdgeEffectActive;

    private ActionMode mActionMode;
    private boolean mIsActionMode;
    private ActionMode.Callback mActionModeCallback;

    private Set<Long> mSelectedIds;
    private Drawable mItemSelector;

    public ScheduleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mClipRect = new Rect();
        mClickedViewBounds = new Rect();
        mSelectedIds = new HashSet<Long>();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItemSelector = getResources().getDrawable(R.drawable.item_selector);

        mTopEdgeEffect = new EdgeEffectCompat(context);
        mBottomEdgeEffect = new EdgeEffectCompat(context);

        mDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                removeAllViewsInLayout();
                requestLayout();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                removeAllViewsInLayout();
                requestLayout();
            }
        };

        init(context);

        setVerticalScrollBarEnabled(true);
        setHorizontalScrollBarEnabled(false);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScheduleView);
        try {
            if (a != null) {
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                mInternalPaddingTop = (int) a.getDimension(R.styleable.ScheduleView_internal_paddingTop,
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, dm));
                mInternalPaddingBottom = (int) a.getDimension(R.styleable.ScheduleView_internal_paddingBottom,
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, dm));
                mTimeMarksDistance = (int) a.getDimension(R.styleable.ScheduleView_timeMarksDistance,
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, dm));
                mLeftPadding = (int) a.getDimension(R.styleable.ScheduleView_item_paddingLeft,
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, dm));
                mRightPadding = (int) a.getDimension(R.styleable.ScheduleView_item_paddingRight,
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, dm));
                initializeScrollbars(a);
            }
        } finally {
            if (a != null) {
                a.recycle();
            }
        }

        // Draw the background even if no items to display
        setWillNotDraw(false);

        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_context, menu);
                mIsActionMode = true;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.delete_items) {
                    deleteSelectedItems();
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                clearSelection();
                mIsActionMode = false;
                mActionMode = null;
                invalidate();
            }
        };
    }

    /**
     * @param startDayHour 0-23
     * @param endDayHour 0-23
     * @param format24
     */
    public void initTimeMarks(int startDayHour, int endDayHour, boolean format24) {
        if (startDayHour < 0 || startDayHour > 23) {
            throw new IllegalArgumentException("Incorrect startDayMinutes.");
        }

        if (endDayHour < 0 || endDayHour > 23) {
            throw new IllegalArgumentException("Incorrect endDayMinutes.");
        }

        if (startDayHour == endDayHour) {
            throw new IllegalArgumentException("Incorrect arguments: startDayMinutes=endDayMinutes.");
        }

        mStartTime = startDayHour * MILLIS_IN_HOUR;
        mEndTime = endDayHour * MILLIS_IN_HOUR;
        fillTimeMarksTitles(startDayHour, endDayHour, format24);
        initTimeMarkView(format24);
    }

    private void fillTimeMarksTitles(int startDayHour, int endDayHour, boolean format24) {
        SimpleDateFormat timeFormat;
        if (format24) {
            timeFormat = new SimpleDateFormat("HH:mm");
        } else {
            timeFormat = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
        }

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, startDayHour);
        c.set(Calendar.MINUTE, 0);

        mTimeMarks = new ArrayList<String>();
        while (c.get(Calendar.HOUR_OF_DAY) != endDayHour) {
            mTimeMarks.add(timeFormat.format(new Date(c.getTimeInMillis())));
            c.add(Calendar.HOUR_OF_DAY, 1);
            if (c.get(Calendar.HOUR_OF_DAY) == endDayHour) {
                mTimeMarks.add(timeFormat.format(new Date(c.getTimeInMillis())));
            }
        }
    }

    private void deleteSelectedItems() {
        for (Long id : mSelectedIds) {
            mAdapter.removeForId(id);
        }
        mAdapter.notifyDataSetChanged();
        mSelectedIds.clear();
        finishActionMode();
    }

    private void init(Context context) {
        if (!isInEditMode()) {
            mOverScroller = new OverScroller(context);
            mGestureListener = new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    if (DEBUG) {
                        Log.d(DEBUG_TAG, "onDown() y=" + mListY);
                    }

                    releaseEdgeEffects();
                    mOverScroller.forceFinished(true);
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (DEBUG) {
                        Log.d(DEBUG_TAG, "onFling() y=" + mListY);
                    }

                    // Fling isn't needed
                    if (mDeltaHeight < 0) {
                        return true;
                    }

                    mScrollDirection = velocityY > 0 ? 1 : -1;
                    mOverScroller.fling(0, mListY, 0, (int) velocityY, 0, 0, -mDeltaHeight, 0);
                    if (!awakenScrollBars()) {
                        ViewCompat.postInvalidateOnAnimation(ScheduleView.this);
                    }
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    mListY -= (int) distanceY;
                    recalculateOffset();

                    positionItemViews();

                    if (mListY == 0) {
                        mTopEdgeEffect.onPull(distanceY / (float) getHeight());
                        mTopEdgeEffectActive = true;
                    }
                    if (mListY == -mDeltaHeight) {
                        mBottomEdgeEffect.onPull(distanceY / (float) getHeight());
                        mBottomEdgeEffectActive = true;
                    }

                    if (!awakenScrollBars()) {
                        invalidate();
                    }

                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    if (DEBUG) {
                        Log.d(DEBUG_TAG, "onLongPress() y=" + mListY);
                    }

                    View child;
                    for (int i = 0; i < getChildCount(); i++) {
                        child = getChildAt(i);
                        child.getHitRect(mClickedViewBounds);
                        if (mClickedViewBounds.contains((int) e.getX(), (int) e.getY())) {
                            if (!mIsActionMode) {
                                mActionMode = startActionMode(mActionModeCallback);
                                mIsActionMode = true;
                            }

                            if (!child.isSelected()) {
                                mSelectedIds.add(mAdapter.getItemId(i));
                                child.setSelected(true);
                            } else {
                                mSelectedIds.remove(mAdapter.getItemId(i));
                                child.setSelected(false);
                            }

                            if (mSelectedIds.isEmpty()) {
                                finishActionMode();
                            }

                            invalidate();
                            return;
                        }
                    }
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    if (DEBUG) {
                        Log.d(DEBUG_TAG, "onSingleTapConfirmed() y=" + mListY);
                    }

                    View child;
                    for (int i = 0; i < getChildCount(); i++) {
                        child = getChildAt(i);
                        child.getHitRect(mClickedViewBounds);
                        if (mClickedViewBounds.contains((int) e.getX(), (int) e.getY())) {
                            if (!mIsActionMode) {
                                OnItemClickListener callback = getOnItemClickListener();

                                if (callback != null) {
                                    callback.onItemClick(ScheduleView.this, child, i, mAdapter.getItemId(i));
                                }
                            } else {
                                if (!child.isSelected()) {
                                    mSelectedIds.add(mAdapter.getItemId(i));
                                    child.setSelected(true);
                                } else {
                                    mSelectedIds.remove(mAdapter.getItemId(i));
                                    child.setSelected(false);
                                }

                                if (mSelectedIds.isEmpty()) {
                                    finishActionMode();
                                }

                                invalidate();
                            }
                            break;
                        }
                    }
                    return true;
                }
            };

            mGestureDetector = new GestureDetector(context, mGestureListener);
        }
    }

    private void setSelection() {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (mSelectedIds.contains(mAdapter.getItemId(i))) {
                getChildAt(i).setSelected(true);
            } else {
                getChildAt(i).setSelected(false);
            }
        }
    }

    private void clearSelection() {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setSelected(false);
        }
        mSelectedIds.clear();
    }

    private void recalculateOffset() {
        if (mDeltaHeight > 0) {
            // Background height is more than screen height
            if (mListY < -mDeltaHeight) {
                mListY = -mDeltaHeight;
            }
        } else {
            // Screen height is more than background height
            mListY = 0;
        }

        if (mListY > 0) {
            mListY = 0;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private int getCurrentVelocity() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return (int) mOverScroller.getCurrVelocity();
        }
        return 0;
    }

    private static class ViewHolder {
        public TextView time;
        public View timeLine;
    }

    private void initTimeMarkView(boolean format24) {
        if (mTimeMark == null) {
            if (format24) {
                mTimeMark = mInflater.inflate(R.layout.time_mark_24, null);
            } else {
                mTimeMark = mInflater.inflate(R.layout.time_mark_12, null);
            }

            ViewHolder holder = new ViewHolder();
            holder.time = (TextView) mTimeMark.findViewById(R.id.time);
            holder.timeLine = mTimeMark.findViewById(R.id.time_line);

            mTimeMark.setTag(holder);
        }
    }

    @Override
    public ScheduleAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setAdapter(ScheduleAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }

        clearSelection();
        finishActionMode();
        removeAllViewsInLayout();
        requestLayout();
        invalidate();
    }

    private void finishActionMode() {
        if (mIsActionMode) {
            mActionMode.finish();
            mActionMode = null;
            mIsActionMode = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
    }

    @Override
    public View getSelectedView() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSelection(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Parcelable onSaveInstanceState() {
        ScheduleState ss = new ScheduleState(super.onSaveInstanceState());
        ss.listY = mListY;
        ss.backgroundHeight = mBackgroundHeight;
        ss.isActionMode = mIsActionMode;
        ss.selectedIds = mSelectedIds;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof ScheduleState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        ScheduleState ss = (ScheduleState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mListY = ss.listY;
        mOldBackgroundHeight = ss.backgroundHeight;
        mIsActionMode = ss.isActionMode;
        mSelectedIds = ss.selectedIds;

        if (mIsActionMode) {
            mActionMode = startActionMode(mActionModeCallback);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
        mViewHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mViewWidth, mViewHeight);

        mTimeMark.measure(MeasureSpec.makeMeasureSpec(mViewWidth
                        - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        ViewHolder holder = (ViewHolder) mTimeMark.getTag();
        mItemWidth = holder.timeLine.getMeasuredWidth() - mLeftPadding - mRightPadding;
        mTimeMarkHeight = mTimeMark.getMeasuredHeight();
        mHourHeight = mTimeMarkHeight + mTimeMarksDistance;

        mBackgroundHeight = calculateBackgroundHeight();
        mDeltaHeight = mBackgroundHeight - mViewHeight;

        // Correct scroll position if another time mark layout is used
        if (mOldBackgroundHeight != -1) {
            float ratio = (float) mBackgroundHeight / mOldBackgroundHeight;
            mListY = (int) (mListY * ratio);
            recalculateOffset();
            mOldBackgroundHeight = -1;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mAdapter == null || mAdapter.isEmpty()) {
            return;
        }

        if (getChildCount() == 0) {
            addAndMeasureItemViews();
        }

        positionItemViews();
        invalidate();
    }

    private void addAndMeasureItemViews() {
        View child;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            // TODO Cache non visible views and use them
            child = mAdapter.getView(i, null, this);
            ViewGroup.LayoutParams params = child.getLayoutParams();
            if (params == null) {
                params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            }
            addViewInLayout(child, i, params, true);

            BaseScheduleItem item = mAdapter.getItem(i);
            child.measure(MeasureSpec.makeMeasureSpec(mItemWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(calculateDistance(item.start, item.end), MeasureSpec.EXACTLY));
        }
        setSelection();
    }

    private int calculateDistance(long start, long end) {
        float hours = (float) (end - start) / MILLIS_IN_HOUR;
        return (int) (hours * mHourHeight);
    }

    private void positionItemViews() {
        if (mAdapter == null) {
            return;
        }

        View child;
        final int right = mViewWidth - getPaddingRight() - mRightPadding;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            child = getChildAt(i);
            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();

            BaseScheduleItem item = mAdapter.getItem(i);

            int top = mListY + mInternalPaddingTop + getPaddingTop() + mTimeMarkHeight / 2 +
                    calculateDistance(mStartTime, getTimeInMillis(item.start));

            child.layout(right - width, top, right, top + height);
        }
    }

    private long getTimeInMillis(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        return (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE))*60*1000;
    }

    private void releaseEdgeEffects() {
        mTopEdgeEffectActive = false;
        mBottomEdgeEffectActive = false;
        mTopEdgeEffect.onRelease();
        mBottomEdgeEffect.onRelease();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        View child;
        for (int i = 0; i < getChildCount(); i++) {
            child = getChildAt(i);
            child.getHitRect(mClickedViewBounds);
            if (mClickedViewBounds.contains((int) e.getX(), (int) e.getY())) {
                if (DEBUG) {
                    Log.d(DEBUG_TAG, "dispatchTouchEvent() to child " + i);
                }
                // FIXME Add fade animation
                child.dispatchTouchEvent(e);
            }
        }
        return mGestureDetector.onTouchEvent(e);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        boolean needsInvalidate = false;
        if (mOverScroller.computeScrollOffset()) {
            mListY = mOverScroller.getCurrY();

            positionItemViews();

            if (mOverScroller.isOverScrolled()) {
                if (mScrollDirection > 0) {
                    mListY = 0;
                } else if (mScrollDirection < 0) {
                    mListY = -mDeltaHeight;
                }

                if (mListY == 0 && !mTopEdgeEffectActive) {
                    mTopEdgeEffect.onAbsorb(getCurrentVelocity());
                    mTopEdgeEffectActive = true;
                    needsInvalidate = true;
                } else if (mListY == -mDeltaHeight && !mBottomEdgeEffectActive) {
                    mBottomEdgeEffect.onAbsorb(getCurrentVelocity());
                    mBottomEdgeEffectActive = true;
                    needsInvalidate = true;
                }
            }
        }

        if (!mOverScroller.isFinished()) {
            needsInvalidate = true;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return mViewHeight;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return -mListY;
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mBackgroundHeight;
    }

    private int calculateBackgroundHeight() {
        return mInternalPaddingTop + getPaddingTop() + mTimeMarks.size() * (mTimeMarkHeight + mTimeMarksDistance) +
                mInternalPaddingBottom + getPaddingBottom();
    }

    private void changeClipRect(Canvas canvas) {
        canvas.getClipBounds(mClipRect);
        mClipRect.left += getPaddingLeft();
        mClipRect.top += getPaddingTop();
        mClipRect.right -= getPaddingRight();
        mClipRect.bottom -= getPaddingBottom();
        canvas.clipRect(mClipRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        changeClipRect(canvas);

        canvas.save();
        canvas.translate(getPaddingLeft(), mListY + mInternalPaddingTop + getPaddingTop());
        ViewHolder holder = (ViewHolder) mTimeMark.getTag();
        for (int i = 0; i < mTimeMarks.size(); i++) {
            holder.time.setText(mTimeMarks.get(i));
            mTimeMark.layout(0, 0, mTimeMark.getMeasuredWidth(), mTimeMark.getMeasuredHeight());
            mTimeMark.draw(canvas);

            if (i != mTimeMarks.size() - 1) {
                canvas.translate(0, mTimeMarkHeight + mTimeMarksDistance);
            }
        }
        canvas.restore();

        drawEdgeEffects(canvas);
    }

    private void drawEdgeEffects(Canvas canvas) {
        boolean needsInvalidate = false;

        final int overScrollMode = ViewCompat.getOverScrollMode(this);
        if (overScrollMode == ViewCompat.OVER_SCROLL_ALWAYS
                || overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS) {
            if (!mTopEdgeEffect.isFinished()) {
                int saveCount = canvas.save();
                int width = mViewWidth - getPaddingLeft() - getPaddingRight();
                int height = mViewHeight - getPaddingTop() - getPaddingBottom();

                canvas.translate(0, getPaddingTop());

                mTopEdgeEffect.setSize(width, height);
                needsInvalidate |= mTopEdgeEffect.draw(canvas);
                canvas.restoreToCount(saveCount);
            }
            if (!mBottomEdgeEffect.isFinished()) {
                int saveCount = canvas.save();
                int width = mViewWidth - getPaddingLeft() - getPaddingRight();
                int height = mViewHeight - getPaddingTop() - getPaddingBottom();

                canvas.translate(mViewWidth, mViewHeight - getPaddingBottom());
                canvas.rotate(180);

                mBottomEdgeEffect.setSize(width, height);
                needsInvalidate |= mBottomEdgeEffect.draw(canvas);
                canvas.restoreToCount(saveCount);
            }
        } else {
            mTopEdgeEffect.finish();
            mBottomEdgeEffect.finish();
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mAdapter == null) {
            return;
        }

        Rect bounds = new Rect();
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).isSelected()) {
                getChildAt(i).getHitRect(bounds);
                mItemSelector.setBounds(bounds);
                mItemSelector.setAlpha((int) (ALPHA_PERCENT * 2.55f));
                mItemSelector.draw(canvas);
            }
        }
    }

    public static class ScheduleState extends BaseSavedState {
        int listY;
        int backgroundHeight;
        boolean isActionMode;
        Set<Long> selectedIds;

        public ScheduleState(Parcelable superState) {
            super(superState);
            selectedIds = new HashSet<Long>();
        }

        private ScheduleState(Parcel in) {
            super(in);
            listY = in.readInt();
            backgroundHeight = in.readInt();
            isActionMode = in.readInt() == 1;
            Long[] ids = (Long[]) in.readArray(Long.class.getClassLoader());
            selectedIds = new HashSet<Long>(Arrays.asList(ids));
        }

        public static final Parcelable.Creator<ScheduleState> CREATOR = new Parcelable.Creator<ScheduleState>() {
            @Override
            public ScheduleState createFromParcel(Parcel in) {
                return new ScheduleState(in);
            }

            @Override
            public ScheduleState[] newArray(int size) {
                return new ScheduleState[0];
            }
        };

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(listY);
            out.writeInt(backgroundHeight);
            out.writeInt(isActionMode ? 1 : 0);
            out.writeArray(selectedIds.toArray(new Long[selectedIds.size()]));
        }
    }
}
