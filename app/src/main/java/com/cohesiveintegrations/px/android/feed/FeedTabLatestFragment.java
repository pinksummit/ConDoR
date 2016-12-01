package com.cohesiveintegrations.px.android.feed;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.authentication.UserStore;
import com.cohesiveintegrations.px.android.data.metacard.Metacard;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;
import com.cohesiveintegrations.px.android.ddms.DDMSConstants;
import com.cohesiveintegrations.px.android.feed.metacard.MetacardAdapter;
import com.cohesiveintegrations.px.android.feed.metacard.MetacardIngestService;
import com.cohesiveintegrations.px.android.feed.metacard.MetacardQueryService;
import com.cohesiveintegrations.px.android.util.AppUtils;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that contains the content for an individual tab within the feed.
 */
public class FeedTabLatestFragment extends AbstractQueryFragment {

    private static final String DDMS_TEMPLATE_ASSET = "ddms-template.xml";

    private static final String JSON_TEMPLATE = "{\n" +
            "    \"properties\": {\n" +
            "        \"title\": \"$TITLE$\",\n" +
            "        \"thumbnail\": \"$THUMBNAIL$\",\n" +
            "        \"resource-uri\": \"$RESOURCE$\",\n" +
            "        \"resource-size\": \"$RES_SIZE$\",\n" +
            "        \"created\": \"$CREATED$\",\n" +
            "        \"metadata-content-type-version\": \"myVersion\",\n" +
            //"        \"metadata-content-type\": \"$TYPE$\",\n" +
            "        \"metadata-content-type\": \"$COLLECTIONS$\",\n" +
            "        \"metadata\": \"$METADATA$\",\n" +
            "        \"modified\": \"$MODIFIED$\",\n" +
            "        \"collections\": \"$COLLECTIONS$\"\n" +
            "    },\n" +
            "    \"type\": \"Feature\",\n" +
            "    $GEOMETRY$" +
            "} ";

    private static final String JSON_GEOMETRY_TEMPLATE =
            "    \"geometry\": {\n" +
                    "        \"type\": \"Point\",\n" +
                    "        \"coordinates\": [\n" +
                    "            $LONG$,\n" +
                    "            $LAT$\n" +
                    "        ]\n" +
                    "    }\n";

    private static final String JSON_SIMPLE_METADATA = "\"<xml><title>$TITLE$</title><content>$CONTENT$</content></xml>\"";

    private static final String TITLE_KEY = "$TITLE$";
    private static final String THUMBNAIL_KEY = "$THUMBNAIL$";
    private static final String CREATED_KEY = "$CREATED$";
    private static final String MODIFIED_KEY = "$MODIFIED$";
    private static final String TYPE_KEY = "$TYPE$";
    private static final String CONTENT_KEY = "$CONTENT$";
    private static final String LONGITUDE_KEY = "$LONG$";
    private static final String LATITUDE_KEY = "$LAT$";
    private static final String COLLECTIONS_KEY = "$COLLECTIONS$";
    private static final String GEOMETRY_KEY = "$GEOMETRY$";
    private static final String METADATA_KEY = "$METADATA$";

    private static final String IMAGE_NAME = "px_android_image";
    private static final String VIDEO_NAME = "px_android_video";
    // TODO if activity is allowed to change orientation this variable will be deleted causing NPEs
    private File mediaFile;

    private static final int NOTIFICATION_ID = 0;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_VIDEO_CAPTURE = 2;
    static final int REQUEST_GALLERY_CAPTURE = 3;

    private Context ROOT_CONTEXT;
    private PackageManager pm;
    private ViewGroup container;
    private LayoutInflater inflater;
    private NotificationManager notificationManager;
    private File mediaLocation;

    private MetacardAdapter metacardAdapter;

    private SwipeRefreshLayout refreshLayout;

    private JsonObject criteria;

    private static final String SEARCH_TERMS_KEY = "q";
    private static final String COLLECTIONS_TERMS_KEY = "metadata-content-type";

    private int contentCollectionSelected = R.id.query_radio_all;

    public FeedTabLatestFragment() {
        super();
        setQueryType(MetacardQueryService.QueryType.LATEST);
        criteria = new JsonObject();
        criteria.addProperty(SEARCH_TERMS_KEY, "*");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // set up the initial info on the abstract parent
        super.onCreate();

        this.inflater = inflater;
        this.container = container;

        // configure the internal views and layouts
        Metacard.SortType sortType = Metacard.SortType.valueOf(getArguments().getString(Metacard.SORT_TYPE_KEY));
        final View rootView = inflater.inflate(R.layout.fragment_feed_tab_list, container, false);

        ROOT_CONTEXT = rootView.getContext();
        mediaLocation = ROOT_CONTEXT.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // set up the card view
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.feedlist);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        metacardAdapter = new MetacardAdapter(sortType, getActivity(), metacardMap);
        recyclerView.setAdapter(metacardAdapter);

