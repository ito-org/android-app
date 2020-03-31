package app.bandemic.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NearbyDevicesViewModel extends ViewModel {
    public MutableLiveData<double[]> distances = new MutableLiveData<>(new double[0]);
}
