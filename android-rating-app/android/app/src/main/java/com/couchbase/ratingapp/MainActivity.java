package com.couchbase.ratingapp;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.couchbase.lite.LiveQuery;

import org.w3c.dom.Text;

public class MainActivity extends FragmentActivity {

    public StorageManager storageManager;
    RecyclerView recyclerView;
    RatingsAdapter adapter;
    ViewPager viewPager;

    TextView keyTextView;
    TextView valueTextView;

    Switch syncSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** Get the ViewPager and set it's PagerAdapter so that it can display items */
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        final RatingFragmentPagerAdapter adapter = new RatingFragmentPagerAdapter(getSupportFragmentManager(),
                MainActivity.this);
        viewPager.setAdapter(adapter);

        /** Connect the view pager to the tab layout to automatically synchronize the tab
         * state and current page of the view pager that is being displayed */
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d("RatingApp", String.valueOf(tab.getPosition()));
                viewPager.setCurrentItem(tab.getPosition(), true);
                switch (tab.getPosition()) {
                    case 0:
                        setupUniqueQuery();
                        return;
                    case 1:
                        setupUserQuery();
                        return;
                    default:
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition(), true);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition(), true);
            }
        });

        /** When the switch changes value, update the replications */
        syncSwitch = (Switch) findViewById(R.id.switch1);
        syncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    storageManager.startSyncGatewayReplications();
                } else {
                    storageManager.stopSyncGatewayReplications();
                }
            }
        });

        /** Couchbase Lite database bootstrap */
        storageManager = new StorageManager(getApplicationContext());

        recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        keyTextView = (TextView) findViewById(R.id.keyColumn);
        valueTextView = (TextView) findViewById(R.id.valueColumn);

        /** Query the views that were registered when the StorageManager was instantiated */
        setupUniqueQuery();
    }

    void setupUniqueQuery() {
        /** Update the labels of the Recycler View */
        keyTextView.setText("Rating");
        valueTextView.setText("Total");

        LiveQuery liveQuery = storageManager.database.getView(StorageManager.UNIQUE_RATINGS_VIEW).createQuery().toLiveQuery();
        liveQuery.setGroupLevel(1); // use group level to aggregate by key (i.e. the rating value)
        liveQuery.setDescending(true);
        adapter = new RatingsAdapter(liveQuery, this);
        recyclerView.setAdapter(adapter);
    }

    void setupUserQuery() {
        /** Update the labels of the Recycler View */
        keyTextView.setText("Name (id)");
        valueTextView.setText("Conflicts");

        LiveQuery liveQuery = storageManager.database.getView(StorageManager.USER_RATINGS_VIEW).createQuery().toLiveQuery();
        adapter = new RatingsAdapter(this, liveQuery, storageManager.database);
        recyclerView.setAdapter(adapter);
    }

    /** Get local IP to display in a TextView on the P2P tab */
    public String getLocalIpAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

}
