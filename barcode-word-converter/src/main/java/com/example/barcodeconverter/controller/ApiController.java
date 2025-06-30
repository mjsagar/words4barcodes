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

        String requestedRuleSetName = request.getRuleSetName();
        RuleSet ruleSet;

        if (requestedRuleSetName == null || requestedRuleSetName.trim().isEmpty()) {
            System.out.println("No ruleSetName provided in request, attempting to use default from RuleService.");
            // RuleService's getRuleSetByName handles null/empty by returning a default if available
            ruleSet = ruleService.getRuleSetByName(null);
            if (ruleSet == null) {
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(ConversionResponse.error("No default rule set configured or no rule sets available in the system."));
            }
            System.out.println("Defaulting to RuleSet: " + ruleSet.getName());
        } else {
            ruleSet = ruleService.getRuleSetByName(requestedRuleSetName);
            if (ruleSet == null) {
                return ResponseEntity.badRequest().body(ConversionResponse.error("RuleSet with name '" + requestedRuleSetName + "' not found."));
            }
        }

        // RuleSets loaded by RuleService should already be validated.
        // An explicit call to ruleSet.validateRules() here is redundant and potentially problematic
        // if the RuleSet instance is shared and validateRules() is not idempotent or has side effects
        // beyond setting a flag. Our RuleSet.validateRules() is idempotent if already validated.
        // However, it's better to rely on the service layer to provide validated objects.
        // If a RuleSet from RuleService is not validated, it's an issue in RuleService's loading logic.
        if (!ruleSet.isValidated()) {
            // This case should ideally not happen if RuleService guarantees validated RuleSets.
            // If it can happen (e.g., rules are modified externally), then re-validation might be needed.
            // For now, we assume RuleService provides validated RuleSets.
            // If validation was strictly necessary here, it should be handled carefully.
            // For example, logging a warning or even an error if a non-validated ruleset is received.
            System.err.println("Warning: RuleSet '" + ruleSet.getName() + "' obtained from RuleService is not marked as validated. This might indicate an issue in RuleService's initialization.");
            // Depending on policy, could either try to validate it now, or reject.
            // try {
            //     ruleSet.validateRules();
            // } catch (IllegalStateException e) {
            //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            //                   .body(ConversionResponse.error("Error re-validating RuleSet '" + ruleSet.getName() + "': " + e.getMessage()));
            // }
            // For this implementation, we will trust that RuleService.getRuleSetByName returns a validated RuleSet
            // or a RuleSet that will throw an error during its use in ConversionService if it's fundamentally broken
            // and was not caught by RuleService's initial validation.
        }

        try {
            // The actual ruleSetName used, especially if a default was applied.
            String effectiveRuleSetName = ruleSet.getName();

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
