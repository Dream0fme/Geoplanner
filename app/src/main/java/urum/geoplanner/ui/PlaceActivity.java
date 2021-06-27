package urum.geoplanner.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.content.CursorLoader;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import urum.geoplanner.R;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.service.ConnectorService;
import urum.geoplanner.viewmodel.ModelFactory;
import urum.geoplanner.viewmodel.PlaceViewModel;

import static urum.geoplanner.utils.Utils.round;

@SuppressLint("NewApi")
public class PlaceActivity extends AppCompatActivity {
    private static final String TAG = "mytag";
    private String PACKAGE_NAME;
    Geocoder geocoder;
    List<Address> addresses;
    String errorMessage;
    CheckBox entering, exiting;
    TextInputEditText nameBox;
    AutoCompleteTextView addressBox;
    TextInputEditText conditionBox;
    TextInputEditText number, numberExit;
    TextInputEditText sms, smsExit;
    Button delButton;
    Button saveButton;
    MaterialButton btnGoogleMap, btnNumberEnter, btnNumberExit;
    Spinner spinner, spinnerExit;
    RelativeLayout linearLayout, linearLayoutEntering, linearLayoutExit;
    TextInputLayout addressLayout, radiusBox;
    TextInputLayout layoutNumber, layoutNumberExit;
    TextInputLayout layoutSms, layoutSmsExit;
    TextView addressFromList;
    long placeId = 0;
    double lat = 0.0;
    double lng = 0.0;
    boolean fromMaps;
    boolean fromMain = false;
    String addressFromMaps = "";
    NotificationManager mNotificationManager;
    String addressFromDB = "";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    int posEnter = -1;
    int posExit = -1;
    Snackbar snackbarDND;
    Snackbar snackbarSettings;
    Snackbar snackbarPermission;
    Toolbar mToolbar;

    private ConnectorService connectorService;

