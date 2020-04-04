package org.itoapp.strict.repository;

import android.app.Application;
import android.util.Log;

import org.itoapp.strict.database.AppDatabase;
import org.itoapp.strict.database.InfectedUUID;
import org.itoapp.strict.database.InfectedUUIDDao;
import org.itoapp.strict.database.Infection;
import org.itoapp.strict.network.InfectedUUIDResponse;
import org.itoapp.strict.network.InfectionchainWebservice;
import org.itoapp.strict.network.RetrofitClient;

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

    public LiveData<List<Infection>> getPossiblyInfectedEncounters() {
        return infectedUUIDDao.getPossiblyInfectedEncounters();
    }

    public void refreshInfectedUUIDs() {
        webservice.getInfectedUUIDResponse().enqueue(new Callback<InfectedUUIDResponse>() {
            @Override
            public void onResponse(Call<InfectedUUIDResponse> call, Response<InfectedUUIDResponse> response) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    if(response.body() != null) {
                        infectedUUIDDao.insertAll(response.body().data.toArray(new InfectedUUID[response.body().data.size()]));
                    }
                    else {
                        // TODO: error handling!
                        Log.e(LOG_TAG, "Invalid response from api");
                    }

                });
            }

            @Override
            public void onFailure(Call<InfectedUUIDResponse> call, Throwable t) {
                // TODO error handling
                //Log.e(LOG_TAG, t.getCause().getMessage());
                //Log.e(LOG_TAG, t.getMessage() + t.getStackTrace().toString());
            }
        });
    }
}
