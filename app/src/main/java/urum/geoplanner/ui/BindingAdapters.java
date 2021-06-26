package urum.geoplanner.ui;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.databinding.BindingAdapter;

import com.google.android.material.textview.MaterialTextView;

import urum.geoplanner.R;

public class BindingAdapters {

    @BindingAdapter({"textRadius", "condition", "checkBoxEnter", "checkBoxExit"})
    public static void setRadius(MaterialTextView view, String textRadius, long condition, int checkBoxEnter, int checkBoxExit) {
        String res;
        int lastInt = (int) (condition % 10);
        if (lastInt > 1 & lastInt < 5) {
            res = textRadius + condition + view.getContext().getString(R.string.blank_space) + view.getContext().getString(R.string.meters1);
        } else if (lastInt == 1) {
            res = textRadius + condition + view.getContext().getString(R.string.blank_space) + view.getContext().getString(R.string.meter);
        } else {
            res = textRadius + condition + view.getContext().getString(R.string.blank_space) + view.getContext().getString(R.string.meters2);
        }
        if (checkBoxEnter == 0 & checkBoxExit == 0) {
            res += System.lineSeparator() + view.getContext().getString(R.string.actions_not_chosen);
        }
        SpannableString span = new SpannableString(res);
        span.setSpan(new ForegroundColorSpan(Color.BLACK), 0, textRadius.length() - 1, 0);
        view.setText(span);
    }

    @BindingAdapter({"entry", "number", "sms", "pos"})
    public static void setTextToCard(MaterialTextView view, String entry, String number, String sms, long pos) {
        StringBuilder info = new StringBuilder();
        switch ((int) pos) {
            case 0:
                info.append(entry)
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.call))
                        .append(':')
                        .append(view.getContext().getString(R.string.blank_space))
                        .append(number);
                break;
            case 1:
                info.append(entry)
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.sms))
                        .append(view.getContext().getString(R.string.blank_space))
                        .append(number)
                        .append(".")
                        .append(System.lineSeparator())
                        .append(sms);
                break;
            case 2:
                info.append(entry)
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.notification))
                        .append(':')
                        .append(System.lineSeparator())
                        .append(sms);
                break;
            case 3:
                info.append(entry)
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.on))
                        .append(':')
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.dnd));
                break;
            case 4:
                info.append(entry)
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.off))
                        .append(':')
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.dnd));
                break;
            case 5:
                info.append(entry)
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.wifi_on));
                break;
            case 6:
                info.append(entry)
                        .append(System.lineSeparator())
                        .append(view.getContext().getString(R.string.wifi_off));
                break;
            default:
                return;
        }
        SpannableString span = new SpannableString(info.toString());
        span.setSpan(new ForegroundColorSpan(Color.BLACK), 0, entry.length(), 0);
        view.setText(span);
    }
}
