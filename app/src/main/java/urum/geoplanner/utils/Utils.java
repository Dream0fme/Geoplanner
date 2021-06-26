package urum.geoplanner.utils;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
    public static NavController findNavController(@NonNull Fragment fragment) {
        View view = fragment.getView();
        if (view != null) {
            return Navigation.findNavController(view);
        } else return null;
    }
    public static void enableLayout(ViewGroup layout, boolean enable) {
        layout.setEnabled(enable);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                enableLayout((ViewGroup) child, enable);
            } else {
                child.setEnabled(enable);
            }
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
