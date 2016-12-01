package com.cohesiveintegrations.px.android.data.metacard;

import java.io.Serializable;
import java.util.Date;

public class Metacard implements Serializable {

    public static final String METACARD_ID = "metacardId";

    public static final String QUERY_IDENTIFIER = "EMID";

    public enum ActionType {
        IMAGE, VIDEO, OTHER
    }

    public enum SortType {
        DISTANCE, TIME, SOCIAL
    }

    public static final String SORT_TYPE_KEY = "sortType";

    private String id;

    private String title;

    private String content;

    private String site;

    private Date modifiedDate;

    private String latitude;

    private String longitude;

    private String thumbnail;

    private double score;

    private ActionType actionType;

    private Product product;

    public Metacard(String id, String title, String content, String site, Date modifiedDate, String lat, String lon, String thumbnail, double score, ActionType actionType, Product product) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.site = site;
        this.modifiedDate = modifiedDate;
        this.thumbnail = thumbnail;
        this.score = score;
        this.actionType = actionType;
        this.latitude = lat;
        this.longitude = lon;
        this.product = product;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSite() {
        return site;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public double getScore() {
        return score;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public Product getProduct() {
        return product;
    }

   public static class Product implements Serializable {

        public String link;
        public String size;
        public String mimeType;

        public Product (String link, String size, String mimeType) {
            this.link = link;
            this.size = size;
            this.mimeType = mimeType;
        }
    }
}
