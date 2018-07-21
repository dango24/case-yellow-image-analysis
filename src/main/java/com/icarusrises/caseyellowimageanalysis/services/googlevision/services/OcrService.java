package com.icarusrises.caseyellowimageanalysis.services.googlevision.services;


import com.icarusrises.caseyellowimageanalysis.exceptions.RequestFailureException;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.GoogleVisionRequest;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.OcrResponse;

import java.io.IOException;

public interface OcrService {
    OcrResponse parseImage(GoogleVisionRequest googleVisionRequest) throws IOException, RequestFailureException;
}
