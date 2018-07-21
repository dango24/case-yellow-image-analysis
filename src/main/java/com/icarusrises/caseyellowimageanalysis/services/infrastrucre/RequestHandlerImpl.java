package com.icarusrises.caseyellowimageanalysis.services.infrastrucre;


import com.icarusrises.caseyellowimageanalysis.exceptions.RequestFailureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class RequestHandlerImpl implements RequestHandler {

    private Call<? extends Object> currentRequest = null;

    @Override
    public void cancelRequest() {
        try {
            if (Objects.nonNull(currentRequest)) {
                currentRequest.cancel();
            }
        } catch (Exception e) {
            log.error("Failed to cancel request, " + e.getMessage(), e);
        }
    }

    @Override
    public <T extends Object> T execute(Call<T> request) throws RequestFailureException {
        try {
            currentRequest = request;
            return executeRequest();

        } catch (IOException e) {
                throw new RequestFailureException(e.getMessage(), e);
        } finally {
            currentRequest = null;
        }
    }

    private <T extends Object> T executeRequest() throws IOException, RequestFailureException {
        Response<T> response = (Response<T>) currentRequest.execute();

        if (response.isSuccessful()) {
            return response.body();

        } else if (currentRequest.isCanceled()) {
            throw new RequestFailureException("User cancelRequest request");
        } else {
            throw new RequestFailureException(response.errorBody().string(), response.code());
        }
    }
}
