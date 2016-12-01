package com.cohesiveintegrations.px.android.feed.metacard;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.audit.AbstractAuditor;
import com.cohesiveintegrations.px.android.audit.RetrieveRequestAudit;
import com.cohesiveintegrations.px.android.audit.SearchRequestAudit;
import com.cohesiveintegrations.px.android.data.metacard.Metacard;
import com.cohesiveintegrations.px.android.util.AppUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

public class MetacardHolder extends RecyclerView.ViewHolder {

    private TextView title;
    private TextView description;
    private TextView site;
    private TextView date;
    ImageView thumbnail;
    private ImageButton video;
    private ImageButton image;
    private ImageButton map;
    private Button product;

    public MetacardHolder(View itemView) {
        super(itemView);
        title = (TextView) itemView.findViewById(R.id.feed_title);
        description = (TextView) itemView.findViewById(R.id.feed_description);
        site = (TextView) itemView.findViewById(R.id.feed_site);
        date = (TextView) itemView.findViewById(R.id.feed_date);
        thumbnail = (ImageView) itemView.findViewById(R.id.feed_thumbnail);
        video = (ImageButton) itemView.findViewById(R.id.feed_video);
        image = (ImageButton) itemView.findViewById(R.id.feed_picture);
        map = (ImageButton) itemView.findViewById(R.id.feed_map);
        product = (Button) itemView.findViewById(R.id.feed_product);

    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public void setDescription(String description) {
        this.description.setText(description);
    }

    public void setSite(String site) {
        this.site.setText(site);
    }

    public void setDate(Date date) {
        this.date.setText(DateFormat.getMediumDateFormat(itemView.getContext()).format(date));
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail.setImageBitmap(thumbnail);
    }

    public void setAction(Metacard.ActionType action) {
        switch (action) {
            case VIDEO:
                image.setVisibility(View.GONE);
                video.setVisibility(View.VISIBLE);
                break;
            case IMAGE:
                image.setVisibility(View.VISIBLE);
                video.setVisibility(View.GONE);
                break;
        }
    }

    public void setProduct(Metacard.Product product, final String label, final Context context) {
        if (product != null) {
            final String productLink = product.link;
            String productSize = product.size;
            if (productLink != null && !productLink.isEmpty()) {
                this.product.setVisibility(View.VISIBLE);
                this.product.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri uri = Uri.parse(productLink);
                        DownloadManager.Request request = new DownloadManager.Request(uri);
                        request.setTitle(label);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        downloadManager.enqueue(request);

                        Intent auditIntent = new Intent(context.getApplicationContext(), RetrieveRequestAudit.class);
                        auditIntent.putParcelableArrayListExtra( AbstractAuditor.AUDIT_HEADERS, new ArrayList<Parcelable>());
                        auditIntent.putExtra(AbstractAuditor.SERVICE_URL_PATH, productLink);
                        auditIntent.putExtra(AbstractAuditor.AUDIT_LIST, new String[] {AppUtils.replaceEach(productLink, "&", "&amp;" ) } );
                        context.startService(auditIntent);
                    }
                });
                if (productSize != null && !productSize.isEmpty()) {
                    this.product.setText(readableFileSize(Long.parseLong(productSize)));
                } else {
                    this.product.setText("N/A");
                }
            } else {
                this.product.setVisibility(View.GONE);
            }
        }
    }

    public void setMap(final String lat, final String lon, final String label, final Context context) {
        if (lat != null && lon != null) {
            map.setVisibility(View.VISIBLE);
            map.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.parse("geo:0,0?q=" + lat + "," + lon + "(" + label + ")");
                    intent.setData(uri);
                    context.startActivity(intent);
                }
            });
        } else {
            map.setVisibility(View.GONE);
        }
    }

    /**
     * //TODO find other OSS way of doing this.
     * Source from http://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc
     * Contributed by user Mr Ed on April 8, 2011.
     *
     * @param size
     * @return
     */
    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
