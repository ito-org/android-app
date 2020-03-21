package com.example.infectiontracker.repository;

import android.app.Application;
import android.util.Log;

import com.example.infectiontracker.database.AppDatabase;
import com.example.infectiontracker.database.InfectedUUID;
import com.example.infectiontracker.database.InfectedUUIDDao;
import com.example.infectiontracker.network.InfectedUUIDResponse;
import com.example.infectiontracker.network.InfectionchainWebservice;
import com.example.infectiontracker.network.RetrofitClient;

import java.util.List;

import androidx.lifecycle.LiveData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InfectedUUIDRepository {

    private static final String LOG_TAG = "InfectedUUIDRepository";

    private InfectionchainWebservice webservice;

    private InfectedUUIDDao infectedUUIDDao;

    public InfectedUUIDRepository(Application application) {
        webservice = RetrofitClient.getInfectionchainWebservice();
        AppDatabase db = AppDatabase.getDatabase(application);
        infectedUUIDDao = db.infectedUUIDDao();
    }

    public LiveData<List<InfectedUUID>> getInfectedUUIDs() {
        refreshInfectedUUIDs();
        return infectedUUIDDao.getAll();
    }

    public LiveData<List<InfectedUUID>> getPossiblyInfectedEncounters() {
        // TODO: do comparison between InfectedUUID and Beacons
        // dummy return
        return infectedUUIDDao.getAll();
    }

    public void refreshInfectedUUIDs() {
        webservice.getInfectedUUIDResponse().enqueue(new Callback<InfectedUUIDResponse>() {
            @Override
            public void onResponse(Call<InfectedUUIDResponse> call, Response<InfectedUUIDResponse> response) {
                infectedUUIDDao.insertAll(response.body().data.toArray(new InfectedUUID[response.body().data.size()]));
            }

            @Override
            public void onFailure(Call<InfectedUUIDResponse> call, Throwable t) {
                // TODO error handling
                Log.e(LOG_TAG, t.getCause().getMessage());
                Log.e(LOG_TAG, t.getMessage() + t.getStackTrace().toString());
            }
        });
    }
}