    private PlaceViewModel mPlaceViewModel;




    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place);
        PACKAGE_NAME = this.getPackageName();

        initToolbar();

        connectorService = new ConnectorService(this);
        getLifecycle().addObserver(connectorService);

        snackbarDND = Snackbar.make(findViewById(R.id.activity_place),
                R.string.access_for_dnd,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkPermissionDND();
                    }

                });

        snackbarSettings = Snackbar.make(findViewById(R.id.activity_place),
                R.string.access_manual,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + PACKAGE_NAME)));
                    }
                });

        snackbarCreate(2);

        //mPlaceViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(PlaceViewModel.class);
        mPlaceViewModel = new ViewModelProvider(this,
                new ModelFactory(getApplication())).get(PlaceViewModel.class);

        registerReceiver(CloseReceiver, new IntentFilter("closeApp"));
        mNotificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        linearLayout = findViewById(R.id.linLayout);
        btnGoogleMap = findViewById(R.id.googleMapBtn);
        btnNumberEnter = findViewById(R.id.searchNubmerEnter); // linLayoutEntering , linearLayoutEntering
        btnNumberExit = findViewById(R.id.searchNubmerExit); //linLayoutExit , linearLayoutExit
        addressLayout = findViewById(R.id.addressBox);
        radiusBox = findViewById(R.id.radiusBox);
        linearLayoutEntering = findViewById(R.id.linLayoutEntering);
        linearLayoutExit = findViewById(R.id.linLayoutExit);
        layoutNumber = findViewById(R.id.txtInputLnumber);
        layoutSms = findViewById(R.id.txtInputLsms);
        layoutNumberExit = findViewById(R.id.textInputLayoutNum);
        layoutSmsExit = findViewById(R.id.textInputLayoutSms);
        numberExit = findViewById(R.id.numberExit);
        smsExit = findViewById(R.id.editSmsExit);
        String[] actions = getResources().getStringArray(R.array.actions);

        geocoder = new Geocoder(this, Locale.getDefault());
        addresses = null;
        nameBox = findViewById(R.id.nameBox);
        addressBox = findViewById(R.id.address);
        addressBox.setThreshold(1);
        //addressBox.setAdapter(new CustomAdapter(this, android.R.layout.simple_list_item_1));
        number = findViewById(R.id.number);
        sms = findViewById(R.id.editSMS);
        conditionBox = findViewById(R.id.condition);
        entering = findViewById(R.id.entering);
        exiting = findViewById(R.id.exiting);

        entering.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    linearLayout.addView(linearLayoutEntering);
                    ((RelativeLayout.LayoutParams) exiting.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.linLayoutEntering);
                    posEnter = spinner.getSelectedItemPosition();

                    if (posEnter == 3 || posEnter == 4) {
                        if (!snackbarDND.isShown()) {
                            if (mNotificationManager != null) {
                                if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                                    snackbarDND.show();
                                }
                            }
                        }
                    } else if (posEnter == 0 || posEnter == 1) {
                        if (!snackbarPermission.isShown() || !snackbarSettings.isShown()) {
                            checkPermission(posEnter);
                        }
                    }

                } else {
                    posEnter = -1;
                    linearLayout.removeView(linearLayoutEntering);
                    ((RelativeLayout.LayoutParams) exiting.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.entering);
                    if (snackbarDND.isShown()) {
                        if (exiting.isChecked() & !(spinnerExit.getSelectedItemPosition() == 3 || spinnerExit.getSelectedItemPosition() == 4)) {
                            snackbarDND.dismiss();
                        } else if (!exiting.isChecked()) {
                            snackbarDND.dismiss();
                        }
                    } else if (snackbarPermission.isShown()) {
                        if (exiting.isChecked() & !(spinnerExit.getSelectedItemPosition() == 0 || spinnerExit.getSelectedItemPosition() == 1)) {
                            snackbarPermission.dismiss();
                        } else if (!exiting.isChecked()) {
                            snackbarPermission.dismiss();
                        }
                    } else if (snackbarSettings.isShown()) {
                        if (exiting.isChecked() & !(spinnerExit.getSelectedItemPosition() == 0 || spinnerExit.getSelectedItemPosition() == 1)) {
                            snackbarSettings.dismiss();
                        } else if (!exiting.isChecked()) {
                            snackbarSettings.dismiss();
                        }
                    }
                }
            }
        });

        exiting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    linearLayout.addView(linearLayoutExit);
                    posExit = spinnerExit.getSelectedItemPosition();

                    if (spinnerExit.getSelectedItemPosition() == 3 || spinnerExit.getSelectedItemPosition() == 4) {
                        if (!snackbarDND.isShown()) {
                            if (mNotificationManager != null) {
                                if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                                    snackbarDND.show();
                                }
                            }
                        }
                    } else if (posExit == 0 || posExit == 1) {
                        if (!snackbarPermission.isShown() || !snackbarSettings.isShown()) {
                            checkPermission(posExit);
                        }
                    }

                } else {
                    posExit = -1;
                    linearLayout.removeView(linearLayoutExit);
                    if (snackbarDND.isShown()) {
                        if (entering.isChecked() & !(spinner.getSelectedItemPosition() == 3 || spinner.getSelectedItemPosition() == 4)) {
                            snackbarDND.dismiss();
                        } else if (!entering.isChecked()) {
                            snackbarDND.dismiss();
                        }
                    } else if (snackbarPermission.isShown()) {
                        if (entering.isChecked() & !(spinner.getSelectedItemPosition() == 0 || spinner.getSelectedItemPosition() == 1)) {
                            snackbarPermission.dismiss();
                        } else if (!exiting.isChecked()) {
                            snackbarPermission.dismiss();
                        }
                    } else if (snackbarSettings.isShown()) {
                        if (entering.isChecked() & !(spinner.getSelectedItemPosition() == 0 || spinner.getSelectedItemPosition() == 1)) {
                            snackbarSettings.dismiss();
                        } else if (!exiting.isChecked()) {
                            snackbarSettings.dismiss();
                        }
                    }
                }
            }
        });

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, actions);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = findViewById(R.id.spinner);
        spinner.setAdapter(adapterSpinner);
        spinner.setPrompt(getString(R.string.choose_action));
        spinner.setSelection(2, true);
        addOrRemoveView(2);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                posEnter = position;
                addOrRemoveView(position);
                checkPermission(position);
                snackbarCreate(position);

                if (!(position == 0 || position == 1) &
                        !(spinnerExit.getSelectedItemPosition() == 0 || spinnerExit.getSelectedItemPosition() == 1)) {
                    if (snackbarPermission.isShown()) {
                        snackbarPermission.dismiss();
                        //checkPermission(position);
                    } else if (snackbarSettings.isShown()) {
                        snackbarSettings.dismiss();
                        // checkPermission(position);
                    }
                }


                if (position == 0 || position == 1) {
                    if (!snackbarPermission.isShown() || !snackbarSettings.isShown()) {
                        //checkPermission(position);
                    }
                }


                if (position != 3 & !(spinnerExit.getSelectedItemPosition() == 3 || spinnerExit.getSelectedItemPosition() == 4)) {
                    if (snackbarDND.isShown()) {
                        snackbarDND.dismiss();
                    }
                }
                if (position != 4 & !(spinnerExit.getSelectedItemPosition() == 3 || spinnerExit.getSelectedItemPosition() == 4)) {

                    if (snackbarDND.isShown()) {
                        snackbarDND.dismiss();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        spinnerExit = findViewById(R.id.spinnerExit);
        spinnerExit.setAdapter(adapterSpinner);
        spinnerExit.setAdapter(adapterSpinner);
        spinnerExit.setPrompt(getString(R.string.choose_action));
        spinnerExit.setSelection(2, true);
        addOrRemoveNewView(2);
        spinnerExit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                posExit = position;
                addOrRemoveNewView(position);
                checkPermission(position);
                snackbarCreate(position);

                if (position != 0 & !(spinner.getSelectedItemPosition() == 0)) {
                    if (snackbarPermission.isShown()) {
                        snackbarPermission.dismiss();
                    } else if (snackbarSettings.isShown()) {
                        snackbarSettings.dismiss();
                    }
                }

                if (position != 1 & !(spinner.getSelectedItemPosition() == 1)) {
                    if (snackbarPermission.isShown()) {
                        snackbarPermission.dismiss();
                    } else if (snackbarSettings.isShown()) {
                        snackbarSettings.dismiss();
                    }
                }

                if (position != 3 & !(spinner.getSelectedItemPosition() == 3 || spinner.getSelectedItemPosition() == 4)) {
                    if (snackbarDND.isShown()) {
                        snackbarDND.dismiss();
                    }
                }
                if (position != 4 & !(spinner.getSelectedItemPosition() == 3 || spinner.getSelectedItemPosition() == 4)) {
                    if (snackbarDND.isShown()) {
                        snackbarDND.dismiss();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });


        delButton = findViewById(R.id.deleteButton);
        saveButton = findViewById(R.id.saveButton);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if(extras.getBoolean("FROM_NOTIFICATION_TO_PLACEACTIVITY")){
                placeId = extras.getLong("id_notification", 0);
            } else placeId = extras.getLong("id");

            fromMaps = extras.getBoolean("fromMaps");
            addressFromMaps = extras.getString("addressFromMaps", "");
            fromMain = extras.getBoolean("fromMain");
        }

        if (placeId > 0) { // если 0, то добавление
            Place place = mPlaceViewModel.getPlace(placeId);
            nameBox.setText(place.getName());
            conditionBox.setText(Integer.toString(place.getCondition()));
            Pattern patternGEO = Pattern.compile("^(\\-?\\d+(\\.\\d+)?),\\s*(\\-?\\d+(\\.\\d+)?)$");
            Matcher geoMatcher = patternGEO.matcher(place.getAddress());
            if (!geoMatcher.matches()) {
                addressBox.setText(place.getAddress());
                addressFromDB = place.getAddress();
                if (!addressBox.isEnabled()) {
                    addressBox.setEnabled(true);
                }
            } else {
                //linearLayout.removeView(addressLayout);
                //linearLayout.removeView(btnGoogleMap);
                //((RelativeLayout.LayoutParams) radiusBox.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.nameBoxInput);
                addressBox.setText(round(place.getLatitude(), 5) + ", " + round(place.getLongitude(), 5));
                if (addressBox.isEnabled()) {
                    addressBox.setEnabled(false);
                }
            }

            if (place.getCheckboxEnter() == 1) {
                entering.setChecked(true);
                spinner.setSelection(place.getPosition(), true);
                addOrRemoveView(place.getPosition());

                if (findViewById(R.id.txtInputLnumber) != null) {
                    number.setText(place.getNumber());
                }
                if (findViewById(R.id.txtInputLsms) != null) {
                    sms.setText(place.getSms());
                }
            } else if (place.getCheckboxEnter() == 0) {
                entering.setChecked(false);
            }

            if (place.getCheckboxExit() == 1) {
                exiting.setChecked(true);
                spinnerExit.setSelection(place.getPositionExit(), true);
                addOrRemoveNewView(place.getPositionExit());
                if (findViewById(R.id.textInputLayoutNum) != null) {
                    numberExit.setText(place.getNumberExit());
                }
                if (findViewById(R.id.textInputLayoutSms) != null) {
                    smsExit.setText(place.getSmsExit());
                }
            } else if (place.getCheckboxExit() == 0) {
                exiting.setChecked(false);
            }
            lat = place.getLatitude();
            lng = place.getLongitude();

        } else {
            entering.setChecked(false);
            exiting.setChecked(false);
            if (extras != null) {
                if (fromMaps) {
                    if (addressFromMaps.equals("")) {
                        //linearLayout.removeView(addressLayout);
                        //linearLayout.removeView(btnGoogleMap);
                        //((RelativeLayout.LayoutParams) radiusBox.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.nameBoxInput);
                        lat = extras.getDouble("latFromMaps");
                        lng = extras.getDouble("lngFromMaps");
                        addressBox.setText(round(lat, 5) + ", " + round(lng, 5));
                        if (addressBox.isEnabled()) {
                            addressBox.setEnabled(false);
                        }

                        //Log.i("mytag", "lat" + lat + " lng: " + lng);
                    } else {
                        addressBox.setText(addressFromMaps);
                        lat = extras.getDouble("latFromMaps");
                        lng = extras.getDouble("lngFromMaps");
                        if (!addressBox.isEnabled()) {
                            addressBox.setEnabled(true);
                        }
                    }
                }
            }
            // скрываем кнопку удаления
            delButton.setVisibility(View.GONE);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            goHome();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initToolbar() {
        mToolbar = findViewById(R.id.toolbar_place);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
    }


    public void addOrRemoveView(int pos) {
        switch (pos) {
            case 0:
                if (findViewById(R.id.txtInputLnumber) == null) {
                    linearLayoutEntering.addView(layoutNumber);
                    linearLayoutEntering.addView(btnNumberEnter);
                }
                if (findViewById(R.id.txtInputLsms) != null) {
                    linearLayoutEntering.removeView(layoutSms);
                }
                break;
            case 1:
                if (findViewById(R.id.txtInputLnumber) == null) {
                    linearLayoutEntering.addView(layoutNumber);
                    linearLayoutEntering.addView(btnNumberEnter);
                }
                if (findViewById(R.id.txtInputLsms) == null) {
                    ((RelativeLayout.LayoutParams) layoutSms.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.txtInputLnumber);
                    linearLayoutEntering.addView(layoutSms);
                    layoutSms.setCounterMaxLength(160);
                    layoutSms.setHint(getString(R.string.input_sms));
                } else {
                    ((RelativeLayout.LayoutParams) layoutSms.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.txtInputLnumber);
                    layoutSms.setCounterMaxLength(160);
                    layoutSms.setHint(getString(R.string.input_sms));
                }
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                if (findViewById(R.id.txtInputLnumber) != null) {
                    linearLayoutEntering.removeView(layoutNumber);
                    linearLayoutEntering.removeView(btnNumberEnter);

                }
                if (findViewById(R.id.txtInputLsms) != null) {
                    linearLayoutEntering.removeView(layoutSms);
                }
                break;
            case 2:
                if (findViewById(R.id.txtInputLnumber) != null) {
                    linearLayoutEntering.removeView(layoutNumber);
                    linearLayoutEntering.removeView(btnNumberEnter);
                }
                if (findViewById(R.id.txtInputLsms) == null) {
                    linearLayoutEntering.addView(layoutSms);
                    ((RelativeLayout.LayoutParams) layoutSms.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.spinner);
                    layoutSms.setCounterMaxLength(300);
                    layoutSms.setHint(getString(R.string.input_notification));
                } else {
                    ((RelativeLayout.LayoutParams) layoutSms.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.spinner);
                    layoutSms.setCounterMaxLength(300);
                    layoutSms.setHint(getString(R.string.input_notification));
                }
                break;

        }
    }

    public void addOrRemoveNewView(int pos) {
        switch (pos) {
            case 0:
                if (findViewById(R.id.textInputLayoutNum) == null) {
                    linearLayoutExit.addView(layoutNumberExit);
                    linearLayoutExit.addView(btnNumberExit);
                }
                if (findViewById(R.id.textInputLayoutSms) != null) {
                    linearLayoutExit.removeView(layoutSmsExit);
                }
                break;
            case 1:
                if (findViewById(R.id.textInputLayoutNum) == null) {
                    linearLayoutExit.addView(layoutNumberExit);
                    linearLayoutExit.addView(btnNumberExit);
                }
                if (findViewById(R.id.textInputLayoutSms) == null) {
                    linearLayoutExit.addView(layoutSmsExit);
                    ((RelativeLayout.LayoutParams) layoutSmsExit.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.textInputLayoutNum);
                    layoutSmsExit.setCounterMaxLength(160);
                    layoutSmsExit.setHint(getString(R.string.input_sms));
                } else {
                    ((RelativeLayout.LayoutParams) layoutSmsExit.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.textInputLayoutNum);
                    layoutSmsExit.setCounterMaxLength(160);
                    layoutSmsExit.setHint(getString(R.string.input_sms));
                }
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                if (findViewById(R.id.textInputLayoutNum) != null) {
                    linearLayoutExit.removeView(layoutNumberExit);
                    linearLayoutExit.removeView(btnNumberExit);
                }
                if (findViewById(R.id.textInputLayoutSms) != null) {
                    linearLayoutExit.removeView(layoutSmsExit);
                }
                break;
            case 2:
                if (findViewById(R.id.textInputLayoutNum) != null) {
                    linearLayoutExit.removeView(layoutNumberExit);
                    linearLayoutExit.removeView(btnNumberExit);
                }
                if (findViewById(R.id.textInputLayoutSms) == null) {
                    linearLayoutExit.addView(layoutSmsExit);
                    ((RelativeLayout.LayoutParams) layoutSmsExit.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.spinnerExit);
                    layoutSmsExit.setCounterMaxLength(300);
                    layoutSmsExit.setHint(getString(R.string.input_notification));
                } else {
                    ((RelativeLayout.LayoutParams) layoutSmsExit.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.spinnerExit);
                    layoutSmsExit.setCounterMaxLength(300);
                    layoutSmsExit.setHint(getString(R.string.input_notification));
                }
                break;

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(CloseReceiver);
    }

    private static boolean validatePhoneNumber(String phoneNo) {
        if (phoneNo.matches("\\d{10}")) return true;
        else if (phoneNo.matches("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4}")) return true;
        else if (phoneNo.matches("\\d{3}-\\d{3}-\\d{4}\\s(x|(ext))\\d{3,5}")) return true;
        else if (phoneNo.matches("^(\\+7|7|8)?[\\s\\-]?\\(?[489][0-9]{2}\\)?[\\s\\-]?[0-9]{3}[\\s\\-]?[0-9]{2}[\\s\\-]?[0-9]{2}$"))
            return true;
        else if (phoneNo.matches("\\(\\d{3}\\)-\\d{3}-\\d{4}\\d{2}-\\d{2}")) return true;
        else if (phoneNo.matches("^(\\+\\d{1,3}( )?)?((\\(\\d{1,3}\\))|\\d{1,3})[- .]?\\d{3,4}[- .]?\\d{4}$"))
            return true;
        else if (phoneNo.matches("^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$"
                + "|^(\\+\\d{1,3}( )?)?(\\d{3}[ ]?){2}\\d{3}$"
                + "|^(\\+\\d{1,3}( )?)?(\\d{3}[ ]?)(\\d{2}[ ]?){2}\\d{2}$")) return true;
        else return false;

    }

    @SuppressLint("SetTextI18n")
    public void save(View view) {
        if (nameBox.getText().toString().trim().length() == 0) {
            Toast.makeText(getApplicationContext(), "Введите название места!", Toast.LENGTH_LONG).show();
        } else if (nameBox.getText().toString().trim().length() > 25) {
            Toast.makeText(getApplicationContext(), "Слишком длинное название!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.addressBox) != null & addressBox.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "Введите адрес!", Toast.LENGTH_LONG).show();
        } else if (conditionBox.getText().toString().equals("") || conditionBox.length() > 6) {
            Toast.makeText(getApplicationContext(), "Слишком большой радиус!", Toast.LENGTH_LONG).show();
        } else if (conditionBox.getText().toString().equals("") || Integer.parseInt(conditionBox.getText().toString()) < 50) {
            Toast.makeText(getApplicationContext(), "Радиус не может быть меньше 50 метров!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.txtInputLnumber) != null & number.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "Введите номер!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.txtInputLnumber) != null & !validatePhoneNumber(number.getText().toString())) {
            Toast.makeText(getApplicationContext(), "Номер введён неправильно!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.txtInputLsms) != null & spinner.getSelectedItemPosition() == 1 & sms.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "Введите ваше сообщение!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.txtInputLsms) != null & spinner.getSelectedItemPosition() == 2 & sms.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "Введите ваше уведомление!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.txtInputLsms) != null & spinner.getSelectedItemPosition() == 2 & sms.getText().length() > 300) {
            Toast.makeText(getApplicationContext(), "Слишком длинное уведомление!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.txtInputLsms) != null & spinner.getSelectedItemPosition() == 1 & sms.getText().length() > 160) {
            Toast.makeText(getApplicationContext(), "Слишком длинное сообщение!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.textInputLayoutNum) != null & numberExit.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "Введите номер!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.textInputLayoutNum) != null & !validatePhoneNumber(numberExit.getText().toString())) {
            Toast.makeText(getApplicationContext(), "Номер введён неправильно!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.textInputLayoutSms) != null & spinnerExit.getSelectedItemPosition() == 2 & smsExit.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "Введите ваше уведомление!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.textInputLayoutSms) != null & spinnerExit.getSelectedItemPosition() == 1 & smsExit.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "Введите ваше сообщение!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.textInputLayoutSms) != null & spinnerExit.getSelectedItemPosition() == 2 & smsExit.getText().length() > 300) {
            Toast.makeText(getApplicationContext(), "Слишком длинное уведомление!", Toast.LENGTH_LONG).show();
        } else if (findViewById(R.id.textInputLayoutSms) != null & spinnerExit.getSelectedItemPosition() == 1 & smsExit.getText().length() > 160) {
            Toast.makeText(getApplicationContext(), "Слишком длинное сообщение!", Toast.LENGTH_LONG).show();
        } else {
            try {
                int checkBoxEnter, checkBoxExit;
                if (entering.isChecked()) {
                    checkBoxEnter = 1;
                } else checkBoxEnter = 0;

                if (exiting.isChecked()) {
                    checkBoxExit = 1;
                } else checkBoxExit = 0;
                String namePlace;
                String addressEdit = "";
                int condition;
                String phone;
                String msg;
                String phoneExit;
                String msgExit;
                Address addressName;
                //double latitude, longitude;
                if (addressBox.isEnabled()) {
                    addressEdit = addressBox.getText().toString().trim();
                    Log.d(TAG, "АДРЕС: " + addressEdit);
                    addresses = geocoder.getFromLocationName(addressEdit, 5);
                    if (addresses == null || addresses.size() == 0) {
                        errorMessage = "Не найден";
                        Log.d(TAG, errorMessage);
                        Toast.makeText(getApplicationContext(), "Адрес не найден. Попробуйте ещё раз.", Toast.LENGTH_LONG).show();
                    } else {
                        Address address = addresses.get(0);

                        for (int i = 0; i < addresses.size(); i++) {
                            if (addresses.size() == 1) {
                                break;
                            } else {
                                if (address.getLocality() != null || address.getThoroughfare() != null || address.getFeatureName() != null) {
                                    address = addresses.get(i);
                                    break;
                                }
                            }
                        }

                        namePlace = nameBox.getText().toString().trim();
                        condition = Integer.parseInt(conditionBox.getText().toString());
                        phone = number.getText().toString().trim();
                        msg = sms.getText().toString().trim();
                        int position = spinner.getSelectedItemPosition();
                        int positionExit = spinnerExit.getSelectedItemPosition();
                        phoneExit = numberExit.getText().toString().trim();
                        msgExit = smsExit.getText().toString().trim();
                        addressName = address;
                        if (!fromMaps) {
                            if (lat == 0.0 & lng == 0.0) {
                                lat = addressName.getLatitude();
                                lng = addressName.getLongitude();
                            } else if (!addressFromDB.equalsIgnoreCase(addressBox.getText().toString())) {
                                lat = addressName.getLatitude();
                                lng = addressName.getLongitude();
                            }
                        }
                        Place place = new Place(placeId, namePlace, addressEdit, condition, lat, lng, checkBoxEnter, position, phone, msg, checkBoxExit, positionExit, phoneExit, msgExit);

                        if (placeId > 0) {
                            place.setActivation(1);
                            mPlaceViewModel.updateToPlaces(place);
                        } else {
                            place.setActivation(1);
                            mPlaceViewModel.insertToPlaces(place);
                        }
                        view.setEnabled(false);
                        goHome();
                    }
                } else {
                    addressEdit = round(lat, 5) + ",\n" + round(lng, 5);
                    namePlace = nameBox.getText().toString().trim();
                    condition = Integer.parseInt(conditionBox.getText().toString());
                    phone = number.getText().toString().trim();
                    msg = sms.getText().toString().trim();
                    int position = spinner.getSelectedItemPosition();
                    int positionExit = spinnerExit.getSelectedItemPosition();
                    phoneExit = numberExit.getText().toString().trim();
                    msgExit = smsExit.getText().toString().trim();
                    Place place = new Place(placeId, namePlace, addressEdit, condition, lat, lng, checkBoxEnter, position, phone, msg, checkBoxExit, positionExit, phoneExit, msgExit);
                    if (placeId > 0) {
                        place.setActivation(1);
                        mPlaceViewModel.updateToPlaces(place);
                        //replyIntent.putExtra("Place", place);
                        //setResult(RESULT_OK, replyIntent);

                    } else {
                        place.setActivation(1);
                        mPlaceViewModel.insertToPlaces(place);
                    }
                    view.setEnabled(false);
                    goHome();
                }
            } catch (IOException ioException) {
                Log.d(TAG, "Сервис не работает", ioException);
                Toast.makeText(getApplicationContext(), "Сервис не работает", Toast.LENGTH_LONG).show();

            } catch (IllegalArgumentException illegalArgumentException) {
                Log.d(TAG, "Используется неверная широта или долгота", illegalArgumentException);
                Toast.makeText(getApplicationContext(), "Используется неверная широта или долгота", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void openGoogleMap(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("fromActivityPlace", true);
        Pattern patternGEO = Pattern.compile("^(\\-?\\d+(\\.\\d+)?),\\s*(\\-?\\d+(\\.\\d+)?)$");
        Matcher geoMatcher = patternGEO.matcher(addressBox.getText().toString());
        if (!addressBox.getText().toString().equals("") && !addressFromDB.equalsIgnoreCase(addressBox.getText().toString()) && !geoMatcher.matches()) {
            try {
                String addressEdit = addressBox.getText().toString().trim();
                Log.d(TAG, "АДРЕС: " + addressEdit);
                addresses = geocoder.getFromLocationName(addressEdit, 5);
                if (addresses == null || addresses.size() == 0) {
                    errorMessage = "Не найден";
                    Log.d(TAG, errorMessage);
                    Toast.makeText(getApplicationContext(), "Адрес не найден. Попробуйте ещё раз.", Toast.LENGTH_LONG).show();
                } else {
                    Address address = addresses.get(0);

                    for (int i = 0; i < addresses.size(); i++) {
                        if (addresses.size() == 1) {
                            break;
                        } else {
                            if (address.getLocality() != null || address.getThoroughfare() != null || address.getFeatureName() != null) {
                                address = addresses.get(i);
                                break;
                            }
                        }
                    }
                    intent.putExtra("lat", address.getLatitude());
                    intent.putExtra("lng", address.getLongitude());
                    startActivityForResult(intent, 1);
                }
            } catch (IOException ioException) {
                Log.d(TAG, "Сервис не работает", ioException);
                Toast.makeText(getApplicationContext(), "Сервис не работает", Toast.LENGTH_LONG).show();

            } catch (IllegalArgumentException illegalArgumentException) {
                Log.d(TAG, "Используется неверная широта или долгота", illegalArgumentException);
                Toast.makeText(getApplicationContext(), "Используется неверная широта или долгота", Toast.LENGTH_LONG).show();
            }
        } else {
            intent.putExtra("lat", lat);
            intent.putExtra("lng", lng);
            startActivityForResult(intent, 1);
        }
    }

    @SuppressLint("IntentReset")
    public void openContactsBookEnter(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, 2);
    }

    @SuppressLint("IntentReset")
    public void openContactsBookExit(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, 3);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
        super.onActivityResult(requestCode, resultCode, i);
        if (i == null) {
            return;
        }
        switch (requestCode) {
            case 1:
                fromMaps = i.getBooleanExtra("fromMaps", false);
                addressFromMaps = i.getStringExtra("addressFromMaps");
                Log.d(TAG, addressFromMaps);
                if (fromMaps) {
                    if (addressFromMaps.equals("")) {
                        //linearLayout.removeView(addressLayout);
                        //linearLayout.removeView(btnGoogleMap);
                        //((RelativeLayout.LayoutParams) radiusBox.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.nameBoxInput);
                        lat = i.getDoubleExtra("latFromMaps", 0.0);
                        lng = i.getDoubleExtra("lngFromMaps", 0.0);
                        addressBox.setText(round(lat, 5) + ", " + round(lng, 5));
                        if (addressBox.isEnabled()) {
                            addressBox.setEnabled(false);
                        }
                    } else {
                        addressBox.setText(addressFromMaps);
                        Log.i(TAG, addressFromMaps);
                        Log.i(TAG, "lat" + lat + " lng: " + lng);
                        lat = i.getDoubleExtra("latFromMaps", 0.0);
                        lng = i.getDoubleExtra("lngFromMaps", 0.0);
                        if (!addressBox.isEnabled()) {
                            addressBox.setEnabled(true);
                        }
                    }
                }
                break;
            case 2:
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactsData = i.getData();
                    CursorLoader loader;
                    if (contactsData != null) {
                        loader = new CursorLoader(this, contactsData, null, null, null, null);
                        Cursor c = loader.loadInBackground();
                        if (c != null && c.moveToFirst()) {
                            number.setText(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        }
                    }
                }
                break;
            case 3:
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactsData = i.getData();
                    CursorLoader loader;
                    if (contactsData != null) {
                        loader = new CursorLoader(this, contactsData, null, null, null, null);
                        Cursor c = loader.loadInBackground();
                        if (c != null && c.moveToFirst()) {
                            numberExit.setText(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        goHome();
    }

    public void delete(View view) {
        mPlaceViewModel.deleteFromPlaces(placeId);
        goHome();
    }

    private void goHome() {
        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();

        boolean permissionCall = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE);
        boolean permissionSMS = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS);
        boolean permissionReadContacts = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS);

        boolean showRationaleCall;
        boolean showRationaleSMS;
        boolean showRationaleRead;
        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
            if (entering.isChecked() & (spinner.getSelectedItemPosition() == 3 || spinner.getSelectedItemPosition() == 4)) {
                grantResult(true, null, posEnter, mNotificationManager.isNotificationPolicyAccessGranted());
            }
            if (exiting.isChecked() & (spinnerExit.getSelectedItemPosition() == 3 || spinnerExit.getSelectedItemPosition() == 4)) {
                grantResult(true, null, posExit, mNotificationManager.isNotificationPolicyAccessGranted());
            }
        }


        if (entering.isChecked() & spinner.getSelectedItemPosition() == 0) {
            if (!permissionCall || !permissionSMS || !permissionReadContacts) {
                showRationaleCall = shouldShowRequestPermissionRationale("android.permission.CALL_PHONE");
                showRationaleRead = shouldShowRequestPermissionRationale("android.permission.READ_CONTACTS");
                if (!showRationaleCall & !permissionCall) {
                    grantResult(true, "android.permission.CALL_PHONE", posEnter, true);
                } else if (!showRationaleRead & !permissionReadContacts) {
                    grantResult(true, "android.permission.READ_CONTACTS", posEnter, true);
                    //checkPermission(posEnter);

                }
            }
        }

        if (entering.isChecked() & spinner.getSelectedItemPosition() == 1) {
            if (!permissionCall || !permissionSMS || !permissionReadContacts) {
                showRationaleSMS = shouldShowRequestPermissionRationale("android.permission.SEND_SMS");
                showRationaleRead = shouldShowRequestPermissionRationale("android.permission.READ_CONTACTS");
                if (!showRationaleSMS & !permissionSMS) {
                    //checkPermission(posEnter);
                    grantResult(true, "android.permission.SEND_SMS", posEnter, true);
                }
                if (!showRationaleRead & !permissionReadContacts) {
                    grantResult(true, "android.permission.READ_CONTACTS", posEnter, true);
                    //checkPermission(posEnter);
                }
            }
        }

        if (exiting.isChecked() & (spinnerExit.getSelectedItemPosition() == 0)) {
            if (!permissionCall || !permissionReadContacts) {
                showRationaleCall = shouldShowRequestPermissionRationale("android.permission.CALL_PHONE");
                showRationaleRead = shouldShowRequestPermissionRationale("android.permission.READ_CONTACTS");
                if (!showRationaleCall & !permissionCall) {
                    //checkPermission(posExit);
                    grantResult(true, "android.permission.CALL_PHONE", posExit, true);
                } else if (!showRationaleRead & !permissionReadContacts) {
                    grantResult(true, "android.permission.READ_CONTACTS", posExit, true);
                    //checkPermission(posEnter);

                }
            }
        }

        if (exiting.isChecked() & (spinnerExit.getSelectedItemPosition() == 1)) {
            if (!permissionSMS || !permissionReadContacts) {
                showRationaleSMS = shouldShowRequestPermissionRationale("android.permission.SEND_SMS");
                showRationaleRead = shouldShowRequestPermissionRationale("android.permission.READ_CONTACTS");
                if (!showRationaleSMS & !permissionSMS) {
                    //checkPermission(posExit);
                    grantResult(true, "android.permission.SEND_SMS", posExit, true);
                }
                if (!showRationaleRead & !permissionReadContacts) {
                    grantResult(true, "android.permission.READ_CONTACTS", posExit, true);
                    //checkPermission(posEnter);
                }
            }
        }
    }

    private BroadcastReceiver CloseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "PlaceActivity закрытo");
            finish();
        }

    };

    public void snackbarCreate(final int pos) {
        snackbarPermission = Snackbar.make(findViewById(R.id.activity_place),
                R.string.access_permissions,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.turn_on,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (pos) {
                            case 0:
                                ActivityCompat.requestPermissions(PlaceActivity.this,
                                        new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS},
                                        REQUEST_PERMISSIONS_REQUEST_CODE);
                                break;
                            case 1:
                                ActivityCompat.requestPermissions(PlaceActivity.this,
                                        new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS},
                                        REQUEST_PERMISSIONS_REQUEST_CODE);
                                break;
                        }
                    }
                });
    }

    public void checkPermission(int pos) {
        switch (pos) {
            case 0:
                if (PackageManager.PERMISSION_DENIED == ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.CALL_PHONE) || PackageManager.PERMISSION_DENIED == ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CONTACTS)) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CALL_PHONE)) {
                        ActivityCompat.requestPermissions(PlaceActivity.this,
                                new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS},
                                REQUEST_PERMISSIONS_REQUEST_CODE);

                    } else {
                        ActivityCompat.requestPermissions(PlaceActivity.this,
                                new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS},
                                REQUEST_PERMISSIONS_REQUEST_CODE);
                    }
                }
                break;
            case 1:
                if (PackageManager.PERMISSION_DENIED == ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS) || PackageManager.PERMISSION_DENIED == ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CONTACTS)) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.SEND_SMS)) {
                        ActivityCompat.requestPermissions(PlaceActivity.this,
                                new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS},
                                REQUEST_PERMISSIONS_REQUEST_CODE);

                    } else {
                        ActivityCompat.requestPermissions(PlaceActivity.this,
                                new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS},
                                REQUEST_PERMISSIONS_REQUEST_CODE);
                    }

                }
                break;
            case 3:
            case 4:
                boolean permissionDNDaccess = mNotificationManager.isNotificationPolicyAccessGranted();
                if (!permissionDNDaccess) {
                    checkPermissionDND();
                }
                break;
        }
    }

    public void checkPermissionDND() {
        if (mNotificationManager != null) {
            if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                Intent dndIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                dndIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(dndIntent);
            }
        }
    }


    private boolean checkPermissions() {
        boolean permissionLocation = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        boolean permissionCall = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE);
        boolean permissionSMS = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS);
        boolean permissionReadContacts = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS);
        boolean permissionBackGroundLoc;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionBackGroundLoc = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        } else {
            permissionBackGroundLoc = true;
        }
        boolean permissionDNDaccess = mNotificationManager.isNotificationPolicyAccessGranted();
        return permissionCall && permissionLocation && permissionSMS &&
                permissionReadContacts && permissionDNDaccess && permissionBackGroundLoc;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        boolean shouldProvideCall =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CALL_PHONE);
        boolean shouldProvideSMS =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.SEND_SMS);
        boolean shouldProvideReadCont =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS);
        boolean shouldBackgroundLoc;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            shouldBackgroundLoc =
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        } else {
            shouldBackgroundLoc = true;
        }

        if (shouldProvideRationale || shouldProvideCall || shouldProvideSMS
                || shouldProvideReadCont || shouldBackgroundLoc || !mNotificationManager.isNotificationPolicyAccessGranted()) {
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            //startActivity(intent);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void grantResult(boolean grant, String permission, final int pos, boolean DND) {
        boolean showRationale;
        if (permission != null) {
            showRationale = shouldShowRequestPermissionRationale(permission);
        } else {
            showRationale = true;
        }
        //Log.d(TAG, permission + ", " + showRationale);
        if (grant) {
            if (!DND) {
                if (!snackbarDND.isShown()) snackbarDND.show();
            } else if (!showRationale) {
                if (!snackbarSettings.isShown()) snackbarSettings.show();
            } else {
                if (!snackbarPermission.isShown()) snackbarPermission.show();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0) {
                if (posEnter != -1 || posExit != -1) {

                    if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
                        switch (posEnter) {
                            case 0:
                            case 1:
                                Log.d(TAG, grantResults.length + "");
                                Log.d(TAG, permissions[0]);
                                Log.d(TAG, permissions[1]);
                                grantResult(grantResults[0] == PackageManager.PERMISSION_DENIED, permissions[0], posEnter, true);
                                grantResult(grantResults[1] == PackageManager.PERMISSION_DENIED, permissions[1], posEnter, true);
                                break;
                            case 3:
                            case 4:
                                grantResult(false, null, posEnter, mNotificationManager.isNotificationPolicyAccessGranted());
                                break;
                        }
                        switch (posExit) {
                            case 0:
                            case 1:
                                grantResult(grantResults[0] == PackageManager.PERMISSION_DENIED, permissions[0], posExit, true);
                                grantResult(grantResults[1] == PackageManager.PERMISSION_DENIED, permissions[1], posExit, true);
                                break;
                            case 3:
                            case 4:
                                grantResult(false, null, posExit, mNotificationManager.isNotificationPolicyAccessGranted());
                                break;

                        }
                    }
                }
            }
        }
    }
}