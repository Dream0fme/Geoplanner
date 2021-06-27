package urum.geoplanner.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import urum.geoplanner.R;
import urum.geoplanner.databinding.FragmentMapBinding;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.ui.MainActivity;
import urum.geoplanner.ui.PlaceActivity;
import urum.geoplanner.utils.GeocoderAdapter;
import urum.geoplanner.utils.NetworkChecker;
import urum.geoplanner.viewmodel.ModelFactory;
import urum.geoplanner.viewmodel.PlaceViewModel;

import static android.content.Context.LOCATION_SERVICE;
import static urum.geoplanner.utils.Constants.ADDRESS_FROM_MAPS;
import static urum.geoplanner.utils.Constants.FROM_ACTIVITY_PLACE;
import static urum.geoplanner.utils.Constants.FROM_MAPS;
import static urum.geoplanner.utils.Constants.ID;
import static urum.geoplanner.utils.Constants.LATITUDE_FROM_MAPS;
import static urum.geoplanner.utils.Constants.LATITUDE_FROM_PLACEACTIVITY;
import static urum.geoplanner.utils.Constants.LONGITUDE_FROM_MAPS;
import static urum.geoplanner.utils.Constants.LONGITUDE_FROM_PLACEACTIVITY;
import static urum.geoplanner.utils.Constants.TAG;
import static urum.geoplanner.utils.Utils.enableLayout;
import static urum.geoplanner.utils.Utils.findNavController;
import static urum.geoplanner.utils.Utils.getLastLocation;
import static urum.geoplanner.utils.Utils.round;


