package com.couchbase.ratingapp;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;

public class RatingsAdapter extends RecyclerView.Adapter<RatingsAdapter.ViewHolder> {

    Context context;
    LiveQuery query;
    private QueryEnumerator enumerator;

    public RatingsAdapter(final LiveQuery query, Context context) {
        this.context = context;
        this.query = query;

        this.query.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(LiveQuery.ChangeEvent event) {
                ((Activity) RatingsAdapter.this.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enumerator = query.getRows();
                        notifyDataSetChanged();
                    }
                });
            }
        });
        query.start();
    }

    /**
     * Initialize parameters and starts listening on the Live Query for updates from the database.
     * @param context Android context in which the application is running
     * @param query LiveQuery to use in the adapter to populate the Recycler View
     */
    public RatingsAdapter(Context context, final LiveQuery query, Database database) {
        this.context = context;
        this.query = query;

        /** Use the database change listener instead of live query listener because we want
         * to listen for conflicts as well
         */
        database.addChangeListener(new Database.ChangeListener() {
            @Override
            public void changed(final Database.ChangeEvent event) {
                ((Activity) RatingsAdapter.this.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enumerator = query.getRows();
                        notifyDataSetChanged();
                    }
                });
            }
        });
        query.start();
        ((Activity) RatingsAdapter.this.context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    enumerator = query.run();
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Inflate a new instance of the ViewHolder. The Recycler View handles reuses already
     * instantiated ViewHolders when possible.
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_rating, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Use the position to get the corresponding query row and populate the ViewHolder that
     * was created above.
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final QueryRow row = (QueryRow) getItem(position);

        /** TODO: This is a hack to populate the result of the ratings or conflicts view query,
         ** have two different recycler view adapters instead. */
        holder.ratingValue.setText(String.valueOf(row.getKey()));
        if (row.getValue() == null) {
            try {
                int conflicts = row.getDocument().getConflictingRevisions().size();
                holder.totalRatings.setText(String.valueOf(conflicts - 1));
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }

        } else {
            holder.totalRatings.setText(String.valueOf(row.getValue()));
        }
    }

    @Override
    public int getItemCount() {
        return enumerator != null ? enumerator.getCount() : 0;
    }
    public Object getItem(int i) {
        return enumerator != null ? enumerator.getRow(i) : null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView ratingValue;
        public TextView totalRatings;

        public ViewHolder(View itemView) {
            super(itemView);
            ratingValue = (TextView) itemView.findViewById(R.id.ratingValue);
            totalRatings = (TextView) itemView.findViewById(R.id.totalRatings);
        }
    }

}
