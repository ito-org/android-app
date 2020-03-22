package com.example.infectiontracker.viewmodel;

import android.app.Application;

import com.example.infectiontracker.database.Beacon;
import com.example.infectiontracker.database.InfectedUUID;
import com.example.infectiontracker.repository.BroadcastRepository;
import com.example.infectiontracker.repository.InfectedUUIDRepository;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class EnvironmentLoggerViewModel extends AndroidViewModel {

    private BroadcastRepository mBroadcastRepository;

    private LiveData<List<Beacon>> mAllBeacons;

    public EnvironmentLoggerViewModel(Application application) {
        super(application);
        //TODO: are two instances of repository ok (in ViewModel and TracingService)?
        mBroadcastRepository = new BroadcastRepository(application);
        mAllBeacons = mBroadcastRepository.getAllBeacons();
    }

    //todo do I need a refresh function as for uuids? this should update automatically

    public LiveData<List<Beacon>> getAllBeacons() {
        return mAllBeacons;
    }
}

