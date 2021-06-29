package urum.geoplanner.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionSet;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;
import urum.geoplanner.R;
import urum.geoplanner.databinding.ActivityMainBinding;
import urum.geoplanner.service.ConnectorService;
import urum.geoplanner.service.LocationService;
import urum.geoplanner.utils.BootReceiver;

import static urum.geoplanner.utils.Constants.CLOSEAPPINTENTFILTER;
import static urum.geoplanner.utils.Constants.FROM_ACTIVITY_PLACE;
import static urum.geoplanner.utils.Constants.FROM_NOTIFICATION_TO_SETTINGS;
import static urum.geoplanner.utils.Constants.LATITUDE_FROM_PLACEACTIVITY;
import static urum.geoplanner.utils.Constants.LOCATION_PERMISSIONS;
import static urum.geoplanner.utils.Constants.LONGITUDE_FROM_PLACEACTIVITY;
import static urum.geoplanner.utils.Constants.REQUEST_CHECK_SETTINGS;
import static urum.geoplanner.utils.Constants.SWITCH_SERVICE;
import static urum.geoplanner.utils.Constants.TAG;
import static urum.geoplanner.utils.Utils.enableLayout;


@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, EasyPermissions.PermissionCallbacks {

    private String PACKAGE_NAME;

    public ActivityMainBinding binding;
    MaterialToolbar mToolbar;
    public NavController navController;
    CoordinatorLayout.LayoutParams params;
    Transition transition;

    FragmentManager fm;

    private ConnectorService connectorService;

    boolean activeService;
    SharedPreferences sharedPreferences;
    Snackbar snackbarActiveService;

    private GoogleApiClient googleApiClient;
    private LocationRequest mLocationRequest;
    Bundle bundle;


    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        PACKAGE_NAME = this.getPackageName();
        registerReceiver(CloseReceiver, new IntentFilter(CLOSEAPPINTENTFILTER));
        initToolbar();
        binding.setLifecycleOwner(this);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.list_places, R.id.map, R.id.settings)
                .build();

        fm = getSupportFragmentManager();

        NavHostFragment navHostFragment =
                (NavHostFragment) fm.findFragmentById(R.id.nav_host_fragment);

        navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        transition = new Fade();
        transition.setDuration(400);
        TransitionSet set = new TransitionSet();
        set.addTransition(transition);

        params = (CoordinatorLayout.LayoutParams)
                binding.navHostFragment.getLayoutParams();

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onDestinationChanged(@NonNull NavController controller,
                                             @NonNull NavDestination destination, @Nullable Bundle arguments) {
                switch (destination.getId()) {
                    case R.id.list_places:
                        changeLayoutForListPlaces();
                        break;
                    case R.id.map:
                        changeLayoutForMap();
                        break;
                    case R.id.settings:
                        changeLayoutForSettings();
                        break;
                }
            }
        });

        if (navController.getCurrentDestination().getId() == R.id.archive) {
            binding.navView.setVisibility(View.GONE);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        activeService = sharedPreferences.getBoolean(SWITCH_SERVICE, true);

        connectorService = new ConnectorService(this);
        getLifecycle().addObserver(connectorService);

        final ComponentName onBootReceiver = new ComponentName(getApplication().getPackageName(), BootReceiver.class.getName());
        if (getPackageManager().getComponentEnabledSetting(onBootReceiver) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            getPackageManager().setComponentEnabledSetting(onBootReceiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        snackbarActiveService = Snackbar.make(binding.container, R.string.service_not_active,
                Snackbar.LENGTH_LONG).setAnchorView(binding.fabButton).setAction(R.string.turn_on,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences.Editor e = sharedPreferences.edit();
                        e.putBoolean(SWITCH_SERVICE, true);
                        e.apply();
                        startService(new Intent(MainActivity.this, LocationService.class));
                    }
                });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getBoolean(FROM_NOTIFICATION_TO_SETTINGS)) {
                Log.d("bundle", FROM_NOTIFICATION_TO_SETTINGS);
                navController.navigate(R.id.settings);
            }

            if (extras.getBoolean(FROM_ACTIVITY_PLACE)) {
                Log.d("bundle", "fromActivityPlace");
                bundle = new Bundle();
                bundle.putBoolean(FROM_ACTIVITY_PLACE, extras.getBoolean(FROM_ACTIVITY_PLACE));
                bundle.putDouble(LATITUDE_FROM_PLACEACTIVITY, extras.getDouble(LATITUDE_FROM_PLACEACTIVITY));
                bundle.putDouble(LONGITUDE_FROM_PLACEACTIVITY, extras.getDouble(LONGITUDE_FROM_PLACEACTIVITY));
                navController.navigate(R.id.map, bundle);
            }
        }
    }


    public void changeLayoutForListPlaces() {
        params.setBehavior(new AppBarLayout.ScrollingViewBehavior(binding.navHostFragment.getContext(), null));
        getSupportActionBar().show();
        binding.fabButton.show();
        slideUp();
    }

    public void changeLayoutForMap() {
        params.setBehavior(null);
        getSupportActionBar().hide();
        binding.fabButton.hide();
        slideUp();
    }

    public void changeLayoutForSettings() {
        params.setBehavior(new AppBarLayout.ScrollingViewBehavior(binding.navHostFragment.getContext(), null));
        getSupportActionBar().show();
        binding.fabButton.hide();
        slideUp();
    }

    public void changeLayoutForArchive(FragmentManager fm) {
        if (fm != null) {
            navController.navigate(R.id.action_list_places_to_archive);
        } else {
            binding.fabButton.hide();
            slideDown();
        }
    }

    public void slideUp() {
        ViewGroup.LayoutParams layoutParams = binding.navView.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.Behavior behavior =
                    ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
            if (behavior instanceof HideBottomViewOnScrollBehavior) {
                HideBottomViewOnScrollBehavior<BottomNavigationView> hideShowBehavior =
                        (HideBottomViewOnScrollBehavior<BottomNavigationView>) behavior;
                hideShowBehavior.slideUp(binding.navView);
            }
        }
    }

    public void slideDown() {
        ViewGroup.LayoutParams layoutParams = binding.navView.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.Behavior behavior =
                    ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
            if (behavior instanceof HideBottomViewOnScrollBehavior) {
                HideBottomViewOnScrollBehavior<BottomNavigationView> hideShowBehavior =
                        (HideBottomViewOnScrollBehavior<BottomNavigationView>) behavior;
                hideShowBehavior.slideDown(binding.navView);
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!activeService) {
            snackbarActiveService.show();
        }
        googleApiClient.connect();
        mLocationRequest = LocationRequest.create();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                    }
                }
            }
        });
        requiresLocationPermission();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        googleApiClient.disconnect();
        unregisterReceiver(CloseReceiver);
    }


    public void initToolbar() {
        mToolbar = findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
    }

    public void changeToolbarForArchive() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.archive));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (fm.getBackStackEntryCount() > 0 & fm != null) {
                    fm.popBackStackImmediate();
                } else {
                    this.onBackPressed();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void addPlace(View view) {
        navController.navigate(R.id.action_list_places_to_placeActivity);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case SWITCH_SERVICE:
                activeService = sharedPreferences.getBoolean(s, true);
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private BroadcastReceiver CloseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "MainActivity закрыт");
            finish();
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(LOCATION_PERMISSIONS)
    private void requiresLocationPermission() {
        List<String> permsList = new ArrayList<String>() {{
            add(Manifest.permission.ACCESS_FINE_LOCATION);
        }};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permsList.add(0, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        Log.d(TAG, permsList.toString());
        String[] perms = permsList.toArray(new String[0]);
        if (EasyPermissions.hasPermissions(this, perms)) {
            enableLayout(binding.container, true);
        } else {
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(this, LOCATION_PERMISSIONS, perms)
                            .setRationale(R.string.permission_rationale)
                            .setPositiveButtonText(R.string.start)
                            .build());
            enableLayout(binding.container, false);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "GRANTED: " + perms.toString());
        enableLayout(binding.container, true);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

        enableLayout(binding.container, false);
        Log.d(TAG, "Denied: " + perms.toString());
        for (String perm : perms) {
            Log.d(TAG, "Denied one: " + perm);
            if (EasyPermissions.permissionPermanentlyDenied(this, perm)) {
                Snackbar.make(binding.container,
                        R.string.manual_access_for_geo,
                        Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + PACKAGE_NAME)));
                            }
                        }).setAnchorView(binding.fabButton)
                        .show();
            } else if (EasyPermissions.somePermissionDenied(this, perm)) {
                Snackbar.make(binding.container,
                        R.string.geo_access_all_time,
                        Snackbar.LENGTH_INDEFINITE).setAction(R.string.turn_on,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                            LOCATION_PERMISSIONS);
                                } else {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            LOCATION_PERMISSIONS);
                                }
                            }
                        }).setAnchorView(binding.fabButton)
                        .show();
            } else {
                requiresLocationPermission();
            }
        }
    }
}