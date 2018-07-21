package com.icarusrises.caseyellowimageanalysis.commons;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.util.Objects.nonNull;

@Slf4j
public class FileUtils {

    public static void deleteFile(File file) {
        try {
            if (nonNull(file) && file.exists()) {
                Files.deleteIfExists(file.toPath());
            }
        } catch (IOException e) {
            log.error(String.format("Failed to delete file: %s", e.getMessage()));
        }
    }
}
