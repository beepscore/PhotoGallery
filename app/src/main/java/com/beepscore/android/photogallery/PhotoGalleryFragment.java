package com.beepscore.android.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by stevebaker on 11/26/14.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    ThumbnailDownloader<ImageView> mThumbnailThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // start async task
        new FetchItemsTask().execute();

        mThumbnailThread = new ThumbnailDownloader<ImageView>();
        // call start to get thread ready before calling getLooper
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mGridView = (GridView)view.findViewById(R.id.gridView);
        setupAdapter();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // explicitly quit thread to avoid memory leak
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    /**
     * Call in onCreateView to handle rotation
     * Call when model changes to update view
     */
    void setupAdapter() {
        // check if fragment is attached to an Activity
        // when using AsyncTask fragment may not be attached to an Activity
        if (getActivity() == null ||
                mGridView == null) {
            return;
        }

        if (mItems != null) {
            mGridView.setAdapter(new GalleryItemAdapter(mItems));
        } else {
            mGridView.setAdapter(null);
        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

        public GalleryItemAdapter(ArrayList<GalleryItem> items) {
            super(getActivity(), 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.gallery_item, parent, false);
            }

            ImageView imageView = (ImageView)convertView
                    .findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(R.drawable.brian_up_close);
            GalleryItem item = getItem(position);
            mThumbnailThread.queueThumbnail(imageView, item.getUrl());

            return imageView;
        }
    }

    /**
     * third parameter is AsyncTask result type
     * matches doInBackground return type and onPostExecute input type
     */
    private class FetchItemsTask
            extends AsyncTask<Void,Void,ArrayList<GalleryItem>> {

        /**
         * executes on background thread, so can't update UI safely
         * Android won't let app update UI from a background thread
         */
        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems();
        }

        /**
         * callback on main thread (UI thread), so can update UI safely
         */
        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            mItems = items;
            setupAdapter();
        }
    }

}
