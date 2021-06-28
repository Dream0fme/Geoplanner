package urum.geoplanner.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static urum.geoplanner.utils.Utils.containsIgnoreCase;
import static urum.geoplanner.utils.Utils.getAddressInfo;
import static urum.geoplanner.utils.Utils.startsWithIgnoreCase;


public class GeocoderAdapter extends ArrayAdapter implements Filterable {
    private ArrayList<String> resultList;

    public GeocoderAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public String getItem(int index) {
        return resultList.get(index);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    resultList = getAddressInfo(constraint.toString(), getContext());
                    filterResults.values = resultList;
                    filterResults.count = resultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}
