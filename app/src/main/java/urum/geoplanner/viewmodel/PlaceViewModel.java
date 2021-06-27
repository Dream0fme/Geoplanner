package urum.geoplanner.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import urum.geoplanner.db.entities.Place;
import urum.geoplanner.db.entities.PlaceLog;
import urum.geoplanner.db.repository.PlaceRepository;

public class PlaceViewModel extends AndroidViewModel {

    public PlaceRepository mRepository;
    public final LiveData<List<Place>> mAllPlaces;
    public final LiveData<List<Place>> mAllPlacesArchive;
    public final LiveData<List<Place>> mAllPlacesToService;
    public final LiveData<List<PlaceLog>> mAllPlacesLog;

    public PlaceViewModel(@NonNull Application application) {
        super(application);
        mRepository = new PlaceRepository(application);
        mAllPlaces = mRepository.getPlacesFromPlaces();
        mAllPlacesArchive = mRepository.getPlacesFromArchive();
        mAllPlacesToService = mRepository.getPlacesToService();
        mAllPlacesLog = mRepository.getPlacesFromLog();
    }

    // select place
    public Place getPlace(long id) {
        return mRepository.getPlace(id);
    }

    // return all
    public LiveData<List<Place>> getPlacesFromPlaces() {
        return mAllPlaces;
    }

    public LiveData<List<Place>> getPlacesToService() {
        return mAllPlacesToService;
    }

    public LiveData<List<Place>> getPlacesFromArchive() {
        return mAllPlacesArchive;
    }

    public LiveData<List<PlaceLog>> getPlacesFromLog() {
        return mAllPlacesLog;
    }

    // Inserts
    public void insertToPlaces(Place place) {
        mRepository.insertToPlaces(place);
    }

    public void insertToLog(PlaceLog place) {
        mRepository.insertToLog(place);

    }

    // Updates
    public void updateToPlaces(Place place) {
        mRepository.updateToPlaces(place);
    }

    public void unarchiveAll() {
        mRepository.unarchiveAll();
    }

    // Delete
    public void deleteFromPlaces(long id) {
        mRepository.deleteFromPlaces(id);
    }

    public void deleteFromArchive(long id) {
        mRepository.deleteFromArchive(id);
    }

    public void deleteFromLog(long id) {
        mRepository.deleteFromLog(id);

    }

    // Delete all

    public void deleteFromPlacesAll() {
        mRepository.deleteFromPlacesAll();
    }

    public void deleteFromArchiveAll() {
        mRepository.deleteFromArchiveAll();
    }

    public void deleteFromLogAll() {
        mRepository.deleteFromLogAll();
    }
}
