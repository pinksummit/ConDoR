/*
 * Copyright (C) 2014 Cohesive Integrations, LLC (info@cohesiveintegrations.com)
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cohesiveintegrations.px.android.feed;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This class parses generic Atom feeds.
 * <p/>
 * <p>Given an InputStream representation of a feed, it returns a List of entries,
 * where each list element represents a single entry (post) in the XML feed.
 * <p/>
 * <p>An example of an Atom feed can be found at:
 * http://en.wikipedia.org/w/index.php?title=Atom_(standard)&oldid=560239173#Example_of_an_Atom_1.0_feed
 */
public class FeedParser {

    private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";

    /**
     * Parse an Atom feed, returning a collection of Entry objects.
     *
     * @param in Atom feed, as a stream.
     * @return List of {@link FeedParser.Entry} objects.
     * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
     * @throws java.io.IOException                   on I/O error.
     */
    public List<Entry> parse(InputStream in)
            throws XmlPullParserException, IOException, ParseException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    /**
     * Decode a feed attached to an XmlPullParser.
     *
     * @param parser Incoming XMl
     * @return List of {@link FeedParser.Entry} objects.
     * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
     * @throws java.io.IOException                   on I/O error.
     */
    private List<Entry> readFeed(XmlPullParser parser)
            throws XmlPullParserException, IOException, ParseException {
        List<Entry> entries = new ArrayList<Entry>();

        // Search for <feed> tags. These wrap the beginning/end of an Atom document.
        //
        // Example:
        // <?xml version="1.0" encoding="utf-8"?>
        // <feed xmlns="http://www.w3.org/2005/Atom">
        // ...
        // </feed>
        parser.require(XmlPullParser.START_TAG, ATOM_NAMESPACE, "feed");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the <entry> tag. This tag repeats inside of <feed> for each
            // article in the feed.
            //
            // Example:
            // <entry>
            //   <title>Article title</title>
            //   <product rel="alternate" type="text/html" href="http://example.com/article/1234"/>
            //   <product rel="edit" href="http://example.com/admin/article/1234"/>
            //   <id>urn:uuid:218AC159-7F68-4CC6-873F-22AE6017390D</id>
            //   <published>2003-06-27T12:00:00Z</published>
            //   <updated>2003-06-28T12:00:00Z</updated>
            //   <summary>Article summary goes here.</summary>
            //   <author>
            //     <name>Rick Deckard</name>
            //     <email>deckard@example.com</email>
            //   </author>
            // </entry>
            if (name.equals("entry")) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    /**
     * Parses the contents of an entry. If it encounters a title, summary, or product tag, hands them
     * off to their respective "read" methods for processing. Otherwise, skips the tag.
     */
    private Entry readEntry(XmlPullParser parser)
            throws XmlPullParserException, IOException, ParseException {
        parser.require(XmlPullParser.START_TAG, ATOM_NAMESPACE, "entry");
        String id = null;
        String title = null;
        String productLink = null;
        String productSize = null;
        String site = null;
        double score = 0;
        long publishedOn = 0;
        String lat = null;
        String lon = null;
        String type = null;
        String thumbnailLink = null;

        boolean isPoint = false;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "id":
                    // Example: <id>urn:uuid:218AC159-7F68-4CC6-873F-22AE6017390D</id>
                    id = readBasicTag(parser, name);
                    break;
                case "title":
                    // Example: <title>Article title</title>
                    title = readBasicTag(parser, name);
                    break;
                case "link":
                    // Example: <product rel="alternate" type="text/html" href="http://example.com/article/1234"
                    // length="1728195"/>

                    String linkType = parser.getAttributeValue(null, "rel");

                    if (linkType != null) {
                        switch (linkType) {
                            // product
                            case "alternate":
                                productLink = parser.getAttributeValue(null, "href");
                                productSize = parser.getAttributeValue(null, "length");
                                break;
                            // thumbnail
                            case "preview":
                                thumbnailLink = parser.getAttributeValue(null, "href");
                                break;
                        }
                    }

                    while (true) {
                        if (parser.nextTag() == XmlPullParser.END_TAG) {
                            break;
                        }
                    }

                    break;
                case "updated":
                    // Example: <published>2003-06-27T12:00:00Z</published>
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    String date = readBasicTag(parser, name);
                    publishedOn = format.parse(date).getTime();
                    break;
                case "resultSource":
                    site = readBasicTag(parser, name);
                    break;
                case "score":
                    score = Double.parseDouble(readBasicTag(parser, name));
                    break;
                case "category":
                    String label = parser.getAttributeValue(XmlPullParser.NO_NAMESPACE, "label");
                    if ("Content Type".equals(label)) {
                        type = parser.getAttributeValue(XmlPullParser.NO_NAMESPACE, "term");
                    }
                    while (true) {
                        if (parser.nextTag() == XmlPullParser.END_TAG) {
                            break;
                        }
                    }
                    break;
                case "where":
                    //TODO there is probably a better way to pull out the coords
                    break;
                case "Point":
                    isPoint = true;
                    break;
                case "pos":
                    if (isPoint) {
                        String latLon = readBasicTag(parser, name);
                        String[] locAry = latLon.split(" ");
                        lat = locAry[0];
                        lon = locAry[1];
                        while (parser.next() == XmlPullParser.END_TAG) {
                            if ("where".equals(parser.getName())) {
                                // break out at the end tag for the where element
                                break;
                            }
                        }
                    } else {
                        skip(parser);
                    }
                    break;
                default:
                    skip(parser);
            }
        }
        return new Entry(id, title, productLink, productSize, site, score, publishedOn, lat, lon, type, thumbnailLink);
    }

    /**
     * Reads the body of a basic XML tag, which is guaranteed not to contain any nested elements.
     * <p/>
     * <p>You probably want to call readTag().
     *
     * @param parser Current parser object
     * @param tag    XML element tag name to parse
     * @return Body of the specified tag
     * @throws java.io.IOException
     * @throws org.xmlpull.v1.XmlPullParserException
     */
    private String readBasicTag(XmlPullParser parser, String tag)
            throws IOException, XmlPullParserException {
        //parser.require(XmlPullParser.START_TAG, ns, tag);
        String result = readText(parser);
        //parser.require(XmlPullParser.END_TAG, ns, tag);
        return result;
    }

    /**
     * For the tags title and summary, extracts their text values.
     */
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = null;
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /**
     * Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
     * if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
     * finds the matching END_TAG (as indicated by the value of "depth" being 0).
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    /**
     * This class represents a single entry (post) in the XML feed.
     * <p/>
     * <p>It includes the data members "title," "product," and "summary."
     */
    public static class Entry {
        public final String id;
        public final String title;
        public final String product;
        public final String productSize;
        public final String site;
        public final double score;
        public final long published;
        public final String lat;
        public final String lon;
        public final String type;
        public final String thumbnail;

        Entry(String id, String title, String link, String productSize, String site, double score, long published, String lat, String lon, String type, String thumbnail) {
            this.id = id;
            this.title = title;
            this.product = link;
            this.productSize = productSize;
            this.site = site;
            this.score = score;
            this.published = published;
            this.type = type;
            this.thumbnail = thumbnail;
            this.lat = lat;
            this.lon = lon;
        }
    }
}
