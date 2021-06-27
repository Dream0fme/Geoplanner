package urum.geoplanner.db.repository;


import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import urum.geoplanner.db.PlaceDao;
import urum.geoplanner.db.PlaceRoomDatabase;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.db.entities.PlaceLog;

public class PlaceRepository {

    private PlaceDao mPlaceDao;
    private LiveData<List<Place>> mAllPlaces;
    private LiveData<List<Place>> mAllPlacesArchive;
    public LiveData<List<Place>> mAllPlacesToService;
    private LiveData<List<PlaceLog>> mAllPlacesLog;

    public PlaceRepository(Application application) {
        PlaceRoomDatabase db = PlaceRoomDatabase.getDatabase(application);
        this.mPlaceDao = db.placeDao();
        this.mAllPlaces = this.mPlaceDao.getPlacesFromPlaces();
        this.mAllPlacesToService = this.mPlaceDao.getPlacesToService();
        this.mAllPlacesArchive = this.mPlaceDao.getPlacesFromArchive();
        this.mAllPlacesLog = this.mPlaceDao.getPlacesFromLog();
    }

    // Select place
    public Place getPlace(long id) {
        return this.mPlaceDao.getPlace(id);
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
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.insertToPlaces(place);
        });
    }

    public void insertToArchive(Place place) {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.insertToArchive(place);
        });
    }

    public void insertToLog(PlaceLog place) {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.insertToLog(place);
        });
    }

    // Updates
    public void updateToPlaces(Place place) {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.updateToPlaces(place);
        });
    }

    // Delete
    public void deleteFromPlaces(long id) {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.deleteFromPlaces(id);
        });
    }

    public void deleteFromArchive(long id) {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.deleteFromArchive(id);
        });
    }

    public void deleteFromLog(long id) {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.deleteFromLog(id);
        });
    }

    // Delete all

    public void deleteFromPlacesAll() {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.deleteFromPlacesAll();
        });
    }

    public void deleteFromArchiveAll() {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.deleteFromArchiveAll();
        });
    }

    public void deleteFromLogAll() {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.deleteFromLogAll();
        });
    }

    public void unarchiveAll() {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaceDao.unarchiveAll();
        });
    }
}
