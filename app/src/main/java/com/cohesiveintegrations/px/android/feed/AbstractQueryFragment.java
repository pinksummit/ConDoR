package com.cohesiveintegrations.px.android.feed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.cohesiveintegrations.px.android.data.metacard.Metacard;
import com.cohesiveintegrations.px.android.feed.metacard.MetacardQueryService;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract (building block) fragment that can be extended for fragments that need to perform queries.
 */
public class AbstractQueryFragment extends Fragment {

    protected Map<String, Metacard> metacardMap = new HashMap<>();

    private FeedStatusReceiver statusReceiver;

    private MetadataUpdatedCallback updatedCallback = null;

    private MetacardQueryService.QueryType queryType;

    public void onCreate() {
        IntentFilter filter = new IntentFilter(MetacardQueryService.BROADCAST_PREFIX + queryType.toString());
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        statusReceiver = new FeedStatusReceiver();
        getActivity().registerReceiver(statusReceiver, filter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setQueryType(MetacardQueryService.QueryType queryType) {
        this.queryType = queryType;
    }

    public void setCallback(MetadataUpdatedCallback callback) {
        updatedCallback = callback;
    }

    /**
     * Performs a new query and refreshes the metadata in the metadata map
     * @param context Context to create the intent with that the request will be sent on.
     * @param criteria JSON-encoded criteria to send.
     */
    public void refreshMetadata(Context context, String criteria) {
        Intent intent = new Intent(Intent.ACTION_SYNC, null, context, MetacardQueryService.class);
        intent.putExtra(MetacardQueryService.QUERY_TYPE, queryType);
        if (criteria != null && !criteria.isEmpty()) {
            intent.putExtra(MetacardQueryService.QUERY_CRITERIA, criteria);
        }
        getActivity().startService(intent);
    }



    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(statusReceiver);
        super.onDestroy();
    }

    public class FeedStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Map<String, Metacard> metacardResults = (Map<String, Metacard>) intent.getSerializableExtra(MetacardQueryService.RESULT_KEY);
            metacardMap.clear();
            metacardMap.putAll(metacardResults);
            if (updatedCallback != null) {
                updatedCallback.metadataUpdated();
            }
        }
    }

    public interface MetadataUpdatedCallback {
        void metadataUpdated();
    }

}
