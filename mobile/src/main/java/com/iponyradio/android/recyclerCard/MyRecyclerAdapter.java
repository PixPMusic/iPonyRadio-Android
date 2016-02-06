package com.iponyradio.android.recyclerCard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iponyradio.android.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MyRecyclerAdapter extends RecyclerView.Adapter<FeedListRowHolder> {


    private List<FeedItem> feedItemList;

    private Context mContext;

    public MyRecyclerAdapter(Context context, List<FeedItem> feedItemList) {
        this.feedItemList = feedItemList;
        this.mContext = context;
    }

    @Override
    public FeedListRowHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_card, null);
        FeedListRowHolder mh = new FeedListRowHolder(v);

        return mh;
    }

    @Override
    public void onBindViewHolder(FeedListRowHolder feedListRowHolder, int i) {
        FeedItem feedItem = feedItemList.get(i);

        if (!feedItem.getThumbnail().equals("ignore")) {
            Picasso.with(mContext).load(feedItem.getThumbnail())
                    .error(R.drawable.placeholder)
                    .placeholder(R.drawable.placeholder)
                    .into(feedListRowHolder.thumbnail);

//            Bitmap mDrawable = ((BitmapDrawable)feedListRowHolder.thumbnail.getDrawable()).getBitmap();
//            Palette p = Palette.from(mDrawable).generate();
//            int cardColor;
//            Palette.Swatch s;
//            int textColor;
//            if (p.getDarkVibrantColor(0xffffff) != 0xffffff) {
//                s = p.getDarkVibrantSwatch();
//                cardColor = p.getDarkVibrantColor(0xffffff);
//                textColor = s.getTitleTextColor();
//            } else if (p.getVibrantColor(0xffffff) != 0xffffff) {
//                s = p.getVibrantSwatch();
//                cardColor = p.getVibrantColor(0xffffff);
//                textColor = s.getTitleTextColor();
//            } else {
//                cardColor = 0xffffff;
//                textColor = 0x000000;
//            }
//            feedListRowHolder.card.setCardBackgroundColor(cardColor);
//            feedListRowHolder.title.setTextColor(textColor);
        }
        feedListRowHolder.title.setText(Html.fromHtml(feedItem.getTitle()));
    }

    @Override
    public int getItemCount() {
        return (null != feedItemList ? feedItemList.size() : 0);
    }
}