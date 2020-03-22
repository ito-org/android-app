package com.example.infectiontracker.viewmodel;

import android.app.Application;

import com.example.infectiontracker.database.InfectedUUID;
import com.example.infectiontracker.repository.InfectedUUIDRepository;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class InfectionCheckViewModel extends AndroidViewModel {

    private InfectedUUIDRepository mRepository;

    private LiveData<List<InfectedUUID>> possiblyInfectedEncounters;

    public InfectionCheckViewModel(Application application) {
        super(application);
        mRepository = new InfectedUUIDRepository(application);
        possiblyInfectedEncounters = mRepository.getPossiblyInfectedEncounters();
    }

    public void refreshInfectedUUIDs() {
        mRepository.refreshInfectedUUIDs();
    }

    public LiveData<List<InfectedUUID>> getPossiblyInfectedEncounters() { return possiblyInfectedEncounters; }

}
