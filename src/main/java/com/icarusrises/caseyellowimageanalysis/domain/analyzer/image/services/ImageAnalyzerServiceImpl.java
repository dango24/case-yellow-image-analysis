package com.icarusrises.caseyellowimageanalysis.domain.analyzer.image.services;

import com.icarusrises.caseyellowimageanalysis.commons.FileUtils;
import com.icarusrises.caseyellowimageanalysis.domain.analyzer.image.model.AnalyzedImage;
import com.icarusrises.caseyellowimageanalysis.exceptions.AnalyzeException;
import com.icarusrises.caseyellowimageanalysis.exceptions.SpeedTestParserException;
import com.icarusrises.caseyellowimageanalysis.queues.model.ImageDetails;
import com.icarusrises.caseyellowimageanalysis.queues.model.MessageType;
import com.icarusrises.caseyellowimageanalysis.queues.services.MessageProducerService;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.GoogleVisionRequest;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.OcrResponse;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.services.OcrService;
import com.icarusrises.caseyellowimageanalysis.services.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.icarusrises.caseyellowimageanalysis.commons.FileUtils.adjustSuccessfulDirPath;
import static com.icarusrises.caseyellowimageanalysis.commons.ImageUtils.createData;

@Slf4j
@Service
public class ImageAnalyzerServiceImpl implements ImageAnalyzerService {

    @Value("${snapshot_dir}")
    private String successfulTestsDir;

    private OcrService ocrService;
    private StorageService storageService;
    private MessageProducerService messageProducerService;
    private SpeedTestParserSupplier speedTestParserSupplier;

    @Autowired
    public ImageAnalyzerServiceImpl(OcrService ocrService,
                                    StorageService storageService,
                                    MessageProducerService messageProducerService,
                                    SpeedTestParserSupplier speedTestParserSupplier) {

        this.ocrService = ocrService;
        this.storageService = storageService;
        this.messageProducerService = messageProducerService;
        this.speedTestParserSupplier = speedTestParserSupplier;
    }

    @Override
    public void analyzeImage(ImageDetails imageDetails) throws AnalyzeException {
        File imageFile = null;

        try {
            imageFile = storageService.getFile( adjustSuccessfulDirPath(successfulTestsDir, imageDetails.getPath()) );
            GoogleVisionRequest googleVisionRequest = new GoogleVisionRequest(imageFile.getAbsolutePath());
            Map<String, Object> data = createData(imageDetails.getIdentifier(), googleVisionRequest);

            AnalyzedImage analyzedImage = analyzeImage(data);
            analyzedImage.setPath(imageDetails.getPath());
            analyzedImage.setMd5(imageDetails.getMd5());

            if (!analyzedImage.isAnalyzed()) {
                log.error(String.format("Failed to analyze image: %s", imageDetails.getPath()));
            }

            messageProducerService.send(MessageType.SNAPSHOT_ANALYZED, analyzedImage);

        } catch (Exception e) {
            String errorMessage = String.format("Failed to analyze image: %s, cause: %s", imageDetails.getPath(), e.getMessage());
            log.error(errorMessage);
            throw new AnalyzeException(errorMessage, e);

        } finally {
            FileUtils.deleteFile(imageFile);
        }
    }

    @Override
    public OcrResponse analyzeImage(GoogleVisionRequest googleVisionRequest) throws IOException {
        return ocrService.parseImage(googleVisionRequest);
    }

    @Override
    public AnalyzedImage analyzeImage(Map<String, Object> data) throws IOException {
        try {
            SpeedTestParser speedTestParser = speedTestParserSupplier.getSpeedTestParser(String.valueOf(data.get("identifier")));
            double result = speedTestParser.parseSpeedTest(data);

            return new AnalyzedImage(result);

        } catch (IllegalArgumentException | SpeedTestParserException e) {
            log.error(String.format("Failed to analyze image: %s", e.getMessage()), e);
            return AnalyzedImage.AnalyzedImageFailure(e.getMessage());
        }
    }
}
