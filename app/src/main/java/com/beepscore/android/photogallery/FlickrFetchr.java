package com.beepscore.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by stevebaker on 11/28/14.
 */
public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";

    public static final String PREF_SEARCH_QUERY = "PREF_SEARCH_QUERY";

    // flickr requires https not http
    // http://forums.bignerdranch.com/viewtopic.php?f=423&t=8944
    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";

    // get temporary key from
    // http://www.flickr.com/services/api/explore/?method=flickr.photos.search
    // on that page, do a search to get url at bottom of page. Then copy &apiKey up to &
    // Alternatively, register with Flickr for a developer key
    // http://www.flickr.com/services/api/keys/apply/
    // redirects to Yahoo login
    private static final String API_KEY = "2d15ecf3259aff792342c76f402dc28f";

    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String XML_PHOTOS = "photos";
    private static final String XML_PHOTOS_TOTAL = "total";
    private static final String XML_PHOTO = "photo";

    private static final String PARAM_EXTRAS = "extras";
    private static final String PARAM_TEXT = "text";
    private static final String EXTRA_SMALL_URL = "url_s";

    Photos mPhotos;

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        // because url is http, we can cast URLConnection to HttpURLConnection
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            int bytesRead = 0;

            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public Photos fetchItems() {
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_GET_RECENT)
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                .build().toString();
        return downloadGalleryItems(url);
    }

    public Photos search(String query) {
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_SEARCH)
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                .appendQueryParameter(PARAM_TEXT, query)
                .build().toString();
        return downloadGalleryItems(url);
    }

    public Photos downloadGalleryItems(String url) {
        Photos photos = new Photos();
        ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
        try {
            String xmlString = getUrl(url);
            Log.i(TAG, "Received xml: " + xmlString);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlString));

            photos = parseItems(items, parser);
        } catch (IOException ioException) {
            Log.e(TAG, "Failed to fetch items", ioException);
        } catch (XmlPullParserException xmlPullParserException) {
            Log.e(TAG, "Failed to parse items", xmlPullParserException);
        }
        return photos;
    }

    public Photos parseItems(ArrayList<GalleryItem> items, XmlPullParser parser)
            throws XmlPullParserException, IOException {

        Photos photos = new Photos();

        int eventType = parser.next();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG &&
                    XML_PHOTOS.equals(parser.getName())) {
                String photosCountString = parser.getAttributeValue(null, XML_PHOTOS_TOTAL);
                photos.setCount(Integer.parseInt(photosCountString));
            }
            if (eventType == XmlPullParser.START_TAG &&
                    XML_PHOTO.equals(parser.getName())) {
                String id = parser.getAttributeValue(null, "id");
                String caption = parser.getAttributeValue(null, "title");
                String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);

                GalleryItem item = new GalleryItem();
                item.setId(id);
                item.setCaption(caption);
                item.setUrl(smallUrl);
                items.add(item);
            }
            eventType = parser.next();
        }
        photos.setItems(items);
        return photos;
    }

}
