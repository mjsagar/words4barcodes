package com.example.barcodeconverter.service;

import com.example.barcodeconverter.model.BarcodeSegmentRule;
import com.example.barcodeconverter.model.RuleSet;
import com.example.barcodeconverter.model.SegmentType;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct; // Correct import for @PostConstruct

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class ConversionService {

    private List<String> wordList;
    private Map<String, Integer> wordToIndexMap;
    private static final String WORD_LIST_PATH = "/words.txt"; // Path in resources
    private static final int EXPECTED_WORD_COUNT_FOR_CONVERSION = 4;

    @PostConstruct
    public void init() throws IOException {
        wordList = new ArrayList<>();
        wordToIndexMap = new HashMap<>();

        System.out.println("Loading word list from: " + WORD_LIST_PATH);
        try (InputStream inputStream = ConversionService.class.getResourceAsStream(WORD_LIST_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            if (inputStream == null) {
                System.err.println("Word list file not found at " + WORD_LIST_PATH);
                throw new IOException("Word list file not found at " + WORD_LIST_PATH);
            }
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                String trimmedWord = line.trim();
                if (!trimmedWord.isEmpty()) {
                    wordList.add(trimmedWord);
                    wordToIndexMap.put(trimmedWord, index++);
                }
            }
            System.out.println("Successfully loaded " + wordList.size() + " words into memory.");
        } catch (IOException e) {
            System.err.println("Failed to load word list: " + e.getMessage());
            throw e; // Re-throw to indicate service initialization failure
        } catch (NullPointerException e) {
            // This can happen if getResourceAsStream returns null
             System.err.println("NullPointerException while trying to load word list. Check path: " + WORD_LIST_PATH);
            throw new IOException("Word list file not found at " + WORD_LIST_PATH + ". InputStream was null.", e);
        }
    }

    public String wordsToBarcode(List<String> words, RuleSet ruleSet) {
        if (words == null || words.size() != EXPECTED_WORD_COUNT_FOR_CONVERSION) {
            throw new IllegalArgumentException("Exactly " + EXPECTED_WORD_COUNT_FOR_CONVERSION + " words are required.");
        }
        if (ruleSet == null) {
            throw new IllegalArgumentException("RuleSet cannot be null.");
        }
        if (!ruleSet.isValidated()) {
            ruleSet.validateRules(); // Ensure rules are valid before use
        }

        List<BarcodeSegmentRule> wordMappingRules = ruleSet.getRules().stream()
            .filter(BarcodeSegmentRule::isMapsToWord)
            .collect(Collectors.toList());

        if (wordMappingRules.size() != EXPECTED_WORD_COUNT_FOR_CONVERSION) {
            throw new IllegalStateException("RuleSet is not configured for " + EXPECTED_WORD_COUNT_FOR_CONVERSION + " word mappings.");
        }

        Map<Integer, String> barcodeParts = new HashMap<>();
        int wordInputIndex = 0;
        for (BarcodeSegmentRule rule : wordMappingRules) {
            String currentWord = words.get(wordInputIndex++);
            Integer wordIndex = wordToIndexMap.get(currentWord);
            if (wordIndex == null) {
                throw new IllegalArgumentException("Word not found in dictionary: " + currentWord);
            }
            // Format the index to match the segment length, padding with leading zeros
            String formattedIndex = String.format("%0" + rule.getLength() + "d", wordIndex);
            if (formattedIndex.length() > rule.getLength()) {
                 throw new IllegalArgumentException("Word index " + wordIndex + " for word '" + currentWord +
                                                   "' is too long for segment rule: " + rule);
            }
            barcodeParts.put(rule.getOrder(), formattedIndex);
        }

        StringBuilder barcodeBuilder = new StringBuilder();
        for (BarcodeSegmentRule rule : ruleSet.getRules()) {
            if (barcodeParts.containsKey(rule.getOrder())) {
                barcodeBuilder.append(barcodeParts.get(rule.getOrder()));
            } else {
                // Handle non-word-mapped segments
                switch (rule.getType()) {
                    case STATIC:
                        barcodeBuilder.append(rule.getStaticValue());
                        break;
                    case NUMERIC:
                        // This numeric segment is not from a word. It needs a source or default.
                        // For now, let's fill with zeros as a placeholder. This needs refinement.
                        barcodeBuilder.append("0".repeat(rule.getLength()));
                        // Consider throwing an error if no source for this data
                        // throw new IllegalStateException("Numeric segment not mapped to a word and no value provided: " + rule);
                        break;
                    case BASE64:
                        // Similar to NUMERIC, needs a source of data to encode.
                        // Placeholder: fill with Base64 of zeros. This also needs refinement.
                        // e.g. Base64 encode a string of '0's of appropriate length to fill rule.getLength()
                        // This is tricky because Base64 encoding changes length.
                        // For now, let's use a placeholder that fits. 'AAAA...'
                        if (rule.getLength() > 0) {
                            // Create a placeholder byte array. The content doesn't matter much for this placeholder.
                            // The length of the byte array should be such that its Base64 representation fits rule.getLength().
                            // Base64 encoding: 3 bytes -> 4 chars.
                            // So, numBytes = ceil(rule.getLength() * 3 / 4.0)
                            // However, for a generic placeholder, we can just use 'A's.
                            // This is not a robust solution for actual data.
                             String placeholderBase64 = "A".repeat(rule.getLength());
                             barcodeBuilder.append(placeholderBase64);

                        }
                        // throw new IllegalStateException("Base64 segment not mapped to a word and no value provided: " + rule);
                        break;
                    default:
                        throw new IllegalStateException("Unhandled segment type: " + rule.getType());
                }
            }
        }
        return barcodeBuilder.toString();
    }

    public List<String> barcodeToWords(String barcode, RuleSet ruleSet) {
        if (barcode == null || barcode.isEmpty()) {
            throw new IllegalArgumentException("Barcode cannot be null or empty.");
        }
        if (ruleSet == null) {
            throw new IllegalArgumentException("RuleSet cannot be null.");
        }
        if (!ruleSet.isValidated()) {
            ruleSet.validateRules(); // Ensure rules are valid
        }

        if (barcode.length() != ruleSet.getTotalBarcodeLength()) {
            throw new IllegalArgumentException("Barcode length (" + barcode.length() +
                ") does not match expected length from RuleSet (" + ruleSet.getTotalBarcodeLength() + ").");
        }

        List<String> resultWords = new ArrayList<>();
        int currentPos = 0;

        List<BarcodeSegmentRule> wordMappingRules = ruleSet.getRules().stream()
            .filter(BarcodeSegmentRule::isMapsToWord)
            .collect(Collectors.toList());

        if (wordMappingRules.isEmpty()) {
            throw new IllegalStateException("RuleSet does not define any segments that map to words.");
        }

        // Iterate through all rules to parse the barcode, but only extract words from mapped segments
        for (BarcodeSegmentRule rule : ruleSet.getRules()) {
            if (currentPos + rule.getLength() > barcode.length()) {
                throw new IllegalArgumentException("Barcode is too short to process rule: " + rule +
                                                   ". Current position: " + currentPos);
            }
            String segmentValue = barcode.substring(currentPos, currentPos + rule.getLength());
            currentPos += rule.getLength();

            if (rule.isMapsToWord()) {
                if (rule.getType() != SegmentType.NUMERIC) {
                     throw new IllegalStateException("Rule misconfiguration: Segment " + rule.getOrder() + " maps to word but is not NUMERIC.");
                }
                try {
                    int wordIndex = Integer.parseInt(segmentValue);
                    if (wordIndex < 0 || wordIndex >= wordList.size()) {
                        throw new IllegalArgumentException("Invalid word index " + wordIndex +
                                                           " extracted from segment (order " + rule.getOrder() +
                                                           "). Out of bounds for word list size " + wordList.size());
                    }
                    resultWords.add(wordList.get(wordIndex));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Segment (order " + rule.getOrder() +
                                                       ") marked for word mapping does not contain a valid number: " + segmentValue);
                }
            } else {
                // For non-word-mapped segments, we might still want to validate them
                switch (rule.getType()) {
                    case STATIC:
                        if (!segmentValue.equals(rule.getStaticValue())) {
                            throw new IllegalArgumentException("Static segment mismatch for rule (order " + rule.getOrder() +
                                                               "). Expected '" + rule.getStaticValue() + "' but got '" + segmentValue + "'.");
                        }
                        break;
                    case NUMERIC:
                        if (!segmentValue.matches("\\d+")) {
                            throw new IllegalArgumentException("Numeric segment (order " + rule.getOrder() +
                                                               ") contains non-numeric characters: '" + segmentValue + "'.");
                        }
                        break;
                    case BASE64:
                        // Validate if it's a valid Base64 string of the correct length
                        // This is a basic check. More robust validation might be needed depending on requirements.
                        try {
                            Base64.getDecoder().decode(segmentValue);
                        } catch (IllegalArgumentException e) {
                             throw new IllegalArgumentException("Base64 segment (order " + rule.getOrder() +
                                                               ") contains invalid Base64 characters: '" + segmentValue + "'.");
                        }
                        break;
                }
            }
        }

        if (resultWords.size() != EXPECTED_WORD_COUNT_FOR_CONVERSION) {
             // This should ideally be caught by RuleSet validation, but as a safeguard:
            System.err.println("Warning: Extracted " + resultWords.size() + " words, but expected " + EXPECTED_WORD_COUNT_FOR_CONVERSION +
                               " based on RuleSet configuration.");
            // Depending on strictness, could throw an error here.
        }

        return resultWords;
    }
}
