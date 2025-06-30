package com.example.barcodeconverter.controller;

import com.example.barcodeconverter.dto.ConversionRequest;
import com.example.barcodeconverter.model.BarcodeSegmentRule;
import com.example.barcodeconverter.model.RuleSet;
import com.example.barcodeconverter.model.SegmentType;
import com.example.barcodeconverter.service.RuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.junit.jupiter.api.Assertions.*; // Added this line
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // For converting objects to JSON strings

    @SpyBean // Spy on RuleService to control its behavior if needed, but allow real methods too
    private RuleService ruleService;

    private static final String DEFAULT_RULE_SET_NAME = "test-default-integration";

    @BeforeEach
    void setUp() throws IOException {
        // Ensure a known rules.json exists for testing or mock its loading
        // For integration tests, it's often better to have a controlled test rules.json
        // or ensure RuleService is set up in a predictable way.

        List<BarcodeSegmentRule> rules = new ArrayList<>();
        rules.add(new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, (String) null, true)); // apple
        rules.add(new BarcodeSegmentRule(1, 2, SegmentType.STATIC, "IT", false));
        rules.add(new BarcodeSegmentRule(2, 4, SegmentType.NUMERIC, (String) null, true)); // banana
        rules.add(new BarcodeSegmentRule(3, 3, SegmentType.BASE64, (String) null, false)); // Placeholder "AAA"
        rules.add(new BarcodeSegmentRule(4, 4, SegmentType.NUMERIC, (String) null, true)); // cherry
        rules.add(new BarcodeSegmentRule(5, 1, SegmentType.STATIC, "Z", false));
        rules.add(new BarcodeSegmentRule(6, 4, SegmentType.NUMERIC, (String) null, true)); // date

        RuleSet testRuleSet = new RuleSet(DEFAULT_RULE_SET_NAME, rules);
        testRuleSet.validateRules(); // Validate it

        // Instead of writing to a file, we can ensure RuleService returns this specific ruleset
        // when getRuleSetByName is called with DEFAULT_RULE_SET_NAME.
        // This avoids file system dependencies during test execution if RuleService loads from file.
        // If RuleService always loads from file, we need to ensure the file is present.
        // The current RuleService creates a default if none exists, which is good.
        // Let's ensure our test rule set is available.
        // We can use @SpyBean to override the getRuleSetByName for our specific test rule set name.
        doReturn(testRuleSet).when(ruleService).getRuleSetByName(DEFAULT_RULE_SET_NAME);

        // Also ensure getAllRuleSetNames includes our test ruleset if the controller defaults to the first one.
        List<String> names = new ArrayList<>(ruleService.getAllRuleSetNames());
        if (!names.contains(DEFAULT_RULE_SET_NAME)) {
            names.add(DEFAULT_RULE_SET_NAME);
        }
        doReturn(names).when(ruleService).getAllRuleSetNames();


        // The actual words.txt will be loaded by ConversionService.
        // We need to ensure the words we use are in src/main/resources/words.txt
        // For this test, "that", "this", "have", "with" should be in the generated words.txt
    }

    @Test
    void convertWordsToBarcode_success() throws Exception {
        ConversionRequest request = new ConversionRequest();
        // Using words known to be in the generated words.txt
        request.setWords(Arrays.asList("that", "this", "have", "with"));
        request.setRuleSetName(DEFAULT_RULE_SET_NAME);

        // "that" is 0, "this" is 1, "have" is 2, "with" is 3 in a 0-indexed list of first few words >3chars.
        // The actual words.txt has "that" as the first word (index 0).
        // "this" is index 3. "have" is index 6. "with" is index 2.
        // Check words.txt: that(0), this(1), with(2), from(3), your(4), have(5), more(6), will(7)...
        // Words for test: "that", "this", "have", "with"
        // Indices: 0, 1, 5, 2
        // Expected barcode: 0000 (that) + IT + 0001 (this) + AAA (base64) + 0005 (have) + Z + 0002 (with)
        String expectedBarcode = "0000IT0001AAA0005Z0002";

        mockMvc.perform(post("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.barcode", is(expectedBarcode)));
    }

    @Test
    void convertBarcodeToWords_success() throws Exception {
        ConversionRequest request = new ConversionRequest();
        // This barcode corresponds to indices: 0, 1, 5, 2 based on the ruleSet
        // Indices: 0 ("that"), 1 ("this"), 5 ("have"), 2 ("with")
        request.setBarcode("0000IT0001AAA0005Z0002");
        request.setRuleSetName(DEFAULT_RULE_SET_NAME);

        List<String> expectedWords = Arrays.asList("that", "this", "have", "with");

        mockMvc.perform(post("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.words", hasSize(4)))
                .andExpect(jsonPath("$.words[0]", is(expectedWords.get(0))))
                .andExpect(jsonPath("$.words[1]", is(expectedWords.get(1))))
                .andExpect(jsonPath("$.words[2]", is(expectedWords.get(2))))
                .andExpect(jsonPath("$.words[3]", is(expectedWords.get(3))));
    }

    @Test
    void convert_missingRuleSetName_usesDefaultAndSucceeds() throws Exception {
        // This test relies on the default "default-20char" ruleset.
        // Its structure is: 4N-1S-4N-1S-4N-1S-4N-1S, total length 20. Static values are T, E, S, T.
        // Words from words.txt (0-indexed):
        // that (0), this (1), with (2), from (3), your (4), have (5), more (6), will (7)
        // acid (8), also (9), anil (10), aqua (11), area (12), army (13), atom (14), away (15)

        // Let's use barcode: "0000T0003E0006S0011T"
        // Indices: 0, 3, 6, 11
        // Expected words: "that", "from", "more", "aqua"

        // Ensure RuleService.getRuleSetByName(null) returns the "default-20char" ruleset.
        // The RuleService's init method should load or create "default-20char".
        // We don't need to mock getAllRuleSetNames specifically for this, as ApiController
        // calls getRuleSetByName(null) which has its own default logic.

        RuleSet defaultRuleSetFromService = ruleService.getRuleSetByName(null); // This should be "default-20char"
        assertNotNull(defaultRuleSetFromService, "RuleService did not provide a default RuleSet.");
        assertEquals("default-20char", defaultRuleSetFromService.getName(), "Default RuleSet name mismatch.");
        assertEquals(21, defaultRuleSetFromService.getTotalBarcodeLength(), "Default RuleSet total length mismatch.");


        ConversionRequest request = new ConversionRequest();
        // Default RuleSet structure: N4-S1("T")-N4-SOR1("E")-N4-B64(2,"AA")-N4-S1("T") Total=21
        // Words: "that" (0), "this" (1), "with" (2), "from" (3)
        // Barcode: 0000 T 0001 E 0002 AA 0003 T
        request.setBarcode("0000T0001E0002AA0003T");
        // No ruleSetName set in request, so ApiController should ask RuleService for default.

        List<String> expectedWords = Arrays.asList("that", "this", "with", "from");

        mockMvc.perform(post("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.words", hasSize(4)))
                .andExpect(jsonPath("$.words[0]", is(expectedWords.get(0))))
                .andExpect(jsonPath("$.words[1]", is(expectedWords.get(1))))
                .andExpect(jsonPath("$.words[2]", is(expectedWords.get(2))))
                .andExpect(jsonPath("$.words[3]", is(expectedWords.get(3))));
    }


    @Test
    void convert_invalidInput_returnsBadRequest() throws Exception {
        ConversionRequest request = new ConversionRequest(); // Empty request
        request.setRuleSetName(DEFAULT_RULE_SET_NAME);

        mockMvc.perform(post("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Either 'barcode' or 'words' must be provided in the request.")));
    }

    @Test
    void convertWordsToBarcode_wrongNumberOfWords_returnsBadRequest() throws Exception {
        ConversionRequest request = new ConversionRequest();
        request.setWords(Arrays.asList("one", "two", "three")); // Only 3 words
        request.setRuleSetName(DEFAULT_RULE_SET_NAME);

        mockMvc.perform(post("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Exactly 4 words are required for conversion to barcode.")));
    }

    @Test
    void convert_unknownRuleSetName_returnsBadRequest() throws Exception {
        ConversionRequest request = new ConversionRequest();
        request.setBarcode("somebarcode");
        request.setRuleSetName("nonExistentRuleSet");

        mockMvc.perform(post("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("RuleSet with name 'nonExistentRuleSet' not found.")));
    }
}