        // set up swipe-to-refresh
        refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.feedSwipeRefreshLayout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.setRefreshing(true);
                refreshMetadata(rootView.getContext(), criteria.toString());
            }
        });

        // allows other methods to check for dependencies (camera/video..etc) before sending out intents
        pm = ROOT_CONTEXT.getPackageManager();

        setupImageButton((ImageButton) rootView.findViewById(R.id.listPicture));
        setupVideoButton((ImageButton) rootView.findViewById(R.id.listVideo));
        setupSearchButton((ImageButton) rootView.findViewById(R.id.listSearch));
        setupGalleryButton((ImageButton) rootView.findViewById(R.id.listUpload));

        setCallback(new UpdateCallback());

        // get metadata on initial load
        refreshLayout.setRefreshing(true);
        refreshMetadata(rootView.getContext(), criteria.toString());

        notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        return rootView;
    }

    private void setupImageButton(ImageButton imageButton) {
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(pm) != null) {
                    try {
                        mediaFile = File.createTempFile(IMAGE_NAME, ".jpg", mediaLocation);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mediaFile));
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Could not access storage to for camera to save picture, cannot take picture.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Could not access camera, cannot take picture.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupVideoButton(ImageButton videoButton) {
        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (takeVideoIntent.resolveActivity(pm) != null) {
                    try {
                        mediaFile = File.createTempFile(VIDEO_NAME, ".mp4", mediaLocation);
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mediaFile));
                        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Could not access storage to for camera to save video, cannot take video.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Could not access camera, cannot take video.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupGalleryButton(final ImageButton galleryButton) {
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQUEST_GALLERY_CAPTURE);
            }
        });
    }

    private void setupSearchButton(ImageButton searchButton) {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View dialogView = inflater.inflate(R.layout.view_query_dialog, container, false);
                final EditText searchTermsText = (EditText) dialogView.findViewById(R.id.query_terms_text);
                String searchTerms = criteria.get(SEARCH_TERMS_KEY).getAsString();
                searchTermsText.setText(searchTerms);

                final RadioGroup collectionsGroup = (RadioGroup) dialogView.findViewById(R.id.query_radio_group);
                collectionsGroup.check(contentCollectionSelected);

                //Log.i("ContentCollection", "Button: " + getString(collectionsGroup.getCheckedRadioButtonId() ));
                final AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(ROOT_CONTEXT, android.R.style.Theme_Holo_Light_Dialog_NoActionBar))
                        .setTitle("Perform Query")
                        .setView(dialogView)
                        .setPositiveButton("Query", null)
                        .setNegativeButton("Cancel", null).create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //get search term
                        criteria.addProperty(SEARCH_TERMS_KEY, searchTermsText.getText().toString());
                        //get collections
                        switch (collectionsGroup.getCheckedRadioButtonId()) {
                            case (R.id.query_radio_all):
                                criteria.remove(COLLECTIONS_TERMS_KEY);
                                contentCollectionSelected = R.id.query_radio_all;
                                break;
                            case (R.id.query_radio_image):
                                criteria.addProperty(COLLECTIONS_TERMS_KEY, "MobileImagery");
                                contentCollectionSelected = R.id.query_radio_image;
                                break;
                            case (R.id.query_radio_video):
                                criteria.addProperty(COLLECTIONS_TERMS_KEY, "MobileVideo");
                                contentCollectionSelected = R.id.query_radio_video;
                                break;
                            case (R.id.query_radio_sensor):
                                criteria.addProperty(COLLECTIONS_TERMS_KEY, "SensorImagery");
                                contentCollectionSelected = R.id.query_radio_sensor;
                                break;
                        }
                        refreshLayout.setRefreshing(true);
                        refreshMetadata(view.getContext(), criteria.toString());
                        dialog.dismiss();
                    }
                });
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            final Date date = new Date();
            final Location location = getCurrentLocation();

            // components inside view
            View dialogView = inflater.inflate(R.layout.view_ingest_dialog, container, false);
            final EditText titleView = (EditText) dialogView.findViewById(R.id.ingest_title);
            final EditText contentView = (EditText) dialogView.findViewById(R.id.ingest_content);
            final TextView locationView = (TextView) dialogView.findViewById(R.id.ingest_location);
            DecimalFormat df = new DecimalFormat("#.0000");

            if (location != null) {
                locationView.setText("(" + df.format(location.getLongitude()) + ", " + df.format(location.getLatitude()) + ")");
            }

            final String type;
            final String mimeType;
            final String collections;
            final Bitmap thumbnail;

            // image was taken
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                thumbnail = getBitmap( mediaFile.getAbsolutePath() );
                type = "Image";
                mimeType = "image/jpeg";
                collections = "MobileImagery";
                //video was taken
            } else if (requestCode == REQUEST_VIDEO_CAPTURE) {
                //create thumbnail
                thumbnail = ThumbnailUtils.createVideoThumbnail(mediaFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
                type = "Video";
                mimeType = "video/mp4";
                collections = "MobileVideo";

                //gallery was used
            } else if (requestCode == REQUEST_GALLERY_CAPTURE) {

                Uri selectedImage = data.getData();

                // got uri, now do the painful process of getting the metadata
                // https://developer.android.com/guide/topics/providers/document-provider.html
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);

                if (cursor != null && cursor.getCount() > 0) {

                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

                    if (columnIndex < 0) {
                        Toast.makeText(getActivity(), "Couldn't load image from gallery, try another image.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String picturePath = cursor.getString(columnIndex);
                    mediaFile = new File(picturePath);
                    cursor.close();

                    //create thumbnail
                    thumbnail = getBitmap( picturePath );
                } else {
                    Toast.makeText(getActivity(), "Couldn't load image from gallery, try another image.", Toast.LENGTH_LONG).show();
                    return;
                }
                type = "Image";
                mimeType = "image/jpeg";
                collections = "MobileImagery";

                //something else happened
            } else {
                //do nothing
                return;
            }

            final AlertDialog ingestDialog = createIngestMetacardDialog(dialogView, thumbnail, date);
            ingestDialog.show();
            ingestDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ingestDialog.dismiss();
                }
            });
            ingestDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ingestMetacard(titleView.getText().toString(), thumbnail, date, date, location, contentView.getText().toString(), type, mimeType, collections);
                    ingestDialog.dismiss();
                }
            });
        }
    }

    private Bitmap getBitmap( String path ) {
        Bitmap thumbnail;
        ExifInterface exif = null;
        try{
            exif = new ExifInterface(mediaFile.getAbsolutePath());
        }catch(IOException e ){
            Log.w("FeedTabLatestFragment", e.getMessage(), e);
        }
        if ( exif == null ){
            thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path ), 100, 100);
        }else {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Bitmap tempThumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile( path ), 100, 100);
            Matrix matrix = new Matrix();
            switch ( orientation ) {
                case (ExifInterface.ORIENTATION_ROTATE_90):
                    matrix.postRotate(90);
                    break;
                case (ExifInterface.ORIENTATION_ROTATE_180):
                    matrix.postRotate(180);
                    break;
                case (ExifInterface.ORIENTATION_ROTATE_270):
                    matrix.postRotate(180);
                    break;
                default:
                    break;
            }
            thumbnail = Bitmap.createBitmap(tempThumbnail, 0, 0, tempThumbnail.getWidth(),
                    tempThumbnail.getHeight(), matrix, true);
        }
        return thumbnail;
    }

    private void ingestMetacard(String title, Bitmap thumbnail, Date modified, Date created, Location location, String content, String type, String mimeType, String collections) {
        String data = JSON_TEMPLATE;
        data = AppUtils.replaceEach(data, TITLE_KEY, title);
        data = AppUtils.replaceEach(data,THUMBNAIL_KEY, encodeThumbnail(thumbnail));
        data = AppUtils.replaceEach(data,MODIFIED_KEY, convertDate(modified));
        data = AppUtils.replaceEach(data,CREATED_KEY, convertDate(created));
        data = AppUtils.replaceEach(data, TYPE_KEY, type);
        if (location != null) {
            String geometry = JSON_GEOMETRY_TEMPLATE;
            geometry = AppUtils.replaceEach(geometry,LONGITUDE_KEY, Double.toString(location.getLongitude()));
            geometry = AppUtils.replaceEach(geometry,LATITUDE_KEY, Double.toString(location.getLatitude()));
            data = AppUtils.replaceEach(data,GEOMETRY_KEY, geometry);
        } else {
            data = AppUtils.replaceEach(data, GEOMETRY_KEY, "");
        }
        data = AppUtils.replaceEach(data, COLLECTIONS_KEY, collections);
        boolean isDDMS = getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean( getString(R.string.settings_ddms_key), true);
        if ( isDDMS ){
            String keywords = AppUtils.replaceEach(DDMSConstants.DDMS_KEYWORD_TEMPLATE, DDMSConstants.KEYWORD_KEY, collections);
            keywords = keywords + AppUtils.replaceEach(DDMSConstants.DDMS_KEYWORD_TEMPLATE, DDMSConstants.KEYWORD_KEY, type);
            String ddms = AppUtils.getFileAssetAsString(this.getResources().getAssets(), DDMS_TEMPLATE_ASSET);

            //TODO need to come up with abetter way to not populate the geo
            double lat = 0;
            double lon = 0;
            if ( location != null ){
                lat = location.getLatitude();
                lon = location.getLongitude();
            }
            ddms = AppUtils.replaceEach( ddms,
                    new String[]{ TITLE_KEY, DDMSConstants.CREATOR_KEY, DDMSConstants.DATE_KEY, CONTENT_KEY,
                            DDMSConstants.DEVICE_ID_KEY, DDMSConstants.FORMAT_KEY, DDMSConstants.KEYWORDS_KEY,
                            LATITUDE_KEY, LONGITUDE_KEY},
                    new String[]{ title, UserStore.getInstance().getUserId(), AppUtils.formatDate(new Date()), content,
                            AppUtils.getDeviceId(this.getActivity()), mimeType, keywords,
                            Double.toString(lat), Double.toString(lon)}

            );
            ddms = AppUtils.replaceEach(ddms, "\"", "\\\"" );
            data = AppUtils.replaceEach(data, METADATA_KEY, ddms);
        }else{
            String metadataXml = AppUtils.replaceEach(JSON_SIMPLE_METADATA,new String[]{TITLE_KEY, CONTENT_KEY}, new String[]{title, content});
            data = AppUtils.replaceEach(data, METADATA_KEY, metadataXml );
        }

        // make sure there is a server to send to
        Server ingestServer =  getIngestServer(ServerCollection.getInstance().getServerList());
        if ( ingestServer != null ) {
            Intent intent = new Intent(Intent.ACTION_SYNC, null, ROOT_CONTEXT, MetacardIngestService.class);
            intent.putExtra(MetacardIngestService.MEDIA_PATH, mediaFile.getAbsolutePath());
            intent.putExtra(MetacardIngestService.MEDIA_TYPE, mimeType);
            intent.putExtra(MetacardIngestService.JSON_DATA_KEY, data);
            //TODO get default server instead of first one
            String serverName = ingestServer.getName();
            intent.putExtra(Server.SERVER_ID, serverName);
            handleNotifications(serverName);
            getActivity().startService(intent);
        } else {
            Toast.makeText(getActivity(), "Cannot ingest, no servers with ingest capaility found.", Toast.LENGTH_LONG).show();
        }
    }

    private Server getIngestServer(List<Server> serverList) {
        Server ingestServer = null;
        if ( serverList != null && !serverList.isEmpty() ){
            for( Server server : serverList ){
                if ( server.isIngest() ){
                    ingestServer = server;
                    break;
                }
            }
        }
        return ingestServer;
    }

    private AlertDialog createIngestMetacardDialog(View dialogView, final Bitmap thumbnail, Date date) {
        ImageView thumbnailView = (ImageView) dialogView.findViewById(R.id.ingest_thumbnail);
        TextView dateView = (TextView) dialogView.findViewById(R.id.ingest_date);

        AlertDialog.Builder builder = new AlertDialog.Builder(ROOT_CONTEXT, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle("Create Metacard")
                .setView(dialogView)
                .setPositiveButton("Ingest", null)
                .setNegativeButton("Cancel", null);
        thumbnailView.setImageBitmap(thumbnail);
        dateView.setText(DateFormat.getDateTimeInstance().format(date));


        return builder.create();

    }

    private String encodeThumbnail(Bitmap thumbnail) {
        String encodedThumbnail = "";
        if (thumbnail != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] thumbnailBytes = baos.toByteArray();
            encodedThumbnail = Base64.encodeToString(thumbnailBytes, Base64.DEFAULT);
        }
        return encodedThumbnail;
    }

    private String convertDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        return dateFormat.format(date);
    }

    private void handleNotifications(final String serverName) {
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ROOT_CONTEXT)
                        .setSmallIcon(R.mipmap.condor_icon_white)
                        .setContentTitle("Metacard Ingest")
                        .setContentText("Ingesting new metacard into server " + serverName);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        IntentFilter filter = new IntentFilter(MetacardIngestService.BROADCAST_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        getActivity().registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String id = intent.getStringExtra("ID");
                if (id == null) {
                    builder.setContentText("Error during ingest, check server logs.");
                } else {
                    builder.setContentText("Success: " + serverName );
                }
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                getActivity().unregisterReceiver(this);
            }
        }, filter);


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
            metacardAdapter.notifyDataSetChanged();
            refreshLayout.setRefreshing(false);
        }
    }


}
