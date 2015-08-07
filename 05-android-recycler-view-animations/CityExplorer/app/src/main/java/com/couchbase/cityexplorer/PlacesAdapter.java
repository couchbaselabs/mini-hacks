package com.couchbase.cityexplorer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.couchbase.cityexplorer.model.Place;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;

import java.io.InputStream;
import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.ViewHolder> {

    Context context;
    List<Place> dataSet;
    Database database;

    public PlacesAdapter(Context context, List<Place> dataSet, Database database) {
        this.context = context;
        this.dataSet = dataSet;
        this.database = database;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_places, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Place place = dataSet.get(position);

        holder.restaurantName.setText(place.getName());
        holder.restaurantText.setText(place.getAddress());

        Document document = database.getDocument(place.getId());
        Attachment attachment = document.getCurrentRevision().getAttachment("photo");
        if (attachment != null) {
            InputStream is = null;
            try {
                is = attachment.getContent();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            Drawable drawable = Drawable.createFromStream(is, "photo");
            holder.restaurantImage.setImageDrawable(drawable);
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView restaurantName;
        public TextView restaurantText;
        public ImageView restaurantImage;

        public ViewHolder(View itemView) {
            super(itemView);
            restaurantName = (TextView) itemView.findViewById(R.id.restaurantName);
            restaurantText = (TextView) itemView.findViewById(R.id.restaurantText);
            restaurantImage = (ImageView) itemView.findViewById(R.id.restaurantImage);
        }
    }

}
