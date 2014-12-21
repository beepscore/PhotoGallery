package com.beepscore.android.photogallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by stevebaker on 11/26/14.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    GridView mGridView;
    Photos mPhotos;
    ThumbnailDownloader<ImageView> mThumbnailThread;
    FlickrFetchr mFlickrFetchr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if (isVisible()) {
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });

        // call start to get thread ready before calling getLooper
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread started");
    }

    public void updateItems() {
        // start async task
        new FetchItemsTask().execute();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    @TargetApi(11)
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_photo_gallery, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Pull out the SearchView
            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView)searchItem.getActionView();

            // Get the data from our searchable.xml as a SearchableInfo
            SearchManager searchManager = (SearchManager)getActivity()
                    .getSystemService(Context.SEARCH_SERVICE);
            ComponentName name = getActivity().getComponentName();
            SearchableInfo searchableInfo = searchManager.getSearchableInfo(name);

            searchView.setSearchableInfo(searchableInfo);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_search:

                //getActivity().onSearchRequested();

                // Ch 28 Challenge 1 change from onSearchRequested to startSearch
                // Note Android recommends use onSearchRequested
                // https://developer.android.com/reference/android/app/Activity.html
                // Book author says Challenge 1 isn't relevant when using Honeycomb SearchView
                // http://forums.bignerdranch.com/viewtopic.php?f=425&t=6994
                String query =  PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
                getActivity().startSearch(query, true, null, false);

                return true;
            case R.id.menu_item_clear:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

        if (mPhotos == null) {
            mPhotos = new Photos();
        }
        if (mPhotos.getItems() != null) {
            mGridView.setAdapter(new GalleryItemAdapter(mPhotos.getItems()));
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

            // book returns convertView, but I expected it would return imageView.
            // either one works. Is this because convertView contains only imageView?
            // return convertView;
            return imageView;
        }
    }

    /**
     * third parameter is AsyncTask result type
     * matches doInBackground return type and onPostExecute input type
     */
    private class FetchItemsTask
            extends AsyncTask<Void,Void,Photos> {

        /**
         * Android calls on background thread, so can't update UI safely
         * Android won't let app update UI from a background thread
         */
        @Override
        protected Photos doInBackground(Void... params) {

            Activity activity = getActivity();
            if (activity == null) {
                return new Photos();
            }

            String query =  PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);

            if (mFlickrFetchr == null) {
                mFlickrFetchr = new FlickrFetchr();
            }

            if (query != null) {
                return mFlickrFetchr.search(query);
            } else {
                return mFlickrFetchr.fetchItems();
            }
        }

        /**
         * Android calls back on main thread (UI thread), so can update UI safely
         */
        @Override
        protected void onPostExecute(Photos photos) {
            mPhotos = photos;
            setupAdapter();
            Toast.makeText(getActivity(),
                    "Result count " + String.valueOf(mPhotos.getCount()),
                    Toast.LENGTH_SHORT).show();
        }
    }

}
