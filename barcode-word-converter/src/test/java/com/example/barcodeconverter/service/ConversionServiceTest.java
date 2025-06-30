package com.example.barcodeconverter.service;

import com.example.barcodeconverter.model.BarcodeSegmentRule;
import com.example.barcodeconverter.model.RuleSet;
import com.example.barcodeconverter.model.SegmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// No Spring context needed for this unit test if we manually set up dependencies
public class ConversionServiceTest {

    @InjectMocks
    private ConversionService conversionService;

    private RuleSet mockRuleSet;
    private List<String> sampleWordList;
    private Map<String, Integer> sampleWordToIndexMap;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this); // Initialize mocks

        // Prepare a sample word list and map for the service
        sampleWordList = Arrays.asList("apple", "banana", "cherry", "date", "elderberry",
                                       "fig", "grape", "honeydew", "kiwi", "lemon",
                                       "mango", "nectarine", "orange", "papaya", "quince", "raspberry"); // 16 words
        sampleWordToIndexMap = new HashMap<>();
        for (int i = 0; i < sampleWordList.size(); i++) {
            sampleWordToIndexMap.put(sampleWordList.get(i), i);
        }

        // Use ReflectionTestUtils to set private fields in ConversionService
        ReflectionTestUtils.setField(conversionService, "wordList", sampleWordList);
        ReflectionTestUtils.setField(conversionService, "wordToIndexMap", sampleWordToIndexMap);

        // Setup a default mock RuleSet for tests
        List<BarcodeSegmentRule> rules = new ArrayList<>();
        rules.add(new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true)); // word 1 (index)
        rules.add(new BarcodeSegmentRule(1, 2, SegmentType.STATIC, "XX", false));
        rules.add(new BarcodeSegmentRule(2, 4, SegmentType.NUMERIC, null, true)); // word 2 (index)
        rules.add(new BarcodeSegmentRule(3, 3, SegmentType.BASE64, null, false)); // placeholder base64
        rules.add(new BarcodeSegmentRule(4, 4, SegmentType.NUMERIC, null, true)); // word 3 (index)
        rules.add(new BarcodeSegmentRule(5, 1, SegmentType.STATIC, "Y", false));
        rules.add(new BarcodeSegmentRule(6, 4, SegmentType.NUMERIC, null, true)); // word 4 (index)

        mockRuleSet = new RuleSet("test-rules", rules);
        // Ensure the rule set is validated by calling its validation method.
        // The ConversionService also calls this, but good to have it pre-validated for test setup.
        mockRuleSet.validateRules();
    }

    @Test
    void wordsToBarcode_success_withBase64Placeholder() {
        List<String> words = Arrays.asList("apple", "banana", "cherry", "date");
        // Expected: 0000 (apple) + XX + 0001 (banana) + AAA (base64 placeholder length 3) + 0002 (cherry) + Y + 0003 (date)
        // RuleSet: NUMERIC(4,T) + STATIC(2,"XX") + NUMERIC(4,T) + BASE64(3,F) + NUMERIC(4,T) + STATIC(1,"Y") + NUMERIC(4,T)
        // Total length: 4+2+4+3+4+1+4 = 22
        String expectedBarcode = "0000XX0001AAA0002Y0003";

        String actualBarcode = conversionService.wordsToBarcode(words, mockRuleSet);
        assertEquals(expectedBarcode, actualBarcode);
    }

    @Test
    void wordsToBarcode_withStaticOr_usesFirstValue() {
        List<BarcodeSegmentRule> rulesWithStaticOr = new ArrayList<>();
        rulesWithStaticOr.add(new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true)); // apple
        rulesWithStaticOr.add(new BarcodeSegmentRule(1, 2, SegmentType.STATIC_OR, Arrays.asList("AA", "BB", "CC"), false));
        rulesWithStaticOr.add(new BarcodeSegmentRule(2, 4, SegmentType.NUMERIC, null, true)); // banana
        rulesWithStaticOr.add(new BarcodeSegmentRule(3, 3, SegmentType.BASE64, null, false));
        rulesWithStaticOr.add(new BarcodeSegmentRule(4, 4, SegmentType.NUMERIC, null, true)); // cherry
        rulesWithStaticOr.add(new BarcodeSegmentRule(5, 1, SegmentType.STATIC, "Y", false));
        rulesWithStaticOr.add(new BarcodeSegmentRule(6, 4, SegmentType.NUMERIC, null, true)); // date
        RuleSet ruleSetWithStaticOr = new RuleSet("staticOr-rules", rulesWithStaticOr);
        ruleSetWithStaticOr.validateRules();

        List<String> words = Arrays.asList("apple", "banana", "cherry", "date");
        // Expected: 0000 (apple) + AA (first from STATIC_OR) + 0001 (banana) + AAA (base64) + 0002 (cherry) + Y + 0003 (date)
        String expectedBarcode = "0000AA0001AAA0002Y0003";
        String actualBarcode = conversionService.wordsToBarcode(words, ruleSetWithStaticOr);
        assertEquals(expectedBarcode, actualBarcode);
    }


    @Test
    void barcodeToWords_success_withBase64Validation() {
        String barcode = "0000XX0001AAA0002Y0003"; // AAA is a valid Base64 placeholder for length 3
        List<String> expectedWords = Arrays.asList("apple", "banana", "cherry", "date");

        List<String> actualWords = conversionService.barcodeToWords(barcode, mockRuleSet);
        assertEquals(expectedWords, actualWords);
    }

    @Test
    void wordsToBarcode_invalidWordCount_throwsException() {
        List<String> words = Arrays.asList("apple", "banana"); // Only 2 words
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            conversionService.wordsToBarcode(words, mockRuleSet);
        });
        assertTrue(exception.getMessage().contains("Exactly 4 words are required"));
    }

    @Test
    void wordsToBarcode_wordNotFound_throwsException() {
        List<String> words = Arrays.asList("apple", "banana", "nonexistent", "date");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            conversionService.wordsToBarcode(words, mockRuleSet);
        });
        assertTrue(exception.getMessage().contains("Word not found in dictionary: nonexistent"));
    }

    @Test
    void barcodeToWords_invalidBarcodeLength_throwsException() {
        String shortBarcode = "123";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            conversionService.barcodeToWords(shortBarcode, mockRuleSet);
        });
        assertTrue(exception.getMessage().contains("does not match expected length"));
    }

    @Test
    void barcodeToWords_staticSegmentMismatch_throwsException() {
        // Correct: "0000XX0001AAA0002Y0003"
        String barcodeWithMismatch = "0000ZZ0001AAA0002Y0003"; // ZZ instead of XX
         Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            conversionService.barcodeToWords(barcodeWithMismatch, mockRuleSet);
        });
        assertTrue(exception.getMessage().contains("Static segment mismatch"));
    }

    @Test
    void barcodeToWords_staticOrSegment_success() {
        List<BarcodeSegmentRule> rules = new ArrayList<>();
        rules.add(new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true)); // apple
        rules.add(new BarcodeSegmentRule(1, 2, SegmentType.STATIC_OR, Arrays.asList("AA", "BB"), false));
        rules.add(new BarcodeSegmentRule(2, 4, SegmentType.NUMERIC, null, true)); // banana
        rules.add(new BarcodeSegmentRule(3, 1, SegmentType.STATIC, "S", false));
        rules.add(new BarcodeSegmentRule(4, 4, SegmentType.NUMERIC, null, true)); // cherry
        rules.add(new BarcodeSegmentRule(5, 1, SegmentType.STATIC, "T", false));
        rules.add(new BarcodeSegmentRule(6, 4, SegmentType.NUMERIC, null, true)); // date
        RuleSet staticOrRuleSet = new RuleSet("staticOr-test", rules);
        staticOrRuleSet.validateRules(); // Total length 4+2+4+1+4+1+4 = 20

        // Test with "AA"
        String barcode1 = "0000AA0001S0002T0003";
        List<String> expectedWords = Arrays.asList("apple", "banana", "cherry", "date");
        List<String> actualWords1 = conversionService.barcodeToWords(barcode1, staticOrRuleSet);
        assertEquals(expectedWords, actualWords1);

        // Test with "BB"
        String barcode2 = "0000BB0001S0002T0003";
        List<String> actualWords2 = conversionService.barcodeToWords(barcode2, staticOrRuleSet);
        assertEquals(expectedWords, actualWords2);
    }

    @Test
    void barcodeToWords_staticOrSegmentMismatch_throwsException() {
        List<BarcodeSegmentRule> rules = new ArrayList<>();
        rules.add(new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true));
        rules.add(new BarcodeSegmentRule(1, 2, SegmentType.STATIC_OR, Arrays.asList("AA", "BB"), false));
        rules.add(new BarcodeSegmentRule(2, 4, SegmentType.NUMERIC, null, true));
        rules.add(new BarcodeSegmentRule(3, 1, SegmentType.STATIC, "S", false));
        rules.add(new BarcodeSegmentRule(4, 4, SegmentType.NUMERIC, null, true));
        rules.add(new BarcodeSegmentRule(5, 1, SegmentType.STATIC, "T", false));
        rules.add(new BarcodeSegmentRule(6, 4, SegmentType.NUMERIC, null, true));
        RuleSet staticOrRuleSet = new RuleSet("staticOr-mismatch", rules);
        staticOrRuleSet.validateRules();

        String barcodeWithMismatch = "0000CC0001S0002T0003"; // CC is not in {"AA", "BB"}
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            conversionService.barcodeToWords(barcodeWithMismatch, staticOrRuleSet);
        });
        assertTrue(exception.getMessage().contains("does not match any of the allowed values"));
    }

    @Test
    void barcodeToWords_invalidBase64Segment_throwsException() {
        // Rule 3 is BASE64, length 3. "A==" is valid Base64 for one byte, but "A=" is not valid for length 3.
        // A valid base64 string of length 3 could be like "QUF" (encoding "AA")
        // An invalid one for "AAA" (length 3) would be "A!B"
        String barcodeWithInvalidBase64 = "0000XX0001A!B0002Y0003";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            conversionService.barcodeToWords(barcodeWithInvalidBase64, mockRuleSet);
        });
        assertTrue(exception.getMessage().contains("contains invalid Base64 characters"));
    }


    @Test
    void barcodeToWords_wordIndexOutOfBounds_throwsException() {
        // Word index for "apple" is 0000. If we use an index like 9999 (which is > 15 for our sample list)
        // The rule for word segment is length 4.
        // Expected: apple (0000), banana (0001), cherry (0002), date (0003)
        // Barcode:    idx | XX | idx  | AAA| idx  | Y | idx
        // Segments:   0   | 1  | 2    | 3  | 4    | 5 | 6
        //             4   | 2  | 4    | 3  | 4    | 1 | 4  = Total 22
        String barcode = "0000XX0001AAA0002Y0016"; // 0016 is out of bounds for a 16-word list (0-15)

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            conversionService.barcodeToWords(barcode, mockRuleSet);
        });
        assertTrue(exception.getMessage().contains("Invalid word index 16")); // Check for integer value in message
    }

    @Test
    void wordsToBarcode_wordIndexTooLongForSegment_throwsException() {
        // This test requires a rule that is too short for a valid word index.
        List<BarcodeSegmentRule> shortRules = new ArrayList<>();
        shortRules.add(new BarcodeSegmentRule(0, 1, SegmentType.NUMERIC, null, true)); // Word 1 (length 1)
        shortRules.add(new BarcodeSegmentRule(1, 1, SegmentType.STATIC, "X", false));
        shortRules.add(new BarcodeSegmentRule(2, 1, SegmentType.NUMERIC, null, true)); // Word 2 (length 1)
        shortRules.add(new BarcodeSegmentRule(3, 1, SegmentType.STATIC, "Y", false));
        shortRules.add(new BarcodeSegmentRule(4, 1, SegmentType.NUMERIC, null, true)); // Word 3 (length 1)
        shortRules.add(new BarcodeSegmentRule(5, 1, SegmentType.STATIC, "Z", false));
        shortRules.add(new BarcodeSegmentRule(6, 1, SegmentType.NUMERIC, null, true)); // Word 4 (length 1)
        RuleSet shortRuleSet = new RuleSet("short-rules", shortRules);
        shortRuleSet.validateRules();

        // "kiwi" has index 8, "lemon" has index 9. "mango" has index 10.
        // Index 10 ("mango") will be "10" which is length 2, but segment rule allows length 1.
        List<String> words = Arrays.asList("apple", "banana", "mango", "date");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            conversionService.wordsToBarcode(words, shortRuleSet);
        });
        assertTrue(exception.getMessage().contains("is too long for segment rule"));
        assertTrue(exception.getMessage().contains("Word index 10 for word 'mango'"));
    }
}
