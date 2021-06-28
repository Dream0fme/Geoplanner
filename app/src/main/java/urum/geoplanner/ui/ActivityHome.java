package urum.geoplanner.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import urum.geoplanner.databinding.ActivityHomeBinding;
import urum.geoplanner.service.ConnectorService;


public class ActivityHome extends AppCompatActivity {
    int SPLASH_TIME;
    int SPLASH_TIME_MIN = 1200;
    int SPLASH_TIME_MAX = 3000;

    ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SPLASH_TIME = rnd(SPLASH_TIME_MIN, SPLASH_TIME_MAX);
        /*splashProgress.getProgressDrawable().setColorFilter(
                getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);*/
        playProgress();
        Log.d("mytag", "splash time: " + SPLASH_TIME);
        ConnectorService connectorService = new ConnectorService(this);
        getLifecycle().addObserver(connectorService);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mySuperIntent = new Intent(ActivityHome.this, MainActivity.class);
                startActivity(mySuperIntent);
                finish();
            }
        }, SPLASH_TIME);
    }

    public static int rnd(int min, int max) {
        max -= min;
        return (int) (Math.random() * ++max) + min;
    }

    private void playProgress() {
        ObjectAnimator.ofInt(binding.splashProgress, "progress", 100)
                .setDuration(SPLASH_TIME)
                .start();
    }
}
