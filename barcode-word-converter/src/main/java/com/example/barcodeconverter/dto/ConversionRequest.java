package com.example.barcodeconverter.dto;

import java.util.List;

public class ConversionRequest {
    private String barcode;
    private List<String> words; // Expecting 4 words
    private String ruleSetName; // To identify which RuleSet to use

    // Getters and Setters
    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public String getRuleSetName() {
        return ruleSetName;
    }

    public void setRuleSetName(String ruleSetName) {
        this.ruleSetName = ruleSetName;
    }

    @Override
    public String toString() {
        return "ConversionRequest{" +
               "barcode='" + barcode + '\'' +
               ", words=" + words +
               ", ruleSetName='" + ruleSetName + '\'' +
               '}';
    }
}
