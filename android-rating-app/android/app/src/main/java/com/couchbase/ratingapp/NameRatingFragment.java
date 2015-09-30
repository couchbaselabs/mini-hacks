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

public class NameRatingFragment extends Fragment {

    EditText nameInput;
    RatingBar ratingBar;
    Button button;
    Database database;

    public NameRatingFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_name, container, false);

        nameInput = (EditText) rootView.findViewById(R.id.nameInput);
        ratingBar = (RatingBar) rootView.findViewById(R.id.ratingBar);
        button = (Button) rootView.findViewById(R.id.button);

        database = ((MainActivity) getActivity()).storageManager.database;

        /** Read or update the document */
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = nameInput.getText().toString();

                Map<String, Object> formProperties = new HashMap<String, Object>();
                formProperties.put("rating", ratingBar.getRating());


                /** Check if the document already exists in the database */
                Document document = database.getExistingDocument(name);

                /** If the document already exists, update it with the properties of the previous
                 *  revision and the new ones. If the document doesn't exist, create one. */
                if (document != null) {
                    Map<String, Object> updateProperties = new HashMap<String, Object>();
                    updateProperties.putAll(document.getProperties());
                    updateProperties.putAll(formProperties);

                    try {
                        document.putProperties(updateProperties);
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    }

                } else {
                    Map<String, Object> createProperties = new HashMap<String, Object>();
                    createProperties.put("type", "conflict");
                    createProperties.putAll(formProperties);

                    Document newDocument = database.getDocument(name);
                    try {
                        newDocument.putProperties(createProperties);
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        return rootView;
    }
}
