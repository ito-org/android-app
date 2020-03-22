package app.bandemic.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class MainActivityViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> eventRefresh;

    public LiveData<Boolean> eventRefresh() {
        if(eventRefresh == null) {
            eventRefresh = new MutableLiveData<>();
        }
        return eventRefresh;
    }

    public void onRefresh() {
        eventRefresh.setValue(true);
    }

    public void finishRefresh() {
        eventRefresh.setValue(false);
    }

    public MainActivityViewModel(Application application) {
        super(application);
    }
}
