package com.couchbase.ratingapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;

import java.util.HashMap;
import java.util.Map;

public class UniqueRatingFragment extends Fragment {

    EditText nameInput;
    RatingBar ratingBar;
    Button button;
    Database database;

    public UniqueRatingFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_unique, container, false);

        nameInput = (EditText) rootView.findViewById(R.id.nameInput);
        ratingBar = (RatingBar) rootView.findViewById(R.id.ratingBar);
        button = (Button) rootView.findViewById(R.id.button);

        database = ((MainActivity) getActivity()).storageManager.database;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Document document = database.createDocument();

                Map<String, Object> properties = new HashMap<String, Object>();

                properties.put("name", nameInput.getText().toString());
                properties.put("rating", String.valueOf(ratingBar.getRating()));
                properties.put("type", "unique");

                try {
                    document.putProperties(properties);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }
        });

        return rootView;
    }
}
