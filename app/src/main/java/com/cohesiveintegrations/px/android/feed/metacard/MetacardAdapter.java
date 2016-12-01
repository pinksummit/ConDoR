package com.cohesiveintegrations.px.android.feed.metacard;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.data.metacard.Metacard;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MetacardAdapter extends RecyclerView.Adapter<MetacardHolder> {

    private List<Metacard> metacardList = new ArrayList<>();

    private Map<String, Metacard> metacardMap;

    private Metacard.SortType sortType;

    private Context context;

    private Context applicationContext;

    public MetacardAdapter(Metacard.SortType sortType, Activity activity, Map<String, Metacard> metacardMap) {
        this.context = activity;
        this.applicationContext = activity.getApplicationContext();
        this.sortType = sortType;
        this.metacardMap = metacardMap;
    }

    @Override
    public MetacardHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.fragment_feed_metacard, viewGroup, false);
        return new MetacardHolder(view);
    }

    @Override
    public void onBindViewHolder(MetacardHolder metacardHolder, int i) {
        Metacard currentMetacard = metacardList.get(i);
        metacardHolder.setTitle(currentMetacard.getTitle());
        metacardHolder.setDescription(currentMetacard.getContent());
        metacardHolder.setSite(currentMetacard.getSite());
        metacardHolder.setDate(currentMetacard.getModifiedDate());
        metacardHolder.setMap(currentMetacard.getLatitude(), currentMetacard.getLongitude(), currentMetacard.getTitle(), context);
        metacardHolder.setProduct(currentMetacard.getProduct(), currentMetacard.getTitle(), context);
        if (currentMetacard.getThumbnail() != null) {
            String thumbnailString = currentMetacard.getThumbnail();
            metacardHolder.thumbnail.setVisibility(View.VISIBLE);
            //thumbnail is either a local image or a network URL
            try {
                // checks if the string is a url
                new URL(thumbnailString);
                // library that loads images from web
                Picasso.with(applicationContext).load(thumbnailString).into(metacardHolder.thumbnail);


            } catch (MalformedURLException mue) {
                // try local image
                try {
                    int thumbnailIndex = Integer.parseInt(thumbnailString);
                    metacardHolder.setThumbnail(BitmapFactory.decodeResource(context.getResources(), thumbnailIndex));
                } catch (NumberFormatException nfe) {
                    // unknown type of thumbnail, leaving blank in UI.
                }
            }
        } else {
            metacardHolder.thumbnail.setVisibility(View.INVISIBLE);
        }
        metacardHolder.setAction(currentMetacard.getActionType());
    }

    @Override
    public int getItemCount() {
        metacardList.clear();
        metacardList.addAll(metacardMap.values());
        Collections.sort(metacardList, new MetacardComparator());
        return metacardList.size();
    }

    private class MetacardComparator implements Comparator<Metacard> {

        @Override
        public int compare(Metacard lhs, Metacard rhs) {
            int compare = 0;
            switch (sortType) {
                case DISTANCE:
                    //TODO sort distance
                    break;
                case TIME:
                    compare = rhs.getModifiedDate().compareTo(lhs.getModifiedDate());
                    break;
                case SOCIAL:
                    compare = Double.compare(lhs.getScore(), rhs.getScore());
                    break;
            }
            return compare;
        }
    }

}
