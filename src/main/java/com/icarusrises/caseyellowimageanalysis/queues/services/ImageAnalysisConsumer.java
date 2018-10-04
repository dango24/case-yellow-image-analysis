package com.icarusrises.caseyellowimageanalysis.queues.services;

import com.amazonaws.services.s3.event.S3EventNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icarusrises.caseyellowimageanalysis.domain.analyzer.image.services.ImageAnalyzerService;
import com.icarusrises.caseyellowimageanalysis.exceptions.AnalyzeException;
import com.icarusrises.caseyellowimageanalysis.queues.model.ImageDetails;
import com.icarusrises.caseyellowimageanalysis.queues.model.MessageType;
import com.icarusrises.caseyellowimageanalysis.queues.model.QueueMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static java.util.Objects.nonNull;

@Slf4j
@Component
@Profile("prod")
@ConfigurationProperties(prefix = "image-analyze")
public class ImageAnalysisConsumer {

    @Value("${snapshot_dir}")
    private String snapshotDir;

    @Getter @Setter
    private List<String> identifiers;

    private ObjectMapper mapper;
    private ImageAnalyzerService imageAnalyzerService;

    @Autowired
    public ImageAnalysisConsumer(ImageAnalyzerService imageAnalyzerService) {
        this.imageAnalyzerService = imageAnalyzerService;
        this.mapper = new ObjectMapper();
    }

    @JmsListener(destination = "${sqs.analyze_snapshot.queue}", containerFactory = "S3EventNotificationContainerFactory")
    public void processS3EventNotificationMessage(@Payload S3EventNotification s3EventNotification) {

        if (isValidNotificationMessage(s3EventNotification)) {

            s3EventNotification.getRecords()
                    .stream()
                    .filter(this::isValidRecord)
                    .map(S3EventNotification.S3EventNotificationRecord::getS3)
                    .map(S3EventNotification.S3Entity::getObject)
                    .map(S3EventNotification.S3ObjectEntity::getKey)
                    .map(this::buildImagePathDetails)
                    .filter(this::isImageQualifiedForAnalyze)
                    .peek(this::putMDC)
                    .forEach(this::analyzeImage);
        }
    }

    @JmsListener(destination = "${sqs.image_analyze.queue}", containerFactory = "imageMessageContainerFactory")
    public void processImageMessage(@Payload QueueMessage queueMessage) throws IOException {

        if (nonNull(queueMessage) && queueMessage.getMessageType() == MessageType.IMAGE_ANALYSIS) {

            ImageDetails imageDetails = mapper.readValue(queueMessage.getPayload(), ImageDetails.class);
            putMDC(imageDetails);
            analyzeImage(imageDetails);
        }
    }

    private void analyzeImage(ImageDetails imagePathDetails) {
        log.info(String.format("Start analyzing image for path: %s", imagePathDetails.getPath()));

        try {
            imageAnalyzerService.analyzeImage(imagePathDetails);

        } catch (AnalyzeException e) {
            handleAnalyzedImageFaliure(imagePathDetails);

        }finally {
            MDC.remove("correlation-id");
        }
    }

    private boolean isImageQualifiedForAnalyze(ImageDetails imagePathDetails) {
        return identifiers.contains(imagePathDetails.getIdentifier());
    }

    private boolean isValidNotificationMessage(S3EventNotification s3EventNotification) {
        return nonNull(s3EventNotification) && nonNull(s3EventNotification.getRecords()) && !s3EventNotification.getRecords().isEmpty();
    }

    private boolean isValidRecord(S3EventNotification.S3EventNotificationRecord eventNotificationRecord) {

        return nonNull(eventNotificationRecord) &&
               nonNull(eventNotificationRecord.getS3()) &&
               nonNull(eventNotificationRecord.getS3().getObject()) &&
               StringUtils.isNotEmpty(eventNotificationRecord.getS3().getObject().getKey());
    }

    private void putMDC(ImageDetails imagePathDetails) {
        MDC.put("correlation-id", imagePathDetails.getMd5());
    }

    private ImageDetails buildImagePathDetails(String path) {
        String[] pathArgs = getPathArguments(path);

        if (pathArgs.length != 4) { // Total numbers of valid arguments
            String errorMessage = String.format("Failed to generate ImageDetails, path args is not in length 4 for path: %s", path);
            log.error(errorMessage);
            throw new AnalyzeException(errorMessage);
        }

        String user = pathArgs[0];
        String md5 = pathArgs[2];
        String identifier = getIdentifierFromPathArgs(pathArgs);

        return new ImageDetails(path, user, identifier, md5);
    }

    private String[] getPathArguments(String path) {
        return path.replaceAll(snapshotDir, "").split("-");
    }

    private String getIdentifierFromPathArgs(String[] pathArgs) {
        String identifier = pathArgs[pathArgs.length -1];

        return identifier.replaceAll(".png", "")
                         .replaceAll(".PNG", "");
    }

    private void handleAnalyzedImageFaliure(ImageDetails imagePathDetails) {

    }
}
