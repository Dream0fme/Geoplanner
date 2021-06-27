package urum.geoplanner.ui;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import urum.geoplanner.R;
import urum.geoplanner.adapter.PlaceLogAdapter;
import urum.geoplanner.databinding.ActivityLogBinding;
import urum.geoplanner.databinding.ActivityMainBinding;
import urum.geoplanner.db.entities.PlaceLog;
import urum.geoplanner.service.ConnectorService;
import urum.geoplanner.viewmodel.ModelFactory;
import urum.geoplanner.viewmodel.PlaceViewModel;

import static urum.geoplanner.utils.Constants.CLOSEAPPINTENTFILTER;
import static urum.geoplanner.utils.Constants.*;


@SuppressLint("RestrictedApi")
public class LogActivity extends AppCompatActivity {

    ActivityLogBinding binding;
    Menu menu;

    PlaceViewModel mPlaceViewModel;
    MaterialToolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initToolbar();
        registerReceiver(CloseReceiver, new IntentFilter(CLOSEAPPINTENTFILTER));

        ConnectorService connectorService = new ConnectorService(this);
        getLifecycle().addObserver(connectorService);


        mPlaceViewModel = new ViewModelProvider(this,
                new ModelFactory(getApplication())).get(PlaceViewModel.class);

        mPlaceViewModel.getPlacesFromLog().observe(this, new Observer<List<PlaceLog>>() {
            @Override
            public void onChanged(@Nullable List<PlaceLog> places) {
                PlaceLogAdapter adapter = new PlaceLogAdapter(LogActivity.this, places);
                binding.list.setAdapter(adapter);
            }
        });
    }

    private void initToolbar() {
        mToolbar = findViewById(R.id.toolbar_log);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.log));
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        this.menu = menu;
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_log, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0);
            item.setTitle(spanString);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menuClearLog:
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.log_delete);
                dialog.setMessage(R.string.log_delete_ask);
                dialog.setCancelable(true);
                dialog.setPositiveButton(getString(R.string.clear), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPlaceViewModel.deleteFromLogAll();
                        restartActivity();
                        Toast.makeText(getApplicationContext(), "Журнал событий очищен", Toast.LENGTH_SHORT).show();
                    }
                });

                dialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog al = dialog.create();
                al.show();
                break;
        }
        return true;
    }

    public void restartActivity() {
        if (Build.VERSION.SDK_INT >= 11) {
            recreate();
        } else {
            // эт так для себя оставил
            Intent intent = getIntent();
            overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
        }
    }


//    @Override
//    public void onBackPressed() {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        startActivity(intent);
//    }

    private BroadcastReceiver CloseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "DatabaseActivity закрыт");
            finish();
        }

    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(CloseReceiver);
    }
}
