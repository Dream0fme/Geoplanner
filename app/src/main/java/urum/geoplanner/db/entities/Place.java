package urum.geoplanner.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;


@Entity(tableName = "places")
public class Place implements Serializable {

    @PrimaryKey(autoGenerate=true)
    private long id;

    @ColumnInfo(name = "sort_id")
    public int sort_id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "activation", defaultValue = "1")
    public int activation;

    @ColumnInfo(name = "archiving", defaultValue = "0")
    public int archiving;

    @ColumnInfo(name = "address")
    private String address;

    @ColumnInfo(name = "condition")
    private int condition;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "checkboxEnter")
    private int checkboxEnter;

    @ColumnInfo(name = "position")
    private int position;

    @ColumnInfo(name = "number")
    private String number;

    @ColumnInfo(name = "sms")
    private String sms;

    @ColumnInfo(name = "checkboxExit")
    private int checkboxExit;

    @ColumnInfo(name = "positionExit")
    private int positionExit;

    @ColumnInfo(name = "numberExit")
    private String numberExit;

    @ColumnInfo(name = "smsExit")
    private String smsExit;


    public Place(long id, String name, String address, int condition, double latitude, double longitude, int checkboxEnter, int position,
                 String number, String sms, int checkboxExit, int positionExit, String numberExit, String smsExit) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.condition = condition;
        this.latitude = latitude;
        this.longitude = longitude;
        this.checkboxEnter = checkboxEnter;
        this.position = position;
        this.number = number;
        this.sms = sms;
        this.checkboxExit = checkboxExit;
        this.positionExit = positionExit;
        this.numberExit = numberExit;
        this.smsExit = smsExit;
    }

    @Ignore
    public Place() {
    }

    public int getActivation() {
        return activation;
    }

    public void setActivation(int activation) {
        this.activation = activation;
    }

    public void getAttrPlace(long id, String name, String address, int condition, double latitude, double longitude, int checkboxEnter, int position,
                             String number, String sms, int checkboxExit, int positionExit, String numberExit, String smsExit) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.condition = condition;
        this.latitude = latitude;
        this.longitude = longitude;
        this.checkboxEnter = checkboxEnter;
        this.position = position;
        this.number = number;
        this.sms = sms;
        this.checkboxExit = checkboxExit;
        this.positionExit = positionExit;
        this.numberExit = numberExit;
        this.smsExit = smsExit;
    }

    public void setSort(int sort) {
        this.sort_id = sort;
    }

    public int getSort() {
        return sort_id;
    }

    public int getArchiving() {
        return archiving;
    }

    public void setArchiving(int archiving) {
        this.archiving = archiving;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getSms() {
        return sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getCondition() {
        return condition;
    }

    public void setCondition(int condition) {
        this.condition = condition;
    }

    public int getCheckboxEnter() {
        return checkboxEnter;
    }

    public void setCheckboxEnter(int checkboxEnter) {
        this.checkboxEnter = checkboxEnter;
    }

    public int getCheckboxExit() {
        return checkboxExit;
    }

    public void setCheckboxExit(int checkboxExit) {
        this.checkboxExit = checkboxExit;
    }

    public int getPositionExit() {
        return positionExit;
    }

    public void setPositionExit(int positionExit) {
        this.positionExit = positionExit;
    }

    public String getNumberExit() {
        return numberExit;
    }

    public void setNumberExit(String numberExit) {
        this.numberExit = numberExit;
    }

    public String getSmsExit() {
        return smsExit;
    }

    public void setSmsExit(String smsExit) {
        this.smsExit = smsExit;
    }

    @Override
    public String toString() {
        return "Place{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", condition=" + condition +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", checkboxEnter=" + checkboxEnter +
                ", position=" + position +
                ", number='" + number + '\'' +
                ", sms='" + sms + '\'' +
                ", checkboxExit=" + checkboxExit +
                ", positionExit=" + positionExit +
                ", numberExit='" + numberExit + '\'' +
                ", smsExit='" + smsExit + '\'' +
                '}';
    }
}
