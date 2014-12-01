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

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by stevebaker on 11/26/14.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    GridView mGridView;
    ArrayList<GalleryItem> mItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

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
            mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(),
                    android.R.layout.simple_gallery_item, mItems));
        } else {
            mGridView.setAdapter(null);
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