public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMyLocationClickListener, View.OnClickListener {

    FragmentMapBinding binding;
    boolean addressNotFound;

    private boolean fromActivityPlace = false;

    private GoogleMap mMap;
    Geocoder geocoder;
    private LatLng addPoint;

    LocationManager lm;

    private HashMap<Marker, Long> mHashMap = new HashMap<Marker, Long>();
    double lat = 0.0;
    double lng = 0.0;
    boolean startAnimation = false;
    Snackbar snackbarInternet;

    private PlaceViewModel mPlaceViewModel;

    public MapView mapView;

    MainActivity mainActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mainActivity = (MainActivity) getActivity();

        final Intent intent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);

        snackbarInternet = Snackbar.make(requireActivity().findViewById(R.id.container),
                R.string.internet_no_connection,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.settingsInternet,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    }
                });

        lm = (LocationManager) requireActivity().getSystemService(LOCATION_SERVICE);
        geocoder = new Geocoder(requireContext(), Locale.getDefault());
        mPlaceViewModel = new ViewModelProvider(this,
                new ModelFactory(getActivity().getApplication())).get(PlaceViewModel.class);

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (!startAnimation) {
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStackImmediate();
                    } else {
                        requireActivity().onBackPressed();
                    }
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_map, container, false);

        //binding.setViewmodel(mViewModel);
        binding.setLifecycleOwner(this);

        binding.addressBoxGM.setHint(null);
        binding.autoCompleteTextView.setHint(getString(R.string.input_address));
        binding.autoCompleteTextView.setThreshold(1);
        binding.autoCompleteTextView.setAdapter(new GeocoderAdapter(requireActivity(), android.R.layout.simple_list_item_1));

        try {
            mapView = binding.mapView;

            mapView.onCreate(savedInstanceState);

            mapView.getMapAsync(this);
            MapsInitializer.initialize(this.getActivity());
        } catch (Exception e) {
            System.out.println(e);
        }

        @SuppressLint("ResourceType") View zoomControls = mapView.findViewById(0x1);
        snackbarInternet.setAnchorView(zoomControls);

        binding.googleMapBtn.setOnClickListener(this);
        return binding.getRoot();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);

        mPlaceViewModel.getPlacesFromPlaces().observe(this, new Observer<List<Place>>() {
            @Override
            public void onChanged(@Nullable List<Place> places) {
                try {
                    if (!places.isEmpty()) {
                        for (int i = 0; i < places.size(); i++) {
                            String namePlace = places.get(i).getName();
                            long id = places.get(i).getId();
                            double lat = places.get(i).getLatitude();
                            double lon = places.get(i).getLongitude();
                            LatLng pointPlace = new LatLng(lat, lon);
                            int radius = places.get(i).getCondition();

                            StringBuilder infoMarker = createInfoMarker(places, i);
                            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                                @Override
                                public View getInfoWindow(Marker arg0) {
                                    return null;
                                }

                                @Override
                                public View getInfoContents(Marker marker) {
                                    Context context = getContext();
                                    LinearLayout linearLayout = new LinearLayout(context);
                                    linearLayout.setOrientation(LinearLayout.VERTICAL);

                                    TextView title = new TextView(context);
                                    title.setTextColor(Color.BLACK);
                                    title.setTextSize(20);
                                    title.setGravity(Gravity.CENTER);
                                    title.setTypeface(null, Typeface.BOLD);
                                    title.setText(marker.getTitle());

                                    TextView snippet = new TextView(context);
                                    snippet.setTextColor(Color.DKGRAY);
                                    snippet.setTypeface(null, Typeface.ITALIC);
                                    snippet.setText(marker.getSnippet());

                                    linearLayout.addView(title);
                                    linearLayout.addView(snippet);
                                    return linearLayout;
                                }
                            });

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(pointPlace)
                                    .snippet(infoMarker.toString())
                                    .title(namePlace));
                            mHashMap.put(marker, id);

                            mMap.addCircle(new CircleOptions()
                                    .center(pointPlace)
                                    .radius(radius)
                                    .fillColor(0x150000FF)
                                    .strokeColor(Color.BLUE)
                                    .strokeWidth(1));
                        }
                    }
                } catch (Exception e) {
                    Log.e("mytag", "" + e.getMessage());
                }
            }
        });
        showCurrentLocationOnMap();
        mMap.setOnMapClickListener(this);
        if (fromActivityPlace & (lat != 0.0 & lng != 0.0)) {
            LatLng myLocation = new LatLng(lat, lng);
            //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));
        } else {
            LatLng myLocation = getLastLocation(requireActivity());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));
        }
        //mMap.setOnMapLongClickListener(this);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
    }

    @SuppressWarnings("MissingPermission")
    private void showCurrentLocationOnMap() {
        try {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.setOnMyLocationButtonClickListener(this);
                mMap.setOnMyLocationClickListener(this);
            }
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage() + "");
        }
    }

    public void searchAddress(View v) {
        if (binding.autoCompleteTextView.getText().toString().trim().length() == 0) {
            Toast.makeText(requireContext(), "Введите адрес!", Toast.LENGTH_LONG).show();
        } else {
            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocationName(binding.autoCompleteTextView.getText().toString().trim(), 5);
                if (addresses == null || addresses.size() == 0) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(requireContext());
                    adb.setTitle("Google Map Search");
                    adb.setMessage("Пожалуйста, введите действительный адрес.");
                    adb.setPositiveButton("Ок", null);
                    adb.setCancelable(true);
                    adb.create();
                    adb.show();
                    binding.autoCompleteTextView.setText("");
                } else {
                    Address address = addresses.get(0);
                    for (int i = 0; i < addresses.size(); i++) {
                        if (addresses.size() == 1) {
                            break;
                        } else {
                            if (address.getLocality() != null || address.getThoroughfare() != null || address.getFeatureName() != null) {
                                address = addresses.get(i);
                                break;
                            } else {
                                i++;
                            }
                        }
                    }
                    LatLng searchLocation = new LatLng(address.getLatitude(), address.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(searchLocation));
                    CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                            searchLocation, 18);
                    mMap.animateCamera(location);
                }
            } catch (IOException ioException) {
                Log.d(TAG, "Сервис не работает", ioException);
                Toast.makeText(requireContext(), "Сервис не работает", Toast.LENGTH_LONG).show();

            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Используется неверная широта или долгота", e);
                Toast.makeText(requireContext(), "Используется неверная широта или долгота", Toast.LENGTH_LONG).show();
            }
        }
    }

    // лучше это не смотреть...
    public StringBuilder createInfoMarker(List<Place> places, int i) {
        StringBuilder infoMarker = new StringBuilder();
        StringBuilder info = new StringBuilder();
        StringBuilder infoExit = new StringBuilder();

        double lat = places.get(i).getLatitude();
        double lng = places.get(i).getLongitude();

        int radius = places.get(i).getCondition();

        int pos = places.get(i).getPosition();
        int posExit = places.get(i).getPositionExit();

        String number = places.get(i).getNumber();
        String numberExit = places.get(i).getNumberExit();
        String sms = places.get(i).getSms();
        String smsExit = places.get(i).getSmsExit();

        if (places.get(i).getCheckboxEnter() == 1) {
            switch (pos) {
                case 0:
                    info.append(getString(R.string.call))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.entry))
                            .append(getString(R.string.blank_space))
                            .append(number);
                    break;
                case 1:
                    info.append(getString(R.string.sms))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.entry))
                            .append(getString(R.string.blank_space))
                            .append(number)
                            .append(".")
                            .append(System.lineSeparator())
                            .append(sms);
                    break;
                case 2:
                    info.append(getString(R.string.notification))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.entry))
                            .append(getString(R.string.blank_space))
                            .append(System.lineSeparator())
                            .append(sms);
                    break;
                case 3:
                    info.append(getString(R.string.dnd_on))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.entry));
                    break;
                case 4:
                    info.append(getString(R.string.dnd_off))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.entry));
                    break;
                case 5:
                    info.append(getString(R.string.wifi_on))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.entry));
                    break;
                case 6:
                    info.append(getString(R.string.wifi_off))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.entry));
                    break;
                default:
                    break;
            }
        }

        if (places.get(i).getCheckboxExit() == 1) {
            switch (posExit) {
                case 0:
                    infoExit.append(getString(R.string.call))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.exit))
                            .append(getString(R.string.blank_space))
                            .append(numberExit);
                    break;
                case 1:
                    infoExit.append(getString(R.string.sms))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.exit))
                            .append(getString(R.string.blank_space))
                            .append(numberExit)
                            .append(".")
                            .append(System.lineSeparator())
                            .append(smsExit);
                    break;
                case 2:
                    infoExit.append(getString(R.string.notification))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.exit))
                            .append(getString(R.string.blank_space))
                            .append(System.lineSeparator())
                            .append(smsExit);
                    break;
                case 3:
                    infoExit.append(getString(R.string.dnd_on))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.exit));
                    break;
                case 4:
                    infoExit.append(getString(R.string.dnd_off))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.exit));
                    break;
                case 5:
                    infoExit.append(getString(R.string.wifi_on))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.exit));
                    break;
                case 6:
                    infoExit.append(getString(R.string.wifi_off))
                            .append(getString(R.string.blank_space))
                            .append(getString(R.string.exit));
                    break;
                default:
                    break;
            }
        }

        int lastInt = radius % 10;

        if (places.get(i).getCheckboxEnter() == 1 & places.get(i).getCheckboxExit() == 1) {
            if (lastInt > 1 & lastInt < 5) {
                infoMarker.append(info)
                        .append(System.lineSeparator())
                        .append(infoExit)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meters1));
            } else if (lastInt == 1) {
                infoMarker.append(info)
                        .append(System.lineSeparator())
                        .append(infoExit)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meter));
            } else {
                infoMarker.append(info)
                        .append(System.lineSeparator())
                        .append(infoExit)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meters2));
            }
        } else if (places.get(i).getCheckboxEnter() == 1 & places.get(i).getCheckboxExit() == 0) {
            if (lastInt > 1 & lastInt < 5) {
                infoMarker.append(info)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meters1));
            } else if (lastInt == 1) {
                infoMarker.append(info)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meter));
            } else {
                infoMarker.append(info)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meters2));
            }
        } else if (places.get(i).getCheckboxEnter() == 0 & places.get(i).getCheckboxExit() == 1) {
            if (lastInt > 1 & lastInt < 5) {
                infoMarker.append(infoExit)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meters1));
            } else if (lastInt == 1) {
                infoMarker.append(infoExit)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meter));
            } else {
                infoMarker.append(infoExit)
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meters2));
            }
        }

        if (places.get(i).getCheckboxExit() == 0 & places.get(i).getCheckboxEnter() == 0) {
            if (lastInt > 1 & lastInt < 5) {
                infoMarker.append(getString(R.string.actions_not_chosen))
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meters1));
            } else if (lastInt == 1) {
                infoMarker.append(getString(R.string.actions_not_chosen))
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meter));
            } else {
                infoMarker.append(getString(R.string.actions_not_chosen))
                        .append(System.lineSeparator())
                        .append(getString(R.string.radius))
                        .append(getString(R.string.blank_space))
                        .append(radius)
                        .append(getString(R.string.blank_space))
                        .append(getString(R.string.meters2));
            }
        }
        return infoMarker;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        try {
            LatLng addPoint = new LatLng(location.getLatitude(), location.getLongitude());
            Log.i("mytag", "lat" + addPoint.latitude + "lng: " + addPoint.longitude);

            List<Address> addresses;
            addresses = geocoder.getFromLocation(addPoint.latitude, addPoint.longitude, 5);
            if (addresses == null || addresses.size() == 0) {
                Log.d(TAG, "Адрес не найден. Попробуйте ещё раз");
                Toast.makeText(requireContext(), "Адрес не найден. Попробуйте ещё раз.", Toast.LENGTH_LONG).show();
            } else {
                Address address = addresses.get(0);
                for (int i = 0; i < addresses.size(); i++) {
                    if (addresses.size() == 1) {
                        break;
                    } else {
                        if (address.getLocality() != null || address.getThoroughfare() != null || address.getFeatureName() != null) {
                            address = addresses.get(i);
                            break;
                        } else {
                            i++;
                        }
                    }
                }
                final StringBuilder addressLine = new StringBuilder();
                boolean addressNotFound;
                if (address.getLocality() == null || address.getThoroughfare() == null || address.getFeatureName() == null) {
                    addressLine.append(round(addPoint.latitude, 5))
                            .append(",")
                            .append(getString(R.string.blank_space))
                            .append(round(addPoint.longitude, 5));
                    addressNotFound = true;
                } else {
                    addressLine.append(address.getLocality())
                            .append(",")
                            .append(getString(R.string.blank_space))
                            .append(address.getThoroughfare())
                            .append(",")
                            .append(getString(R.string.blank_space))
                            .append(address.getFeatureName());
                    addressNotFound = false;
                }
                StringBuilder message = new StringBuilder();
                if (addressNotFound) {
                    message.append(getString(R.string.our_geoposition))
                            .append(getString(R.string.blank_space))
                            .append(addressLine);
                } else {
                    message.append(getString(R.string.our_address))
                            .append(getString(R.string.blank_space))
                            .append(addressLine);
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

            }
        } catch (IOException ioException) {
            Log.d(TAG, "Сервис не работает", ioException);
            Toast.makeText(requireContext(), "Сервис не работает", Toast.LENGTH_LONG).show();

        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Используется неверная широта или долгота", e);
            Toast.makeText(requireContext(), "Используется неверная широта или долгота", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        try {
            addPoint = point;
            List<Address> addresses;
            addresses = geocoder.getFromLocation(addPoint.latitude, addPoint.longitude, 5);
            if (addresses == null || addresses.size() == 0) {
                Toast.makeText(getContext(), "Адрес не найден. Попробуйте ещё раз.", Toast.LENGTH_LONG).show();
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

                final StringBuilder addressLine = new StringBuilder();
                if (address.getLocality() == null || address.getThoroughfare() == null || address.getFeatureName() == null) {
                    addressLine.append(round(addPoint.latitude, 5))
                            .append(",")
                            .append(getString(R.string.blank_space))
                            .append(round(addPoint.longitude, 5));
                    addressNotFound = true;
                } else {
                    addressLine.append(address.getLocality())
                            .append(",")
                            .append(getString(R.string.blank_space))
                            .append(address.getThoroughfare())
                            .append(",")
                            .append(getString(R.string.blank_space))
                            .append(address.getFeatureName());
                    addressNotFound = false;
                }

                final Marker markerTouch = mMap.addMarker(new MarkerOptions()
                        .position(addPoint));
                mHashMap.put(markerTouch, (long) -1);
                dropPinEffect(markerTouch, addressLine, address);
            }
        } catch (IOException ioException) {
            Log.d(TAG, "Сервис не работает", ioException);
            Toast.makeText(getContext(), "Сервис не работает", Toast.LENGTH_LONG).show();

        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Используется неверная широта или долгота", e);
            Toast.makeText(getContext(), "Используется неверная широта или долгота", Toast.LENGTH_LONG).show();
        }
    }

    private void dropPinEffect(final Marker markerDrop, final StringBuilder addressLine,
                               final Address address) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 1200;

        final android.view.animation.Interpolator interpolator =
                new BounceInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                startAnimation = true;
                enableLayout(mainActivity.binding.navView, !startAnimation);

                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(
                        1 - interpolator.getInterpolation((float) elapsed
                                / duration), 0);
                markerDrop.setAnchor(0.2f, 0.5f + 5 * t);
                if (t > 0.0) {
                    handler.postDelayed(this, 15);
                } else {
                    startAnimation = false;
                    enableLayout(mainActivity.binding.navView, !startAnimation);
                    showDialog(markerDrop, addressLine, address);
                }
            }
        });
    }

    public void showDialog(final Marker markerdrop, final StringBuilder addressLine, final Address address) {
        StringBuilder message = new StringBuilder();
        if (addressNotFound) {
            message.append(getString(R.string.address_not_found))
                    .append(getString(R.string.blank_space))
                    .append(System.lineSeparator())
                    .append(getString(R.string.add_geo))
                    .append(getString(R.string.blank_space))
                    .append(System.lineSeparator())
                    .append(addressLine).append("?");
        } else {
            message.append(getString(R.string.add_address))
                    .append(getString(R.string.blank_space))
                    .append(System.lineSeparator())
                    .append(addressLine).append("?");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.add_address_title));
        builder.setMessage(message);//Html.fromHtml("<font color='#FF7F27'>"+getString(R.string.add)+"</font>")
        builder.setPositiveButton(getString(R.string.add), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                boolean fromMaps = true;
                if (fromActivityPlace) {
                    Intent intent = new Intent(requireActivity(), PlaceActivity.class);
                    intent.putExtra(FROM_MAPS, fromMaps);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (address.getLocality() == null || address.getThoroughfare() == null) {
                        intent.putExtra(ADDRESS_FROM_MAPS, "");
                        intent.putExtra(LATITUDE_FROM_MAPS, addPoint.latitude);
                        intent.putExtra(LONGITUDE_FROM_MAPS, addPoint.longitude);
                    } else {
                        intent.putExtra(ADDRESS_FROM_MAPS, addressLine.toString());
                        intent.putExtra(LATITUDE_FROM_MAPS, addPoint.latitude);
                        intent.putExtra(LONGITUDE_FROM_MAPS, addPoint.longitude);
                    }
                    // Не смог найти аналог startActivityForResult в JetPack
                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().finish();
                    markerdrop.remove();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(FROM_MAPS, fromMaps);
                    if (address.getLocality() == null || address.getThoroughfare() == null) {
                        bundle.putDouble(LATITUDE_FROM_MAPS, addPoint.latitude);
                        bundle.putDouble(LONGITUDE_FROM_MAPS, addPoint.longitude);
                    } else {
                        bundle.putString(ADDRESS_FROM_MAPS, addressLine.toString());
                        bundle.putDouble(LATITUDE_FROM_MAPS, addPoint.latitude);
                        bundle.putDouble(LONGITUDE_FROM_MAPS, addPoint.longitude);
                    }
                    findNavController(MapFragment.this).navigate(R.id.action_map_to_placeActivity, bundle);
                    markerdrop.remove();
                }
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (markerdrop != null) {
                    markerdrop.remove();
                }
            }
        });
        builder.setCancelable(false);
        builder.create();
        builder.show();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final long pos = mHashMap.get(marker);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.edit_place));
        builder.setMessage(getString(R.string.continue_edit));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Bundle bundle = new Bundle();
                bundle.putBoolean(FROM_MAPS, true);
                bundle.putLong(ID, pos);
                findNavController(MapFragment.this).navigate(R.id.action_map_to_placeActivity, bundle);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        Bundle extras = getArguments();
        if (extras != null) {
            fromActivityPlace = extras.getBoolean(FROM_ACTIVITY_PLACE);
            lat = extras.getDouble(LATITUDE_FROM_PLACEACTIVITY);
            lng = extras.getDouble(LONGITUDE_FROM_PLACEACTIVITY);
        }

        if (!NetworkChecker.isNetworkAvailable(requireContext())) {
            if (!snackbarInternet.isShown()) {
                snackbarInternet.show();
            }
        } else {
            if (snackbarInternet.isShown()) {
                snackbarInternet.dismiss();
            }
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onClick(View v) {
        if (v == binding.googleMapBtn)
            searchAddress(v);
    }
}