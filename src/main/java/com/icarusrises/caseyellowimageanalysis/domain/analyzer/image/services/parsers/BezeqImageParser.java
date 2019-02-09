package com.icarusrises.caseyellowimageanalysis.domain.analyzer.image.services.parsers;

import com.icarusrises.caseyellowimageanalysis.commons.WordUtils;
import com.icarusrises.caseyellowimageanalysis.domain.analyzer.image.model.PinnedWord;
import com.icarusrises.caseyellowimageanalysis.domain.analyzer.image.model.WordData;
import com.icarusrises.caseyellowimageanalysis.exceptions.SpeedTestParserException;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.GoogleVisionRequest;
import com.icarusrises.caseyellowimageanalysis.services.googlevision.model.OcrResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.icarusrises.caseyellowimageanalysis.commons.WordUtils.createPinnedWord;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;

@Slf4j
@Component
public class BezeqImageParser extends ImageTestParser {


    @Value("${bezeq_Mb_location}")
    private String bezeqMbLocation;

    @Value("${bezeq_Kb_location}")
    private String bezeqKbLocation;

    @Override
    public double parseSpeedTest(Map<String, Object> data) throws IOException {
        double result;
        validateData(data);
        GoogleVisionRequest googleVisionRequest = (GoogleVisionRequest)data.get("file");
        Map<String, List<WordData>> bezeqIdentifiers;
        data = addExtraData(data);

        try {
            OcrResponse ocrResponse = parseImage(googleVisionRequest, String.valueOf(data.get(NEGATIVE_PARSING)));
            log.info("successfully retrieve ocr response");

            bezeqIdentifiers =
                    IntStream.range(0, ocrResponse.getTextAnnotations().size())
                            .filter(index -> ocrResponse.getTextAnnotations().get(index).getDescription().equals(bezeqMbLocation) ||
                                             ocrResponse.getTextAnnotations().get(index).getDescription().equals(bezeqKbLocation))
                            .mapToObj(index -> new WordData(ocrResponse.getTextAnnotations().get(index), index))
                            .collect(groupingBy(WordData::getDescription));

            if (bezeqIdentifiers.size() != 2) {
                throw new IllegalArgumentException(String.format("The number of found identifiers is not match for identifier: %s  expected: %s , actual: %s", data.get("identifier"), 2, bezeqIdentifiers.size()));
            }

            validateResults(bezeqIdentifiers);

            PinnedWord mbPinnedWord = createPinnedWord(bezeqIdentifiers.get(bezeqMbLocation).get(0));

            List<PinnedWord> floatLocationsInText =
                    ocrResponse.getTextAnnotations()
                            .stream()
                            .filter(word -> isCreatable(word.getDescription()))
                            .map(WordUtils::createPinnedWord)
                            .sorted(comparing(word -> WordUtils.euclideanDistance(word.getCentralizedLocation(), mbPinnedWord.getCentralizedLocation())))
                            .collect(Collectors.toList());

            if (floatLocationsInText.isEmpty()) {
                throw new SpeedTestParserException("Failed to parse image, No numbers found");
            }

            result = Double.valueOf(floatLocationsInText.get(0).getDescription()); // retrieve the closest word to bezeqmb identifier

            if (result <= 0) {
                throw new IllegalArgumentException(String.format("Bezeq image parser result is less than 0, result: %s", result));
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to parse image, " + e.getMessage(), e);
            throw new SpeedTestParserException("Failed to parse image, " + e.getMessage(), e);
        }
    }

    @Override
    public String getIdentifier() {
        return "bezeq";
    }

    private void validateResults(Map<String, List<WordData>> bezeqIdentifiers) {

        if (isNull(bezeqIdentifiers.get(bezeqMbLocation)) || bezeqIdentifiers.get(bezeqMbLocation).size() != 1) {
            throw new SpeedTestParserException(bezeqMbLocation + " is null or not match the amount of correct identifiers, result: " + bezeqIdentifiers.get(bezeqMbLocation));
        }

        if (isNull(bezeqIdentifiers.get(bezeqKbLocation)) || bezeqIdentifiers.get(bezeqKbLocation).size() != 1) {
            throw new SpeedTestParserException(bezeqKbLocation + " is null or not match the amount of correct identifiers, result: " + bezeqIdentifiers.get(bezeqKbLocation));
        }

        PinnedWord kbPinnedWord = createPinnedWord(bezeqIdentifiers.get(bezeqKbLocation).get(0));
        PinnedWord mbPinnedWord = createPinnedWord(bezeqIdentifiers.get(bezeqMbLocation).get(0));

        if (!((kbPinnedWord.getCentralizedLocation().getX() < mbPinnedWord.getCentralizedLocation().getX()) ||
             (kbPinnedWord.getCentralizedLocation().getY() > mbPinnedWord.getCentralizedLocation().getY())) ) {
            throw new SpeedTestParserException("Failed to parse image, the Kb point is from the right of the Mb point.");
        }
    }
}
