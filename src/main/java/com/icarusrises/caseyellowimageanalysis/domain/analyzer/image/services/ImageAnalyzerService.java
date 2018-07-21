package com.icarusrises.caseyellowimageanalysis.domain.analyzer.image.services;


import com.icarusrises.caseyellowimageanalysis.domain.analyzer.image.model.AnalyzedImage;
import com.icarusrises.caseyellowimageanalysis.exceptions.AnalyzeException;
import com.icarusrises.caseyellowimageanalysis.queues.model.ImageDetails;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.GoogleVisionRequest;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.OcrResponse;

import java.io.IOException;
import java.util.Map;

public interface ImageAnalyzerService {
    void analyzeImage(ImageDetails imageDetails) throws AnalyzeException;
    OcrResponse analyzeImage(GoogleVisionRequest googleVisionRequest) throws IOException;
    AnalyzedImage analyzeImage(Map<String, Object> data) throws IOException;
}
