package com.couchbase.ratingapp;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.couchbase.lite.Database;

public class P2PRatingFragment extends Fragment {

    EditText targetInput;
    Button button;
    Database database;
    TextView ipTextView;

    public P2PRatingFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_p2p, container, false);

        targetInput = (EditText) rootView.findViewById(R.id.targetInput);
        button = (Button) rootView.findViewById(R.id.button);

        ipTextView = (TextView) rootView.findViewById(R.id.ipTextView);
        ipTextView.setText(((MainActivity) getActivity()).getLocalIpAddress());

        /**
         * Start a replication when the "Push and Pull" button is pressed.
         */
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String targetStringURL = targetInput.getText().toString();
                ((MainActivity) getActivity()).storageManager.oneShotReplication(targetStringURL);
            }
        });

        return rootView;
    }


}
