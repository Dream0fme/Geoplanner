package urum.geoplanner.adapter;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import urum.geoplanner.db.entities.Place;

public class PlacesDiffCallback extends DiffUtil.Callback {

    List<Place> oldPlaces;
    List<Place> newPlaces;

    public PlacesDiffCallback(List<Place> newPlaces, List<Place> oldPlaces) {
        this.newPlaces = newPlaces;
        this.oldPlaces = oldPlaces;
    }


    @Override
    public int getOldListSize() {
        return oldPlaces != null ? oldPlaces.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newPlaces != null ? newPlaces.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldPlaces.get(oldItemPosition).getId() == newPlaces.get(newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldPlaces.get(oldItemPosition).equals(newPlaces.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
