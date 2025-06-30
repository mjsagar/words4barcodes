package com.example.barcodeconverter.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RuleSet {
    private final String name;
    private final List<BarcodeSegmentRule> rules;
    private boolean validated = false;
    private int totalBarcodeLength = 0;
    private static final int EXPECTED_WORD_MAPPED_RULES = 4;

    public RuleSet(String name, List<BarcodeSegmentRule> rules) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("RuleSet name cannot be null or empty.");
        }
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("RuleSet must contain at least one rule.");
        }
        this.name = name;
        // Create a mutable copy and check for null rules before sorting
        this.rules = new ArrayList<>(rules.size());
        for (BarcodeSegmentRule rule : rules) {
            if (rule == null) {
                throw new IllegalArgumentException("Rule list cannot contain null BarcodeSegmentRule objects.");
            }
            this.rules.add(rule);
        }
        // Sort rules by order to simplify validation and processing
        this.rules.sort(Comparator.comparingInt(BarcodeSegmentRule::getOrder));
    }

    public String getName() {
        return name;
    }

    public List<BarcodeSegmentRule> getRules() {
        return rules;
    }

    public boolean isValidated() {
        return validated;
    }

    public int getTotalBarcodeLength() {
        if (!validated) {
            // Or throw new IllegalStateException("RuleSet must be validated before getting total barcode length.");
            // For now, let's allow calculation but it's better if validated first.
            calculateTotalLength();
        }
        return totalBarcodeLength;
    }

    private void calculateTotalLength() {
        this.totalBarcodeLength = rules.stream().mapToInt(BarcodeSegmentRule::getLength).sum();
    }

    public void validateRules() throws IllegalStateException {
        if (validated) {
            return; // Already validated
        }

        Set<Integer> orders = new HashSet<>();
        long wordMappedRulesCount = 0;
        int expectedOrder = rules.get(0).getOrder(); // Assuming rules are sorted

        for (BarcodeSegmentRule rule : rules) {
            if (rule == null) {
                throw new IllegalStateException("RuleSet contains a null rule.");
            }

            // Check for duplicate order numbers
            if (!orders.add(rule.getOrder())) {
                throw new IllegalStateException("Duplicate order number found: " + rule.getOrder() + " in RuleSet '" + name + "'.");
            }

            // Check for sequential order numbers (assumes rules are pre-sorted by order)
            if (rule.getOrder() != expectedOrder) {
                throw new IllegalStateException("Non-sequential order number found. Expected " + expectedOrder +
                                                ", but got " + rule.getOrder() + " in RuleSet '" + name + "'.");
            }
            expectedOrder++;

            if (rule.isMapsToWord()) {
                if (rule.getType() != SegmentType.NUMERIC) {
                    throw new IllegalStateException("Rule (order " + rule.getOrder() + ") maps to a word but is not of type NUMERIC in RuleSet '" + name + "'.");
                }
                wordMappedRulesCount++;
            }
        }

        // Check if the number of word-mapped rules is exactly EXPECTED_WORD_MAPPED_RULES
        if (wordMappedRulesCount != EXPECTED_WORD_MAPPED_RULES) {
            throw new IllegalStateException("RuleSet '" + name + "' must have exactly " + EXPECTED_WORD_MAPPED_RULES +
                                            " rules that map to a word (mapsToWord=true). Found " + wordMappedRulesCount + ".");
        }

        // Calculate total length
        calculateTotalLength();
        if (this.totalBarcodeLength <= 0) {
             throw new IllegalStateException("Total barcode length must be positive for RuleSet '" + name + "'.");
        }

        this.validated = true;
    }

    // Optional: If rules can be added dynamically after construction (not typical for this setup)
    // public void addRule(BarcodeSegmentRule rule) {
    //     if (rule == null) {
    //         throw new IllegalArgumentException("Cannot add a null rule.");
    //     }
    //     this.rules.add(rule);
    //     this.rules.sort(Comparator.comparingInt(BarcodeSegmentRule::getOrder)); // Re-sort
    //     this.validated = false; // Mark as not validated after modification
    //     this.totalBarcodeLength = 0;
    // }

    @Override
    public String toString() {
        return "RuleSet{" +
               "name='" + name + '\'' +
               ", rules=" + rules.size() + " rules" +
               ", validated=" + validated +
               ", totalBarcodeLength=" + totalBarcodeLength +
               '}';
    }
}
