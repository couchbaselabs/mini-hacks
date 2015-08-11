package com.couchbase.cityexplorer.model;

import com.couchbase.lite.support.LazyJsonObject;

public class Place {
    private LazyJsonObject mLazy;

    public Place(LazyJsonObject lazyJsonObject) {
        mLazy = lazyJsonObject;
    }

    public String getName() {
        return (String) mLazy.get("name");
    }

    public String getId() {
        return (String) mLazy.get("_id");
    }

    public String getAddress() {
        return (String) mLazy.get("formatted_address");
    }
}
