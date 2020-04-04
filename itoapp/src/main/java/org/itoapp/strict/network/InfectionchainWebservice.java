package org.itoapp.strict.network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface InfectionchainWebservice {

    @GET("strict/items/infected_ids")
    Call<InfectedUUIDResponse> getInfectedUUIDResponse();
}
