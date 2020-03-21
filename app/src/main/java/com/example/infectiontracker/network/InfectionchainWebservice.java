package com.example.infectiontracker.network;

import com.example.infectiontracker.database.InfectedUUID;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface InfectionchainWebservice {

    @GET("directus/items/infected_ids")
    Call<List<InfectedUUID>> getInfectedUUIDs();
}
