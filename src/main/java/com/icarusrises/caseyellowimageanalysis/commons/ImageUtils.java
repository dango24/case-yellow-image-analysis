package com.icarusrises.caseyellowimageanalysis.commons;

import com.icarusrises.caseyellowimageanalysis.exceptions.AnalyzeException;
import com.icarusrises.caseyellowimageanalysis.exceptions.IORuntimeException;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.GoogleVisionRequest;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.Image;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.VisionRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Slf4j
public class ImageUtils {

   private static String IMAGE_RESOLUTION_SCHEMA = "%s_%s";

   public static Map<String, Object> createData(String identifier, GoogleVisionRequest googleVisionRequest) {
        Map<String,Object> data = new HashMap<>();
        data.put("identifier", identifier);
        data.put("file", googleVisionRequest);

        return data;
    }

   public static File getImgFromResources(String path) throws IOException {
        InputStream resourceAsStream = ImageUtils.class.getResourceAsStream(path);
        BufferedImage image = ImageIO.read(resourceAsStream);
        File tmpFile = File.createTempFile("test_", new File(path).getName());
        ImageIO.write(image, "PNG", tmpFile);

        return tmpFile;
    }

   public static File createTmpPNGFile() throws IORuntimeException {
        try {
            return File.createTempFile("tmpImage", ".PNG");
        } catch (IOException e) {
            throw new IORuntimeException(e.getMessage(), e);
        }
    }

   public static String getImageResolution(VisionRequest visionRequest) {
        File imgFile = null;

        try {
            imgFile = convertBase64ToImage(visionRequest.getImage());
            return getImageResolution(imgFile);

        } catch (Exception e) {
            String errorMessage = String.format("Failed to get image resolution: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new AnalyzeException(errorMessage);

        } finally {
            com.icarusrises.caseyellowimageanalysis.commons.FileUtils.deleteFile(imgFile);
        }
    }

   public static String getImageResolution(File imgFile) throws IOException {
        BufferedImage img = ImageIO.read(imgFile);
        int width = img.getWidth();
        int height = img.getHeight();

        return String.format(IMAGE_RESOLUTION_SCHEMA, width, height);
    }

   public static byte[] createImageBase64Encode(String imgPath)  {
        try {
            File imageFile = new File(imgPath);
            byte[] imageBase64Encode = Base64.getEncoder().encode(FileUtils.readFileToByteArray(imageFile));

            return imageBase64Encode;

        } catch (IOException e) {
            throw new IORuntimeException(e.getMessage(), e);
        }
    }

   public static File convertToNegative(File imgFile) {
        BufferedImage img = null;

        //read image
        try{
            img = ImageIO.read(imgFile);
        }catch(IOException e){
            log.error(e.getMessage(), e);
        }
        //get image width and height
        int width = img.getWidth();
        int height = img.getHeight();
        //convertToNegative to negative
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int p = img.getRGB(x,y);
                int a = (p>>24)&0xff;
                int r = (p>>16)&0xff;
                int g = (p>>8)&0xff;
                int b = p&0xff;
                //subtract RGB from 255
                int offset = -1;
                r = 255 - r - offset;
                g = 255 - g - offset;
                b = 255 - b - offset;
                //set new RGB value
                p = (a<<24) | (r<<16) | (g<<8) | b;
                img.setRGB(x, y, p);
            }
        }
        //write image
        try{
            imgFile = File.createTempFile("negative_file", "output.PNG");
            ImageIO.write(img, "PNG", imgFile);

            return imgFile;

        }catch(IOException e){
            String errorMessage = format("Failed to convert to negative: ", e.getMessage());
            log.error(errorMessage);
            throw new AnalyzeException(errorMessage, e);
        }
    }

   public static File convertBase64ToImage(Image image) {
        File tmpFile = createTmpPNGFile();

        try (FileOutputStream out = new FileOutputStream(tmpFile)) {
            byte[] btDataFile = new sun.misc.BASE64Decoder().decodeBuffer(image.getContent());
            out.write(btDataFile);
            out.flush();

        } catch (IOException e) {
            log.error(format("Failed to convert file to negative, error message: %s", e.getMessage(), e));
        }

        return tmpFile;
    }
}
