package com.icarusrises.caseyellowimageanalysis.services.infrastrucre;

import com.icarusrises.caseyellowimageanalysis.exceptions.RequestFailureException;
import retrofit2.Call;

public interface RequestHandler {

    void cancelRequest();
    <T extends Object> T execute(Call<T> request) throws RequestFailureException;
}
