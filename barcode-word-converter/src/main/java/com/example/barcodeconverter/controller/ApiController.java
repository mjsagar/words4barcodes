package com.example.barcodeconverter.controller;

import com.example.barcodeconverter.dto.ConversionRequest;
import com.example.barcodeconverter.dto.ConversionResponse;
import com.example.barcodeconverter.model.RuleSet;
import com.example.barcodeconverter.service.ConversionService;
import com.example.barcodeconverter.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ConversionService conversionService;
    private final RuleService ruleService;

    @Autowired
    public ApiController(ConversionService conversionService, RuleService ruleService) {
        this.conversionService = conversionService;
        this.ruleService = ruleService;
    }

    @PostMapping("/convert")
    public ResponseEntity<ConversionResponse> convert(@RequestBody ConversionRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(ConversionResponse.error("Request body is missing."));
        }

        String ruleSetName = request.getRuleSetName();
        if (ruleSetName == null || ruleSetName.trim().isEmpty()) {
            // Default to the first available ruleset name if none provided in request
            List<String> availableRuleSets = ruleService.getAllRuleSetNames();
            if (!availableRuleSets.isEmpty()) {
                ruleSetName = availableRuleSets.get(0);
                System.out.println("No ruleSetName provided in request, defaulting to: " + ruleSetName);
            } else {
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(ConversionResponse.error("No rule sets configured in the system."));
            }
        }

        RuleSet ruleSet = ruleService.getRuleSetByName(ruleSetName);

        if (ruleSet == null) {
            return ResponseEntity.badRequest().body(ConversionResponse.error("RuleSet with name '" + request.getRuleSetName() + "' not found."));
        }

        try {
            if (!ruleSet.isValidated()) {
                ruleSet.validateRules(); // Ensure it's validated before use
            }
        } catch (IllegalStateException e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(ConversionResponse.error("Error with selected RuleSet '" + ruleSetName + "': " + e.getMessage()));
        }


        try {
            if (request.getBarcode() != null && !request.getBarcode().trim().isEmpty()) {
                // Barcode to Words
                List<String> words = conversionService.barcodeToWords(request.getBarcode(), ruleSet);
                return ResponseEntity.ok(ConversionResponse.successWords(words));
            } else if (request.getWords() != null && !request.getWords().isEmpty()) {
                // Words to Barcode
                if (request.getWords().size() != 4) {
                    return ResponseEntity.badRequest().body(ConversionResponse.error("Exactly 4 words are required for conversion to barcode."));
                }
                String barcode = conversionService.wordsToBarcode(request.getWords(), ruleSet);
                return ResponseEntity.ok(ConversionResponse.successBarcode(barcode));
            } else {
                return ResponseEntity.badRequest().body(ConversionResponse.error("Either 'barcode' or 'words' must be provided in the request."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ConversionResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            // This might catch issues from RuleSet validation failures if not caught earlier, or other service layer state issues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(ConversionResponse.error("Processing error: " + e.getMessage()));
        } catch (Exception e) {
            // Catch-all for unexpected errors
            System.err.println("Unexpected error during conversion: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(ConversionResponse.error("An unexpected error occurred. Please check server logs."));
        }
    }
}
