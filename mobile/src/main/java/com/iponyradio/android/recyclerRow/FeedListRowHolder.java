package com.iponyradio.android.recyclerRow;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.iponyradio.android.R;

public class FeedListRowHolder extends RecyclerView.ViewHolder {
    protected TextView title;

    public FeedListRowHolder(View view) {
        super(view);
        this.title = (TextView) view.findViewById(R.id.title);
    }

}