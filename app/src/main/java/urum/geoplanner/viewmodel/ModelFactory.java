package urum.geoplanner.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ModelFactory extends ViewModelProvider.NewInstanceFactory {

    @NonNull
    private final Application application;


    public ModelFactory(@NonNull Application application) {
        super();
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == PlaceViewModel.class) {
            return (T) new PlaceViewModel(application);
        }
        return null;
    }
}

