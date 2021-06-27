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
                    resultList = getAddressInfo(constraint.toString());
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

    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }


    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private ArrayList<String> getAddressInfo(String locationName) {
        ArrayList<String> list = new ArrayList<>();
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> a = geocoder.getFromLocationName(locationName.trim(), 1);
            for (int i = 0; i < a.size(); i++) {
                String city = a.get(i).getLocality();
                String street = a.get(i).getThoroughfare();
                String future = a.get(i).getFeatureName();
                if (city != null) {
                    list.add(city);
                }
                if (city != null & street != null) {
                    list.clear();
                    String[] subStr;
                    subStr = locationName.split("\\s|,");
                    for (int j = 0; j < subStr.length; j++) {
                        if (startsWithIgnoreCase(street, subStr[j]) || containsIgnoreCase(future, subStr[j]) || containsIgnoreCase(street, subStr[j])) {
                            list.add(city + "," + " " + street);
                        } else list.clear();
                    }
                }

                if (city != null & future != null) {
                    list.clear();
                    String[] subStr;
                    subStr = locationName.split("\\s|,");
                    for (int j = 0; j < subStr.length; j++) {
                        if (startsWithIgnoreCase(future, subStr[j]) || containsIgnoreCase(future, subStr[j])) {
                            if (!city.equalsIgnoreCase(future)) {
                                list.clear();
                                list.add(city + ", " + future);
                            } else if (subStr.length < 2) {
                                list.clear();
                                list.add(city);
                            }
                        } else list.clear();
                    }
                }
                if (city != null & street != null & future != null) {
                    list.clear();
                    String[] subStr;
                    subStr = locationName.split("\\s|,");
                    for (int j = 0; j < subStr.length; j++) {
                        if (startsWithIgnoreCase(street, subStr[j])) {
                            if (!street.equalsIgnoreCase(future)) {
                                list.clear();
                                list.add(city + "," + " " + street + ", " + future);
                            } else {
                                list.clear();
                                list.add(city + "," + " " + street);
                            }
                        } else list.clear();
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}
