package com.beepscore.android.photogallery;

import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by stevebaker on 11/28/14.
 */
public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";

    // flickr requires https not http
    // http://forums.bignerdranch.com/viewtopic.php?f=423&t=8944
    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";

    // get temporary key from
    // http://www.flickr.com/services/api/explore/?method=flickr.photos.search
    // on that page, do a search to get url at bottom of page. Then copy &apiKey up to &
    // Alternatively, register with Flickr for a developer key
    // http://www.flickr.com/services/api/keys/apply/
    // redirects to Yahoo login
    private static final String API_KEY = "564285c7b559cf24d0715d8c29c9b3af";

    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String PARAM_EXTRAS = "extras";
    private static final String EXTRA_SMALL_URL = "url_s";

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

    public void fetchItems() {
        try {
            String url = Uri.parse(ENDPOINT).buildUpon()
                    .appendQueryParameter("method", METHOD_GET_RECENT)
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                    .build().toString();
            String xmlString = getUrl(url);
            Log.i(TAG, "Received xml: " + xmlString);
        } catch (IOException ioException) {
            Log.e(TAG, "Failed to fetch items", ioException);
        }
    }

}
