package com.example.barcodeconverter.service;

import com.example.barcodeconverter.model.BarcodeSegmentRule;
import com.example.barcodeconverter.model.RuleSet;
import com.example.barcodeconverter.model.SegmentType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RuleService {

    private final Map<String, RuleSet> ruleSets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String RULES_FILE_NAME = "rules.json";

    // In a real scenario, you might make this path configurable
    // For ClassPathResource, it's relative to 'classes' root or classpath root.
    private final String rulesJsonPath = RULES_FILE_NAME;


    @PostConstruct
    public void init() {
        loadRuleSets();
        if (ruleSets.isEmpty()) {
            System.out.println("No rulesets loaded from " + rulesJsonPath + ". Creating a default one.");
            createDefaultRuleSetFile();
            loadRuleSets(); // Try loading again after creating the default
        }
    }

    private void loadRuleSets() {
        try {
            Resource resource = new ClassPathResource(rulesJsonPath);
            if (!resource.exists()) {
                System.out.println(rulesJsonPath + " not found on classpath. No rulesets loaded.");
                return;
            }

            try (InputStream inputStream = resource.getInputStream();
                 Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                String jsonContent = FileCopyUtils.copyToString(reader);

                List<RuleSet> loadedList = objectMapper.readValue(jsonContent, new TypeReference<List<RuleSet>>() {});
                ruleSets.clear(); // Clear existing before loading new ones
                for (RuleSet rs : loadedList) {
                    try {
                        // Ensure rules are sorted and DTOs are properly converted to domain objects if needed
                        // For now, assuming BarcodeSegmentRule and SegmentType deserialize correctly.
                        // The RuleSet constructor now sorts and validates.
                        RuleSet validatedRuleSet = new RuleSet(rs.getName(), rs.getRules());
                        ruleSets.put(validatedRuleSet.getName(), validatedRuleSet);
                        System.out.println("Successfully loaded and validated RuleSet: " + validatedRuleSet.getName() + " with total length " + validatedRuleSet.getTotalBarcodeLength());
                    } catch (IllegalStateException e) {
                        System.err.println("Error validating RuleSet '" + rs.getName() + "': " + e.getMessage() + ". Skipping this ruleset.");
                    } catch (Exception e) {
                        System.err.println("Unexpected error processing RuleSet '" + rs.getName() + "': " + e.getMessage() + ". Skipping this ruleset.");
                        e.printStackTrace();
                    }
                }
                System.out.println("Finished loading " + ruleSets.size() + " rulesets from " + rulesJsonPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to load rulesets from " + rulesJsonPath + ": " + e.getMessage());
            // e.printStackTrace(); // Optionally print stack trace for debugging
        }
    }

    private void createDefaultRuleSetFile() {
        try {
            // Path to src/main/resources for writing during development.
            // This is tricky because files in src/main/resources are packaged into the JAR
            // and are not typically writable at runtime in a deployed app.
            // For initial setup, we can try to write it if running in an environment
            // where src/main/resources is accessible as a file system path.
            Path path = Paths.get("src", "main", "resources", RULES_FILE_NAME);

            if (Files.exists(path)) {
                System.out.println("Default rules file already exists at: " + path.toString());
                return;
            }

            Files.createDirectories(path.getParent());

            List<BarcodeSegmentRule> rulesFor20Char = new ArrayList<>();
            rulesFor20Char.add(new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true)); // Word 1
            rulesFor20Char.add(new BarcodeSegmentRule(1, 1, SegmentType.STATIC, "T", false));
            rulesFor20Char.add(new BarcodeSegmentRule(2, 4, SegmentType.NUMERIC, null, true)); // Word 2
            rulesFor20Char.add(new BarcodeSegmentRule(3, 1, SegmentType.STATIC, "E", false));
            rulesFor20Char.add(new BarcodeSegmentRule(4, 4, SegmentType.NUMERIC, null, true)); // Word 3
            rulesFor20Char.add(new BarcodeSegmentRule(5, 1, SegmentType.STATIC, "S", false));
            rulesFor20Char.add(new BarcodeSegmentRule(6, 4, SegmentType.NUMERIC, null, true)); // Word 4
            rulesFor20Char.add(new BarcodeSegmentRule(7, 1, SegmentType.STATIC, "T", false));

            RuleSet defaultRuleSet = new RuleSet("default-20char", rulesFor20Char);
            // No need to validate here as it's validated on construction of RuleSet object.

            List<RuleSet> defaultRuleSets = Collections.singletonList(defaultRuleSet);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), defaultRuleSets);

            System.out.println("Created default " + RULES_FILE_NAME + " at " + path.toString());

        } catch (IOException e) {
            System.err.println("Could not create default " + RULES_FILE_NAME + ": " + e.getMessage());
            // e.printStackTrace();
        }
    }


    public RuleSet getRuleSetByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            if (!ruleSets.isEmpty()) {
                 System.out.println("RuleSetName is null/empty, attempting to use first available ruleset.");
                 // Provide a consistent default if multiple are loaded, e.g., sorted by name
                 return ruleSets.values().stream().min(java.util.Comparator.comparing(RuleSet::getName)).orElse(null);
            }
            return null;
        }
        return ruleSets.get(name);
    }

    public List<String> getAllRuleSetNames() {
        List<String> names = new ArrayList<>(ruleSets.keySet());
        Collections.sort(names);
        return names;
    }

    // This method would be more complex in a real app, needing to write back to the rules.json
    // It also needs to handle potential concurrent access if rules can be modified at runtime.
    public synchronized void saveRuleSet(RuleSet ruleSetToSave) throws IOException {
        if (ruleSetToSave == null || ruleSetToSave.getName() == null || ruleSetToSave.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("RuleSet and its name cannot be null or empty.");
        }

        // Validate before saving
        try {
            ruleSetToSave.validateRules();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("RuleSet is invalid: " + e.getMessage(), e);
        }

        // Load existing rules
        List<RuleSet> currentRuleSets = new ArrayList<>();
        Resource resource = new ClassPathResource(rulesJsonPath);
        if (resource.exists()) {
             try (InputStream inputStream = resource.getInputStream();
                 Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                String jsonContent = FileCopyUtils.copyToString(reader);
                currentRuleSets = objectMapper.readValue(jsonContent, new TypeReference<List<RuleSet>>() {});
            } catch (IOException e) {
                // If we can't read, maybe we can't write either, or we start fresh
                System.err.println("Could not read existing rules.json to update. Will attempt to overwrite. Error: " + e.getMessage());
                currentRuleSets = new ArrayList<>(); // Start fresh if read fails but allow overwrite
            }
        }

        // Remove old version if it exists, then add new/updated one
        currentRuleSets.removeIf(rs -> rs.getName().equals(ruleSetToSave.getName()));
        currentRuleSets.add(ruleSetToSave);

        // This writes to the target/classes directory if running from Maven, or project root.
        // For a deployed app, this needs a persistent, writable location.
        // For development, writing to src/main/resources is okay but won't reflect in classpath until rebuild.
        try {
            Path path = Paths.get("src", "main", "resources", RULES_FILE_NAME);
             if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), currentRuleSets);
            System.out.println("Successfully saved rules to " + path.toString());

            // Reload rules in memory
            loadRuleSets();

        } catch (IOException e) {
            System.err.println("Failed to save rules to " + RULES_FILE_NAME + ": " + e.getMessage());
            throw e;
        }
    }
}
