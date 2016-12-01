package com.cohesiveintegrations.px.android.audit;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class HeaderItem implements Parcelable {
    // parcel keys
    private static final String HEADER_NAME = "name";
    private static final String HEADER_VALUE = "value";

    private String name;
    private String value;

    /**
     * @return the name
     */
    public String getHeaderName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setHeaderName(String name) {
        this.name = name;
    }

    /**
     * @return the age
     */
    public String getHeaderValue() {
        return value;
    }

    /**
     * @param value the age to set
     */
    public void setHeaderValue(String value) {
        this.value = value;
    }

    /**
     * Empty constructor for array creation
     */
    public HeaderItem() {
    }

    /**
     * @param name
     * @param value
     */
    public HeaderItem(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // create a bundle for the key value pairs
        Bundle bundle = new Bundle();

        // insert the key value pairs to the bundle
        bundle.putString(HEADER_NAME, name);
        bundle.putString(HEADER_VALUE, value);

        // write the key value pairs to the parcel
        dest.writeBundle(bundle);
    }

    /**
     * Creator required for class implementing the parcelable interface.
     */
    public static final Parcelable.Creator<HeaderItem> CREATOR = new Creator<HeaderItem>() {

        @Override
        public HeaderItem createFromParcel(Parcel source) {
            // read the bundle containing key value pairs from the parcel
            Bundle bundle = source.readBundle();

            // instantiate a person using values from the bundle
            return new HeaderItem(bundle.getString(HEADER_NAME), bundle.getString(HEADER_VALUE));
        }

        @Override
        public HeaderItem[] newArray(int size) {
            return new HeaderItem[size];
        }

    };
}
