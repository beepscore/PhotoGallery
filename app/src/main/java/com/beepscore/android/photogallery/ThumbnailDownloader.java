package com.beepscore.android.photogallery;

import android.os.HandlerThread;
import android.util.Log;

/**
 * Created by stevebaker on 12/4/14.
 */
public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";

    public ThumbnailDownloader() {
        super(TAG);
    }

    public void queueThumbnail(Token token, String url) {
        Log.i(TAG, "Got a URL: " + url);
    }

}
