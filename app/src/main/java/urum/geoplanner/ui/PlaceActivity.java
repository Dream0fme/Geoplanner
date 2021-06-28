package urum.geoplanner.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.content.CursorLoader;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import urum.geoplanner.R;
import urum.geoplanner.databinding.ActivityPlaceBinding;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.service.ConnectorService;
import urum.geoplanner.utils.GeocoderAdapter;
import urum.geoplanner.viewmodel.ModelFactory;
import urum.geoplanner.viewmodel.PlaceViewModel;

import static urum.geoplanner.utils.Constants.*;
import static urum.geoplanner.utils.Utils.round;

public class PlaceActivity extends AppCompatActivity {
    private String PACKAGE_NAME;
    Geocoder geocoder;
    List<Address> addresses;

    String errorMessage;

    long placeId = 0;
    double lat = 0.0;
    double lng = 0.0;
    boolean fromMaps;
    boolean fromMain = false;
    String addressFromMaps = "";
    NotificationManager mNotificationManager;
    String addressFromDB = "";

    int posEnter = -1;
    int posExit = -1;
    Snackbar snackbarDND;
    Snackbar snackbarSettings;
    Snackbar snackbarPermission;

    private ConnectorService connectorService;

    private PlaceViewModel mPlaceViewModel;

    private ActivityPlaceBinding binding;

    @SuppressLint({"SimpleDateFormat", "SetTextI18n", "CutPasteId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        PACKAGE_NAME = this.getPackageName();

        initToolbar();

        connectorService = new ConnectorService(this);
        getLifecycle().addObserver(connectorService);

        snackbarDND = Snackbar.make(binding.activityPlace,
                R.string.access_for_dnd,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkPermissionDND();
                    }

                });

