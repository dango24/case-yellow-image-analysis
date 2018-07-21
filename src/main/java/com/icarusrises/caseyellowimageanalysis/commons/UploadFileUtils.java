package com.icarusrises.caseyellowimageanalysis.commons;

import com.icarusrises.caseyellowimageanalysis.exceptions.RequestFailureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
public class UploadFileUtils {

    String DELIMITER = "-";
    String FILE_EXTENSION = ".png";
    String INCEPTION_DIR = "inception-snapshots";

    public static void uploadObject(URL url, String fileToUploadPath) {
        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");

            uploadObject(connection, new File(fileToUploadPath));

        } catch (IOException e) {
            log.error("Failed to upload file, " + e.getMessage(), e);
            throw new RequestFailureException("Failed to upload file, " + e.getMessage(), e);
        }
    }

    public static void uploadObject(HttpURLConnection connection, File fileToUpload) {
        int responseCode;

        try (DataOutputStream dataStream = new DataOutputStream(connection.getOutputStream());
             InputStream inputStream = new FileInputStream(fileToUpload)) {

            dataStream.write(IOUtils.toByteArray(inputStream));
            responseCode = connection.getResponseCode(); // Invoke request

            if (isRequestSuccessful(responseCode)) {
                log.info("Upload object succeed, Service returned response code " + responseCode);
            } else {
                log.error("Failed to upload file, responseCode is " + responseCode);
                throw new RequestFailureException("Failed to upload file, responseCode is " + responseCode);
            }

        } catch (IOException e) {
            log.error("Failed to upload file, " + e.getMessage(), e);
            throw new RequestFailureException("Failed to upload file, " + e.getMessage(), e);
        }
    }

    public static boolean isRequestSuccessful(int responseCode) {
        return responseCode >= 200 && responseCode < 300;
    }
}
