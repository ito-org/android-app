package com.example.infectiontracker.network;

import com.example.infectiontracker.database.InfectedUUID;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface InfectionchainWebservice {

    @GET("strict/items/infected_ids")
    Call<InfectedUUIDResponse> getInfectedUUIDResponse();
}
