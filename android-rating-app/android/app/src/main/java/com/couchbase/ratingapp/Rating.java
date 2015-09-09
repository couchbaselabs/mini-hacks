package com.couchbase.ratingapp;

import com.couchbase.lite.support.LazyJsonObject;

/**
 * Created by jamesnocentini on 03/09/15.
 */
public class Rating {
    private LazyJsonObject mLazy;

    public Rating(LazyJsonObject lazyJsonObject) {
        mLazy = lazyJsonObject;
    }

    public String getName() {
        return (String) mLazy.get("name");
    }

    public String getId() {
        return (String) mLazy.get("_id");
    }

    public float getRating() {
        return (float) mLazy.get("rating");
    }
}
