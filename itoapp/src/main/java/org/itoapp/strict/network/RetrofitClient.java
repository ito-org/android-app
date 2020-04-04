package org.itoapp.strict.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    private static InfectionchainWebservice webservice = null;
    public static final String BASE_URL = "https://backend.infectionchain.online";

    // should this be synchronized?
    public static InfectionchainWebservice getInfectionchainWebservice() {
        if(retrofit == null || webservice == null) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .registerTypeHierarchyAdapter(byte[].class, new HexStringToByteArrayTypeAdapter())
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            webservice = retrofit.create(InfectionchainWebservice.class);
        }
        return webservice;
    }
}
