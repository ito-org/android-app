package com.example.infectiontracker.network;

import com.example.infectiontracker.network.InfectionchainWebservice;

import retrofit2.Retrofit;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    private static InfectionchainWebservice webservice = null;
    public static final String BASE_URL = "https://backend.infectionchain.online";

    // should this be synchronized?
    public static InfectionchainWebservice getInfectionchainWebservice() {
        if(retrofit == null || webservice == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .build();
            webservice = retrofit.create(InfectionchainWebservice.class);
        }
        return webservice;
    }
}
