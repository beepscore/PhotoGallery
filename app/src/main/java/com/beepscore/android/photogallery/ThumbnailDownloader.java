package com.beepscore.android.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by stevebaker on 12/4/14.
 */
public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    Handler mHandler;

    // requestMap is a synchronized HashMap.
    // key is Token, value is String url
    Map<Token, String> requestMap =
            Collections.synchronizedMap(new HashMap<Token, String>());

    public ThumbnailDownloader() {
        super(TAG);
    }

    // Java would warn about subclassing Handler.
    // Might leak memory because mHandler is an anonymous inner class.
    // HandlerThread manages Handler and prevents leak, so suppress lint
    @SuppressLint("HandlerLeak")
    // onLooperPrepared is called before looper checks the queue for the first time
    @Override
    protected void onLooperPrepared () {

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == MESSAGE_DOWNLOAD) {
                    // suppress warning because can't make cast due to "type erasure"
                    @SuppressWarnings("unchecked")
                    Token token = (Token)message.obj;
                    Log.i(TAG, "Got a request for url: " + requestMap.get(token));
                    handleRequest(token);
                }
            }
        };
    }

    public void queueThumbnail(Token token, String url) {
        Log.i(TAG, "Got a URL: " + url);
        // add key-value pair
        requestMap.put(token, url);

        // get message, give it to token as its obj, put on message queue
        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token)
                .sendToTarget();
    }

    private void handleRequest(final Token token) {
        try {
            final String url = requestMap.get(token);
            if (url == null) {
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory
                    .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");
        } catch (IOException ioException) {
            Log.e(TAG, "Error downloading image", ioException);
        }
    }
}
