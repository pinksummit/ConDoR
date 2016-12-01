package com.cohesiveintegrations.px.android.feed;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.data.metacard.Metacard;
import com.cohesiveintegrations.px.android.feed.metacard.MetacardQueryService;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

public class FeedTabNearbyFragment extends AbstractQueryFragment {

    private MapView mapView;
    private GoogleMap googleMap;
    private LayoutInflater inflater;

    public FeedTabNearbyFragment() {
        setQueryType(MetacardQueryService.QueryType.SPATIAL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        onCreate();
        final View rootView = inflater.inflate(R.layout.fragment_feed_tab_map, container, false);
        mapView = (MapView) rootView.findViewById(R.id.feed_map);
        mapView.onCreate(savedInstanceState);
        final Location location = getCurrentLocation();
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                googleMap = map;
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.getUiSettings().setMapToolbarEnabled(true);
                googleMap.setMyLocationEnabled(true);
                googleMap.setInfoWindowAdapter(new MetacardWindowAdapter());
                if (location != null) {
                    CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15);
                    map.moveCamera(yourLocation);
                }
            }
        });
        this.inflater = inflater;
        setCallback(new UpdateCallback());
        JSONObject criteria = new JSONObject();
        try {
            if (location != null) {
                criteria.put("lat", Double.toString(location.getLatitude()));
                criteria.put("lon", Double.toString(location.getLongitude()));
                criteria.put("radius", "100000");
            } else {
                criteria.put("q", "*");
            }
        } catch (JSONException je) {
            Log.w(FeedTabNearbyFragment.class.getName(), "Could not create JSON-encoded criteria", je);
        }
        refreshMetadata(rootView.getContext(), criteria.toString());

        return rootView;
    }

    @Override
    public void onResume() {
        if (googleMap != null) {
            googleMap.clear();
            for (Metacard metacard : metacardMap.values()) {
                if (metacard.getLatitude() != null && !metacard.getLatitude().isEmpty() && metacard.getLongitude() != null && !metacard.getLongitude().isEmpty()) {
                    MarkerOptions marker = new MarkerOptions().position(new LatLng(Double.parseDouble(metacard.getLatitude()),
                            Double.parseDouble(metacard.getLongitude()))).title(metacard.getId());
                    googleMap.addMarker(marker);
                }
            }
        }
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    class MetacardWindowAdapter implements GoogleMap.InfoWindowAdapter {

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            View contentView = inflater.inflate(R.layout.view_map_metacard, null);
            TextView title = (TextView) contentView.findViewById(R.id.metacard_title);
            TextView description = (TextView) contentView.findViewById(R.id.metacard_description);
            TextView date = (TextView) contentView.findViewById(R.id.metacard_date);
            ImageView thumbnail = (ImageView) contentView.findViewById(R.id.metacard_thumbnail);

            final Metacard metacard = metacardMap.get(marker.getTitle());
            title.setText(metacard.getTitle());
            description.setText(metacard.getContent());
            date.setText(DateFormat.getMediumDateFormat(contentView.getContext()).format(metacard.getModifiedDate()));
            Picasso.with(getActivity().getApplicationContext()).load(metacard.getThumbnail()).into(thumbnail);

            return contentView;
        }
    }

    private Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        // start with GPS
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            // next try network
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) {
                // finally just try to piggy-back off of another service
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
        }
        return location;
    }

    public class UpdateCallback implements MetadataUpdatedCallback {

        @Override
        public void metadataUpdated() {
            onResume();
            //refreshLayout.setRefreshing(false);
        }
    }


}
