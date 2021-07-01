package urum.geoplanner.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import urum.geoplanner.db.entities.Place;
import urum.geoplanner.db.entities.PlaceLog;


@Dao
public interface PlaceDao {

    // Select by id
    @Query("SELECT * FROM places WHERE id = :id")
    Place getPlace(long id);

    // Select All
    @Query("SELECT * FROM places WHERE archiving = 0 ORDER BY sort_id ASC")
    LiveData<List<Place>> getPlacesFromPlaces();

    @Query("SELECT * FROM places WHERE archiving = 0 and activation = 1 ORDER BY name ASC")
    LiveData<List<Place>> getPlacesToService();

    @Query("SELECT * FROM places WHERE archiving = 1 ORDER BY name ASC")
    LiveData<List<Place>> getPlacesFromArchive();

    @Query("SELECT * FROM places_log ORDER BY name ASC")
    LiveData<List<PlaceLog>> getPlacesFromLog();

    // Inserts
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertToPlaces(Place place);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertToArchive(Place place);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertToLog(PlaceLog place);

    @Query("UPDATE places SET archiving = 0 WHERE archiving = 1")
    void unarchiveAll();

    // Updates

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void updateToPlaces(Place place);

    /*@Update(onConflict = OnConflictStrategy.IGNORE)
    void updateToArchive(Place place);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void updateToLog(Place place);*/


    // Delete
    @Query("DELETE FROM places WHERE id = :id")
    void deleteFromPlaces(long id);

    @Query("DELETE FROM places WHERE id = :id and archiving = 1")
    void deleteFromArchive(long id);

    @Query("DELETE FROM places_log WHERE id = :id")
    void deleteFromLog(long id);

    // Delete All
    @Query("DELETE FROM places WHERE archiving = 0")
    void deleteFromPlacesAll();

    @Query("DELETE FROM places WHERE archiving = 1")
    void deleteFromArchiveAll();

    @Query("DELETE FROM places_log")
    void deleteFromLogAll();

}
