package urum.geoplanner.utils;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.os.ConfigurationCompat;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import urum.geoplanner.R;

import static urum.geoplanner.utils.Utils.getAddressInfo;


public class GeocoderAdapter extends ArrayAdapter implements Filterable {
    private List<String> resultList;
    private MapboxGeocoding client;
    private Locale currentLocale;

    public GeocoderAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        currentLocale = ConfigurationCompat.getLocales(context.getResources().getConfiguration()).get(0);

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


 /*                   try {
                        client = MapboxGeocoding.builder()
                                .accessToken(getContext().getString(R.string.mapbox_token))
                                .query(constraint.toString())
                                .country(currentLocale)
                                .build();

                        //Response<GeocodingResponse> response = client.executeCall();
                        client.enqueueCall(new Callback<GeocodingResponse>() {
                            @Override
                            public void onResponse(Call<GeocodingResponse> call,
                                                   Response<GeocodingResponse> response) {
                                if (response.body() != null) {
                                    List<CarmenFeature> results = response.body().features();
                                    if (results.size() > 0) {
                                        CarmenFeature feature = results.get(0);
                                        String carmenFeatureAddress = feature.toJson();
                                        if(carmenFeatureAddress != null){
                                            Log.d("mytag",carmenFeatureAddress);
                                        }
                                    } else {
                                        Toast.makeText(getContext(), "no results in geocoding request",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                                Log.e("mytag","Geocoding Failure: " + throwable.getMessage());
                            }
                        });
                    } catch (ServicesException servicesException) {
                        Log.e("mytag","Error geocoding: " + servicesException.toString());
                        servicesException.printStackTrace();
                    }*/
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
