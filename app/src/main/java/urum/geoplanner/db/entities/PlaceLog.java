package urum.geoplanner.db.entities;


import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "places_log")
public
class PlaceLog extends Place {

    @ColumnInfo(name = "date")
    private String date;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