        snackbarSettings = Snackbar.make(binding.activityPlace,
                R.string.access_manual,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + PACKAGE_NAME)));
                    }
                });

        snackbarCreate(2);

        mPlaceViewModel = new ViewModelProvider(this,
                new ModelFactory(getApplication())).get(PlaceViewModel.class);

        registerReceiver(CloseReceiver, new IntentFilter(CLOSEAPPINTENTFILTER));
        mNotificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);


        String[] actions = getResources().getStringArray(R.array.actions);

        geocoder = new Geocoder(this, Locale.getDefault());
        addresses = null;

        binding.addressBox.setThreshold(1);
        binding.addressBox.setAdapter(new GeocoderAdapter(this, android.R.layout.simple_list_item_1));

        binding.conditionBox.setLabelFormatter(new LabelFormatter() {
            @SuppressLint("DefaultLocale")
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f м.", value);
            }
        });

        binding.entering.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    binding.relativeLayoutMain.addView(binding.relativeLayoutEntering);
                    ((RelativeLayout.LayoutParams) binding.exiting.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.relativeLayoutEntering.getId());
                    posEnter = binding.spinner.getSelectedItemPosition();

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
                    binding.relativeLayoutMain.removeView(binding.relativeLayoutEntering);
                    ((RelativeLayout.LayoutParams) binding.exiting.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.entering.getId());
                    if (snackbarDND.isShown()) {
                        if (binding.exiting.isChecked() & !(binding.spinnerExit.getSelectedItemPosition() == 3 || binding.spinnerExit.getSelectedItemPosition() == 4)) {
                            snackbarDND.dismiss();
                        } else if (!binding.exiting.isChecked()) {
                            snackbarDND.dismiss();
                        }
                    } else if (snackbarPermission.isShown()) {
                        if (binding.exiting.isChecked() & !(binding.spinnerExit.getSelectedItemPosition() == 0 || binding.spinnerExit.getSelectedItemPosition() == 1)) {
                            snackbarPermission.dismiss();
                        } else if (!binding.exiting.isChecked()) {
                            snackbarPermission.dismiss();
                        }
                    } else if (snackbarSettings.isShown()) {
                        if (binding.exiting.isChecked() & !(binding.spinnerExit.getSelectedItemPosition() == 0 || binding.spinnerExit.getSelectedItemPosition() == 1)) {
                            snackbarSettings.dismiss();
                        } else if (!binding.exiting.isChecked()) {
                            snackbarSettings.dismiss();
                        }
                    }
                }
            }
        });

        binding.exiting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    binding.relativeLayoutMain.addView(binding.relativeLayoutExiting);
                    posExit = binding.spinnerExit.getSelectedItemPosition();

                    if (binding.spinnerExit.getSelectedItemPosition() == 3 || binding.spinnerExit.getSelectedItemPosition() == 4) {
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
                    binding.relativeLayoutMain.removeView(binding.relativeLayoutExiting);
                    if (snackbarDND.isShown()) {
                        if (binding.entering.isChecked() & !(binding.spinner.getSelectedItemPosition() == 3 || binding.spinner.getSelectedItemPosition() == 4)) {
                            snackbarDND.dismiss();
                        } else if (!binding.entering.isChecked()) {
                            snackbarDND.dismiss();
                        }
                    } else if (snackbarPermission.isShown()) {
                        if (binding.entering.isChecked() & !(binding.spinner.getSelectedItemPosition() == 0 || binding.spinner.getSelectedItemPosition() == 1)) {
                            snackbarPermission.dismiss();
                        } else if (!binding.exiting.isChecked()) {
                            snackbarPermission.dismiss();
                        }
                    } else if (snackbarSettings.isShown()) {
                        if (binding.entering.isChecked() & !(binding.spinner.getSelectedItemPosition() == 0 || binding.spinner.getSelectedItemPosition() == 1)) {
                            snackbarSettings.dismiss();
                        } else if (!binding.exiting.isChecked()) {
                            snackbarSettings.dismiss();
                        }
                    }
                }
            }
        });

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, actions);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.spinner.setAdapter(adapterSpinner);
        binding.spinner.setPrompt(getString(R.string.choose_action));
        binding.spinner.setSelection(2, true);
        addOrRemoveView(2);
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                posEnter = position;
                addOrRemoveView(position);
                checkPermission(position);
                snackbarCreate(position);

                if (!(position == 0 || position == 1) &
                        !(binding.spinnerExit.getSelectedItemPosition() == 0 || binding.spinnerExit.getSelectedItemPosition() == 1)) {
                    if (snackbarPermission.isShown()) {
                        snackbarPermission.dismiss();
                    } else if (snackbarSettings.isShown()) {
                        snackbarSettings.dismiss();
                    }
                }


                if (position != 3 & !(binding.spinnerExit.getSelectedItemPosition() == 3 || binding.spinnerExit.getSelectedItemPosition() == 4)) {
                    if (snackbarDND.isShown()) {
                        snackbarDND.dismiss();
                    }
                }
                if (position != 4 & !(binding.spinnerExit.getSelectedItemPosition() == 3 || binding.spinnerExit.getSelectedItemPosition() == 4)) {

                    if (snackbarDND.isShown()) {
                        snackbarDND.dismiss();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        binding.spinnerExit.setAdapter(adapterSpinner);
        binding.spinnerExit.setAdapter(adapterSpinner);
        binding.spinnerExit.setPrompt(getString(R.string.choose_action));
        binding.spinnerExit.setSelection(2, true);
        addOrRemoveNewView(2);
        binding.spinnerExit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                posExit = position;
                addOrRemoveNewView(position);
                checkPermission(position);
                snackbarCreate(position);

                if (position != 0 & !(binding.spinner.getSelectedItemPosition() == 0)) {
                    if (snackbarPermission.isShown()) {
                        snackbarPermission.dismiss();
                    } else if (snackbarSettings.isShown()) {
                        snackbarSettings.dismiss();
                    }
                }

                if (position != 1 & !(binding.spinner.getSelectedItemPosition() == 1)) {
                    if (snackbarPermission.isShown()) {
                        snackbarPermission.dismiss();
                    } else if (snackbarSettings.isShown()) {
                        snackbarSettings.dismiss();
                    }
                }

                if (position != 3 & !(binding.spinner.getSelectedItemPosition() == 3 || binding.spinner.getSelectedItemPosition() == 4)) {
                    if (snackbarDND.isShown()) {
                        snackbarDND.dismiss();
                    }
                }
                if (position != 4 & !(binding.spinner.getSelectedItemPosition() == 3 || binding.spinner.getSelectedItemPosition() == 4)) {
                    if (snackbarDND.isShown()) {
                        snackbarDND.dismiss();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getBoolean(FROM_NOTIFICATION_TO_PLACEACTIVITY)) {
                placeId = extras.getLong(ID_FROM_NOTIFICATION, 0);
            } else placeId = extras.getLong(ID);

            fromMaps = extras.getBoolean(FROM_MAPS);
            addressFromMaps = extras.getString(ADDRESS_FROM_MAPS, "");
            fromMain = extras.getBoolean(FROM_MAIN);
        }

        if (placeId > 0) { // если 0, то добавление
            Place place = mPlaceViewModel.getPlace(placeId);
            binding.setPlace(place);
            binding.conditionBox.setValue(place.getCondition());

            Pattern patternGEO = Pattern.compile("^(\\-?\\d+(\\.\\d+)?),\\s*(\\-?\\d+(\\.\\d+)?)$");
            Matcher geoMatcher = patternGEO.matcher(place.getAddress());
            if (!geoMatcher.matches()) {
                binding.addressBox.setText(place.getAddress());
                addressFromDB = place.getAddress();
                if (!binding.addressBox.isEnabled()) {
                    binding.addressBox.setEnabled(true);
                }
            } else {
                binding.addressBox.setText(round(place.getLatitude(), 5) + ", " + round(place.getLongitude(), 5));
                if (binding.addressBox.isEnabled()) {
                    binding.addressBox.setEnabled(false);
                }
            }

            if (place.getCheckboxEnter() == 1) {
                binding.entering.setChecked(true);
                binding.spinner.setSelection(place.getPosition(), true);
                addOrRemoveView(place.getPosition());
            } else if (place.getCheckboxEnter() == 0) {
                binding.entering.setChecked(false);
            }

            if (place.getCheckboxExit() == 1) {
                binding.exiting.setChecked(true);
                binding.spinnerExit.setSelection(place.getPositionExit(), true);
                addOrRemoveNewView(place.getPositionExit());
            } else if (place.getCheckboxExit() == 0) {
                binding.exiting.setChecked(false);
            }
            lat = place.getLatitude();
            lng = place.getLongitude();

        } else {
            binding.entering.setChecked(false);
            binding.exiting.setChecked(false);
            if (extras != null) {
                if (fromMaps) {
                    if (addressFromMaps.equals("")) {
                        //linearLayout.removeView(addressLayout);
                        //linearLayout.removeView(btnGoogleMap);
                        //((RelativeLayout.LayoutParams) radiusBox.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.nameBoxInput);
                        lat = extras.getDouble("latFromMaps");
                        lng = extras.getDouble("lngFromMaps");
                        binding.addressBox.setText(round(lat, 5) + ", " + round(lng, 5));
                        if (binding.addressBox.isEnabled()) {
                            binding.addressBox.setEnabled(false);
                        }

                        //Log.i("mytag", "lat" + lat + " lng: " + lng);
                    } else {
                        binding.addressBox.setText(addressFromMaps);
                        lat = extras.getDouble("latFromMaps");
                        lng = extras.getDouble("lngFromMaps");
                        if (!binding.addressBox.isEnabled()) {
                            binding.addressBox.setEnabled(true);
                        }
                    }
                }
            }
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
        setSupportActionBar(binding.toolbarPlace);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbarPlace.setTitleTextColor(getResources().getColor(android.R.color.white));
    }

    public void addOrRemoveView(int pos) {
        switch (pos) {
            case 0:
                if (findViewById(R.id.numberBoxInput) == null) {
                    binding.relativeLayoutEntering.addView(binding.numberBoxInput);
                    binding.relativeLayoutEntering.addView(binding.btnNumberEnter);
                }
                if (findViewById(R.id.smsBoxInput) != null) {
                    binding.relativeLayoutEntering.removeView(binding.smsBoxInput);
                }
                break;
            case 1:
                if (findViewById(R.id.numberBoxInput) == null) {
                    binding.relativeLayoutEntering.addView(binding.numberBoxInput);
                    binding.relativeLayoutEntering.addView(binding.btnNumberEnter);
                }
                if (findViewById(R.id.smsBoxInput) == null) {
                    ((RelativeLayout.LayoutParams) binding.smsBoxInput.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.numberBoxInput.getId());
                    binding.relativeLayoutEntering.addView(binding.smsBoxInput);
                    binding.smsBoxInput.setCounterMaxLength(160);
                    binding.smsBoxInput.setHint(getString(R.string.input_sms));
                } else {
                    ((RelativeLayout.LayoutParams) binding.smsBoxInput.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.numberBoxInput.getId());
                    binding.smsBoxInput.setCounterMaxLength(160);
                    binding.smsBoxInput.setHint(getString(R.string.input_sms));
                }
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                if (findViewById(R.id.numberBoxInput) != null) {
                    binding.relativeLayoutEntering.removeView(binding.numberBoxInput);
                    binding.relativeLayoutEntering.removeView(binding.btnNumberEnter);
                }
                if (findViewById(R.id.smsBoxInput) != null) {
                    binding.relativeLayoutEntering.removeView(binding.smsBoxInput);
                }
                break;
            case 2:
                if (findViewById(R.id.numberBoxInput) != null) {
                    binding.relativeLayoutEntering.removeView(binding.numberBoxInput);
                    binding.relativeLayoutEntering.removeView(binding.btnNumberEnter);
                }
                if (findViewById(R.id.smsBoxInput) == null) {
                    binding.relativeLayoutEntering.addView(binding.smsBoxInput);
                    ((RelativeLayout.LayoutParams) binding.smsBoxInput.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.spinner.getId());
                    binding.smsBoxInput.setCounterMaxLength(300);
                    binding.smsBoxInput.setHint(getString(R.string.input_notification));
                } else {
                    ((RelativeLayout.LayoutParams) binding.smsBoxInput.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.spinner.getId());
                    binding.smsBoxInput.setCounterMaxLength(300);
                    binding.smsBoxInput.setHint(getString(R.string.input_notification));
                }
                break;

        }
    }

    public void addOrRemoveNewView(int pos) {
        switch (pos) {
            case 0:
                if (findViewById(R.id.numberExitBoxInput) == null) {
                    binding.relativeLayoutExiting.addView(binding.numberExitBoxInput);
                    binding.relativeLayoutExiting.addView(binding.btnNumberExit);
                }
                if (findViewById(R.id.smsExitBoxInput) != null) {
                    binding.relativeLayoutExiting.removeView(binding.smsExitBoxInput);
                }
                break;
            case 1:
                if (findViewById(R.id.numberExitBoxInput) == null) {
                    binding.relativeLayoutExiting.addView(binding.numberExitBoxInput);
                    binding.relativeLayoutExiting.addView(binding.btnNumberExit);
                }
                if (findViewById(R.id.smsExitBoxInput) == null) {
                    binding.relativeLayoutExiting.addView(binding.smsExitBoxInput);
                    ((RelativeLayout.LayoutParams) binding.smsExitBoxInput.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.numberExitBoxInput.getId());
                    binding.smsExitBoxInput.setCounterMaxLength(160);
                    binding.smsExitBoxInput.setHint(getString(R.string.input_sms));
                } else {
                    ((RelativeLayout.LayoutParams) binding.smsExitBoxInput.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.numberExitBoxInput.getId());
                    binding.smsExitBoxInput.setCounterMaxLength(160);
                    binding.smsExitBoxInput.setHint(getString(R.string.input_sms));
                }
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                if (findViewById(R.id.numberExitBoxInput) != null) {
                    binding.relativeLayoutExiting.removeView(binding.numberExitBoxInput);
                    binding.relativeLayoutExiting.removeView(binding.btnNumberExit);
                }
                if (findViewById(R.id.smsExitBoxInput) != null) {
                    binding.relativeLayoutExiting.removeView(binding.smsExitBoxInput);
                }
                break;
            case 2:
                if (findViewById(R.id.numberExitBoxInput) != null) {
                    binding.relativeLayoutExiting.removeView(binding.numberExitBoxInput);
                    binding.relativeLayoutExiting.removeView(binding.btnNumberExit);
                }
                if (findViewById(R.id.smsExitBoxInput) == null) {
                    binding.relativeLayoutExiting.addView(binding.smsExitBoxInput);
                    ((RelativeLayout.LayoutParams) binding.smsExitBoxInput.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.spinnerExit.getId());
                    binding.smsExitBoxInput.setCounterMaxLength(300);
                    binding.smsExitBoxInput.setHint(getString(R.string.input_notification));
                } else {
                    ((RelativeLayout.LayoutParams) binding.smsExitBoxInput.getLayoutParams()).addRule(RelativeLayout.BELOW, binding.spinnerExit.getId());
                    binding.smsExitBoxInput.setCounterMaxLength(300);
                    binding.smsExitBoxInput.setHint(getString(R.string.input_notification));
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
        if (setValidation()) {
            try {
                int checkBoxEnter, checkBoxExit;
                if (binding.entering.isChecked()) {
                    checkBoxEnter = 1;
                } else checkBoxEnter = 0;

                if (binding.exiting.isChecked()) {
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
                if (binding.addressBox.isEnabled()) {
                    addressEdit = binding.addressBox.getText().toString().trim();
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

                        namePlace = binding.nameBox.getText().toString().trim();
                        condition = (int) binding.conditionBox.getValue();
                        phone = binding.number.getText().toString().trim();
                        msg = binding.sms.getText().toString().trim();
                        int position = binding.spinner.getSelectedItemPosition();
                        int positionExit = binding.spinnerExit.getSelectedItemPosition();
                        phoneExit = binding.numberExit.getText().toString().trim();
                        msgExit = binding.smsExit.getText().toString().trim();
                        addressName = address;
                        if (!fromMaps) {
                            if (lat == 0.0 & lng == 0.0) {
                                lat = addressName.getLatitude();
                                lng = addressName.getLongitude();
                            } else if (!addressFromDB.equalsIgnoreCase(binding.addressBox.getText().toString())) {
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
                    namePlace = binding.nameBox.getText().toString().trim();
                    condition = (int) binding.conditionBox.getValue();
                    phone = binding.number.getText().toString().trim();
                    msg = binding.sms.getText().toString().trim();
                    int position = binding.spinner.getSelectedItemPosition();
                    int positionExit = binding.spinnerExit.getSelectedItemPosition();
                    phoneExit = binding.numberExit.getText().toString().trim();
                    msgExit = binding.smsExit.getText().toString().trim();
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

    public boolean setValidation() {
        boolean nameValid, addressValid,
                numberValid, smsValid, numberExitValid, smsExitValid;

        if (binding.nameBox.getText().toString().trim().length() == 0) {
            binding.nameBoxInput.setError("Введите название места!");
            nameValid = false;
        } else if (binding.nameBox.getText().toString().trim().length() > 25) {
            binding.nameBoxInput.setError("Слишком длинное название!");
            nameValid = false;
        } else {
            binding.nameBoxInput.setErrorEnabled(false);
            nameValid = true;
        }


        if (findViewById(R.id.addressBox) != null & binding.addressBox.getText().toString().equals("")) {
            binding.addressBoxInput.setError("Введите адрес!");
            addressValid = false;
        } else {
            binding.addressBoxInput.setErrorEnabled(false);
            addressValid = true;
        }

        if (findViewById(R.id.numberBoxInput) != null & binding.number.getText().toString().equals("")) {
            binding.numberBoxInput.setError("Введите номер!");
            numberValid = false;
        } else if (findViewById(R.id.numberBoxInput) != null & !validatePhoneNumber(binding.number.getText().toString())) {
            binding.numberBoxInput.setError("Номер введён неправильно!");
            numberValid = false;

        } else {
            binding.numberBoxInput.setErrorEnabled(false);
            numberValid = true;
        }


        if (findViewById(R.id.smsBoxInput) != null & binding.spinner.getSelectedItemPosition() == 1 & binding.sms.getText().toString().equals("")) {
            binding.smsBoxInput.setError("Введите ваше сообщение!");
            smsValid = false;
        } else if (findViewById(R.id.smsBoxInput) != null & binding.spinner.getSelectedItemPosition() == 2 & binding.sms.getText().toString().equals("")) {
            binding.smsBoxInput.setError("Введите ваше уведомление!");
            smsValid = false;
        } else if (findViewById(R.id.smsBoxInput) != null & binding.spinner.getSelectedItemPosition() == 2 & binding.sms.getText().length() > 300) {
            binding.smsBoxInput.setError("Слишком длинное уведомление!");
            smsValid = false;
        } else if (findViewById(R.id.smsBoxInput) != null & binding.spinner.getSelectedItemPosition() == 1 & binding.sms.getText().length() > 160) {
            binding.smsBoxInput.setError("Слишком длинное сообщение!");
            smsValid = false;
        } else {
            binding.smsBoxInput.setErrorEnabled(false);
            smsValid = true;
        }


        if (findViewById(R.id.numberExitBoxInput) != null & binding.numberExit.getText().toString().equals("")) {
            binding.numberExitBoxInput.setError("Введите номер!");
            numberExitValid = false;
        } else if (findViewById(R.id.numberExitBoxInput) != null & !validatePhoneNumber(binding.numberExit.getText().toString())) {
            binding.numberExitBoxInput.setError("Номер введён неправильно!");
            numberExitValid = false;
        } else {
            binding.numberExitBoxInput.setErrorEnabled(false);
            numberExitValid = true;
        }

        if (findViewById(R.id.smsExitBoxInput) != null & binding.spinnerExit.getSelectedItemPosition() == 1 & binding.smsExit.getText().toString().equals("")) {
            binding.smsExitBoxInput.setError("Введите ваше сообщение!");
            smsExitValid = false;
        } else if (findViewById(R.id.smsExitBoxInput) != null & binding.spinnerExit.getSelectedItemPosition() == 2 & binding.smsExit.getText().toString().equals("")) {
            binding.smsExitBoxInput.setError("Введите ваше уведомление!");
            smsExitValid = false;
        } else if (findViewById(R.id.smsExitBoxInput) != null & binding.spinnerExit.getSelectedItemPosition() == 2 & binding.smsExit.getText().length() > 300) {
            binding.smsExitBoxInput.setError("Слишком длинное уведомление!");
            smsExitValid = false;
        } else if (findViewById(R.id.smsExitBoxInput) != null & binding.spinnerExit.getSelectedItemPosition() == 1 & binding.smsExit.getText().length() > 160) {
            binding.smsExitBoxInput.setError("Слишком длинное сообщение!");
            smsExitValid = false;
        } else {
            binding.smsExitBoxInput.setErrorEnabled(false);
            smsExitValid = true;
        }

        return nameValid & addressValid & numberValid & smsValid & numberExitValid & smsExitValid;
    }

    public void openGoogleMap(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(FROM_ACTIVITY_PLACE, true);
        Pattern patternGEO = Pattern.compile("^(\\-?\\d+(\\.\\d+)?),\\s*(\\-?\\d+(\\.\\d+)?)$");
        Matcher geoMatcher = patternGEO.matcher(binding.addressBox.getText().toString());
        if (!binding.addressBox.getText().toString().equals("") && !addressFromDB.equalsIgnoreCase(binding.addressBox.getText().toString()) && !geoMatcher.matches()) {
            try {
                String addressEdit = binding.addressBox.getText().toString().trim();
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
                    intent.putExtra(LATITUDE_FROM_PLACEACTIVITY, address.getLatitude());
                    intent.putExtra(LONGITUDE_FROM_PLACEACTIVITY, address.getLongitude());
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
            intent.putExtra(LATITUDE_FROM_PLACEACTIVITY, lat);
            intent.putExtra(LONGITUDE_FROM_PLACEACTIVITY, lng);
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
                fromMaps = i.getBooleanExtra(FROM_MAPS, false);
                addressFromMaps = i.getStringExtra(ADDRESS_FROM_MAPS);
                Log.d(TAG, addressFromMaps);
                if (fromMaps) {
                    if (addressFromMaps.equals("")) {
                        //linearLayout.removeView(addressLayout);
                        //linearLayout.removeView(btnGoogleMap);
                        //((RelativeLayout.LayoutParams) radiusBox.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.nameBoxInput);
                        lat = i.getDoubleExtra(LATITUDE_FROM_MAPS, 0.0);
                        lng = i.getDoubleExtra(LONGITUDE_FROM_MAPS, 0.0);
                        binding.addressBox.setText(round(lat, 5) + ", " + round(lng, 5));
                        if (binding.addressBox.isEnabled()) {
                            binding.addressBox.setEnabled(false);
                        }
                    } else {
                        binding.addressBox.setText(addressFromMaps);
                        lat = i.getDoubleExtra(LATITUDE_FROM_MAPS, 0.0);
                        lng = i.getDoubleExtra(LONGITUDE_FROM_MAPS, 0.0);
                        if (!binding.addressBox.isEnabled()) {
                            binding.addressBox.setEnabled(true);
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
                            binding.number.setText(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
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
                            binding.numberExit.setText(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
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
            if (binding.entering.isChecked() & (binding.spinner.getSelectedItemPosition() == 3 || binding.spinner.getSelectedItemPosition() == 4)) {
                grantResult(true, null, posEnter, mNotificationManager.isNotificationPolicyAccessGranted());
            }
            if (binding.exiting.isChecked() & (binding.spinnerExit.getSelectedItemPosition() == 3 || binding.spinnerExit.getSelectedItemPosition() == 4)) {
                grantResult(true, null, posExit, mNotificationManager.isNotificationPolicyAccessGranted());
            }
        }


        if (binding.entering.isChecked() & binding.spinner.getSelectedItemPosition() == 0) {
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

        if (binding.entering.isChecked() & binding.spinner.getSelectedItemPosition() == 1) {
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

        if (binding.exiting.isChecked() & (binding.spinnerExit.getSelectedItemPosition() == 0)) {
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

        if (binding.exiting.isChecked() & (binding.spinnerExit.getSelectedItemPosition() == 1)) {
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
        snackbarPermission = Snackbar.make(binding.activityPlace,
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