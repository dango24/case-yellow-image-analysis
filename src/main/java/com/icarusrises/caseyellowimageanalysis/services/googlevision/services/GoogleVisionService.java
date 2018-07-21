package com.icarusrises.caseyellowimageanalysis.services.googlevision.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icarusrises.caseyellowimageanalysis.configuration.ConfigurationManager;
import com.icarusrises.caseyellowimageanalysis.exceptions.RequestFailureException;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.GoogleVisionRequest;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.OcrResponse;
import com.icarusrises.caseyellowimageanalysis.services.infrastrucre.RequestHandler;
import com.icarusrises.caseyellowimageanalysis.services.infrastrucre.RetrofitBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
public class GoogleVisionService implements OcrService {

    @Value("${google_vision_url}")
    private String googleVisionUrl;

    private RequestHandler requestHandler;
    private ConfigurationManager configurationManager;
    private GoogleVisionRetrofitRequests googleVisionRetrofitRequests;

    @Autowired
    public GoogleVisionService(ConfigurationManager configurationManager, RequestHandler requestHandler) {
        this.configurationManager = configurationManager;
        this.requestHandler = requestHandler;
    }

    @PostConstruct
    public void init() {
        Retrofit retrofit = RetrofitBuilder.Retrofit(googleVisionUrl)
                                           .build();

        googleVisionRetrofitRequests = retrofit.create(GoogleVisionRetrofitRequests.class);
    }

    @Override
    public OcrResponse parseImage(GoogleVisionRequest googleVisionRequest) throws IOException, RequestFailureException {
        String googleVisionKey = configurationManager.googleVisionKey();
        JsonNode response = requestHandler.execute(googleVisionRetrofitRequests.ocrRequest(googleVisionKey, googleVisionRequest));
        return parseResponse(response);
    }

    private OcrResponse parseResponse(JsonNode response) throws IOException {
        JsonNode textAnnotations = response.at("/responses/0").get("textAnnotations");
        String wordsData = "{ \"textAnnotations\" : " + textAnnotations.toString() + "}";

        return new ObjectMapper().readValue(wordsData, OcrResponse.class);
    }
}
