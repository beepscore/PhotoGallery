package com.beepscore.android.photogallery;

import java.util.ArrayList;

/**
 * Created by stevebaker on 12/20/14.
 * MVC Model object
 * Reference
 * http://forums.bignerdranch.com/viewtopic.php?f=425&t=9473
 */
public class Photos {
    private long mCount = 0;
    private ArrayList<GalleryItem> mItems;

    public Photos(){
        mCount=0;
        mItems=new ArrayList<GalleryItem>();
    }

    public long getCount() {
        return mCount;
    }

    public void setCount(long count) {
        mCount = count;
    }

    public ArrayList<GalleryItem> getItems() {
        return mItems;
    }

    public void setItems(ArrayList<GalleryItem> items) {
        mItems = items;
    }

}
