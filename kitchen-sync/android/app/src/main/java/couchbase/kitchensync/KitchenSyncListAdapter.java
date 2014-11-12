/**
 * Created by zgramana on 11/10/14.
 */
package couchbase.kitchensync;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;

import java.util.List;

public class KitchenSyncListAdapter extends ArrayAdapter<QueryRow> {

    private List<QueryRow> list;
    private final Context context;

    public KitchenSyncListAdapter(Context context, int resource, int textViewResourceId, List<QueryRow> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
    }

    private static class ViewHolder {
        ImageView icon;
        TextView label;
    }

    @Override
    public View getView(int position, View itemView, ViewGroup parent) {

        if (itemView == null) {
            LayoutInflater vi = (LayoutInflater)parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = vi.inflate(R.layout.list_item, null);
            ViewHolder vh = new ViewHolder();
            vh.label = (TextView) itemView.findViewById(R.id.label);
            itemView.setTag(vh);
        }

        try {
            TextView label = ((ViewHolder)itemView.getTag()).label;
            QueryRow row;
            try {
                row = getItem(position);
            } catch (ClassCastException e) {
                // We haven't fully setup the Couchbase Lite code yet.
                return itemView;
            }
            SavedRevision currentRevision = row.getDocument().getCurrentRevision();
            Object check = currentRevision.getProperty("check");
            boolean isItemChecked = false;
            if (check != null && check instanceof Boolean) {
                isItemChecked = ((Boolean)check).booleanValue();
            }
            String itemText = (String) currentRevision.getProperty("text");
            label.setText(itemText);
            label.setTextColor(Color.BLACK);
            CheckBox checked = (CheckBox)itemView.findViewById(R.id.item_checked);
            checked.setChecked(isItemChecked);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return itemView;
    }
}
