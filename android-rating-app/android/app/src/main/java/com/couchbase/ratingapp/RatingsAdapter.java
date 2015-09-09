package com.couchbase.ratingapp;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;

/**
 * Created by jamesnocentini on 03/09/15.
 */
public class RatingsAdapter extends RecyclerView.Adapter<RatingsAdapter.ViewHolder> {

    Context context;
    LiveQuery query;
    private QueryEnumerator enumerator;


    public RatingsAdapter(Context context, LiveQuery query) {
        this.context = context;
        this.query = query;
        query.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent changeEvent) {
                ((Activity) RatingsAdapter.this.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enumerator = changeEvent.getRows();
                        notifyDataSetChanged();
                    }
                });
            }
        });
        query.start();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_rating, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final QueryRow row = (QueryRow) getItem(position);

        holder.ratingValue.setText(String.valueOf(row.getKey()));
        if (row.getValue() == null) {
            try {
                int conflicts = row.getDocument().getConflictingRevisions().size();
                holder.totalRatings.setText(String.valueOf(conflicts));
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
