package com.uhf.scanlable;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class SwipeListView extends ListView implements View.OnTouchListener {
    // 这个构造方法用于 XML 加载
    public SwipeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);
    }

    public SwipeListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
    }

    private float downX;

    public SwipeListView(Context context) {
        super(context);
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                float deltaX = event.getX() - downX;
                if (deltaX < -200) { // 向左滑超过 200px 触发删除
                    int position = pointToPosition((int) event.getX(), (int) event.getY());
                    if (position != INVALID_POSITION) {
                        removeItem(position);
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void removeItem(int position) {
        ((ScanMode.MyAdapter) getAdapter()).removeItem(position);
    }
}
