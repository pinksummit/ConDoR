package com.cohesiveintegrations.px.android.feed;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cohesiveintegrations.px.android.R;

/**
 * Main Fragment for the feed.
 */
public class FeedFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_feed, container, false);

        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.feed_pager);
        viewPager.setAdapter(new FeedPagerAdapter(getFragmentManager()));
        viewPager.setCurrentItem(1);

        return rootView;
    }

}