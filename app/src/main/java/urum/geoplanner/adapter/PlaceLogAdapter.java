package urum.geoplanner.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.databinding.DataBindingUtil;

import java.util.List;

import urum.geoplanner.R;
import urum.geoplanner.databinding.ActivityMainBinding;
import urum.geoplanner.databinding.PlaceListBinding;
import urum.geoplanner.db.entities.PlaceLog;

public class PlaceLogAdapter extends ArrayAdapter<PlaceLog> {
    PlaceListBinding binding;

    public PlaceLogAdapter(Context context, List<PlaceLog> users) {
        super(context, 0, users);
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PlaceLog placeLog = getItem(position);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        binding = PlaceListBinding.inflate(inflater,null,false);
        binding.setPlace(placeLog);

        return binding.getRoot();
    }
}
