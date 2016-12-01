package com.cohesiveintegrations.px.android.feed;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.cohesiveintegrations.px.android.data.metacard.Metacard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sets up the tabs on the view pager and loads the fragments as the user swipes around.
 */
public class FeedPagerAdapter extends FragmentStatePagerAdapter {

    List<String> tabList = new ArrayList<>();
    Map<String, Fragment> fragmentMap = new HashMap<>();

    public FeedPagerAdapter(FragmentManager manager) {
        super(manager);
        tabList.add("Nearby");
        tabList.add("Latest");
        //tabList.add("Popular");
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        String tabName = tabList.get(position);
        // if fragment was already made, just pull it back up
        if (fragmentMap.containsKey(tabName)) {
            fragment = fragmentMap.get(tabName);
        } else {
            // create new fragment
            switch (position) {
                case 0:
                    //fragment = createSortedFragment(Metacard.SortType.DISTANCE);
                    fragment = new FeedTabNearbyFragment();
                    //fragment = new Fragment();
                    break;
                case 1:
                    fragment = createSortedFragment(Metacard.SortType.TIME);
                    break;
                case 2:
                    //fragment = createSortedFragment(Metacard.SortType.SOCIAL);
                    fragment = new Fragment();
                    break;
                default:
                    // shouldn't ever be called...but may help prevent NPEs
                    fragment = new Fragment();
            }
            fragmentMap.put(tabName, fragment);
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return tabList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabList.get(position);
    }

    private Fragment createSortedFragment(Metacard.SortType sortType) {
        Fragment fragment = new FeedTabLatestFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Metacard.SORT_TYPE_KEY, sortType.toString());
        fragment.setArguments(bundle);
        return fragment;
    }


}
