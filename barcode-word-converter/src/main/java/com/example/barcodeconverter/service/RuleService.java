package com.example.barcodeconverter.service;

import com.example.barcodeconverter.model.BarcodeSegmentRule;
import com.example.barcodeconverter.model.RuleSet;
import com.example.barcodeconverter.model.SegmentType;
// Removed duplicate org.springframework.stereotype.Service import
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
// import org.springframework.beans.factory.annotation.Value; // Not used
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service; // Keep this one
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
// import java.util.HashMap; // Not directly used by RuleService itself, map is ConcurrentHashMap
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
    private final String rulesJsonPath = RULES_FILE_NAME; // This is correct for ClassPathResource

    @PostConstruct
    public void init() {
        loadRuleSets();
        if (ruleSets.isEmpty()) {
            System.out.println("No rulesets loaded from '" + rulesJsonPath + "'. Creating a default one.");
            // Attempt to create in src/main/resources, which might only work in dev environments
            createDefaultRuleSetFileIfNotExistsInResources();
            loadRuleSets(); // Try loading again
        }
    }

    private void loadRuleSets() {
        try {
            Resource resource = new ClassPathResource(rulesJsonPath);
            if (!resource.exists()) {
                System.out.println("'" + rulesJsonPath + "' not found on classpath. No rulesets will be loaded.");
                return;
            }

            // Define a more specific type for deserialization if RuleSet needs special handling for its List<BarcodeSegmentRule>
            // However, Jackson should be able to handle List<BarcodeSegmentRule> if BarcodeSegmentRule is a valid bean.
            // The key is that the JSON structure must match what Jackson expects for RuleSet and BarcodeSegmentRule.
            // Specifically, BarcodeSegmentRule needs getters and setters or a constructor Jackson can use.
            // Our BarcodeSegmentRule has getters/setters and a no-arg constructor, plus an all-args constructor.
            // Jackson might need @JsonCreator for the all-args constructor if no-arg + setters is not preferred.
            // For RuleSet, it has a constructor RuleSet(String name, List<BarcodeSegmentRule> rules).
            // Jackson will need to know how to map JSON fields to these constructor arguments.
            // This typically requires @JsonProperty annotations on constructor parameters or matching field names.
            // Let's assume the JSON field names match "name" and "rules".

            List<Map<String, Object>> rawRuleSetList;
            try (InputStream inputStream = resource.getInputStream()) {
                 rawRuleSetList = objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
            }

            ruleSets.clear(); // Clear existing before loading new ones

            for (Map<String, Object> rawRuleSet : rawRuleSetList) {
                String name = (String) rawRuleSet.get("name");
                @SuppressWarnings("unchecked") // Jackson deserializes to List<Map<...>> for complex objects in list
                List<Map<String, Object>> rawRules = (List<Map<String, Object>>) rawRuleSet.get("rules");

                if (name == null || rawRules == null) {
                    System.err.println("Skipping a ruleset due to missing name or rules field.");
                    continue;
                }

                List<BarcodeSegmentRule> rules = new ArrayList<>();
                for (Map<String, Object> rawRule : rawRules) {
                    try {
                        // Manually map or use objectMapper.convertValue for each rule
                        BarcodeSegmentRule rule = objectMapper.convertValue(rawRule, BarcodeSegmentRule.class);
                        rules.add(rule);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error converting raw rule to BarcodeSegmentRule for ruleset '" + name + "': " + e.getMessage() + ". Skipping this rule.");
                    }
                }

                if (rules.isEmpty() && !rawRules.isEmpty()) {
                     System.err.println("All rules failed to load for ruleset '" + name + "'. Skipping this ruleset.");
                     continue;
                }
                 if (rules.isEmpty() && rawRules.isEmpty()) { // if rules list was empty in JSON
                    System.err.println("Ruleset '" + name + "' has no rules defined. Skipping this ruleset.");
                    continue;
                }


                try {
                    RuleSet ruleSet = new RuleSet(name, rules); // Constructor sorts and validates rules
                    ruleSet.validateRules(); // This is now called inside the RuleSet constructor or can be called explicitly
                    ruleSets.put(ruleSet.getName(), ruleSet);
                    System.out.println("Successfully loaded and validated RuleSet: " + ruleSet.getName() +
                                       " with " + ruleSet.getRules().size() + " rules and total length " +
                                       ruleSet.getTotalBarcodeLength());
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.err.println("Error processing or validating RuleSet '" + name + "': " + e.getMessage() + ". Skipping this ruleset.");
                } catch (Exception e) { // Catch any other unexpected errors during RuleSet creation
                    System.err.println("Unexpected error creating RuleSet '" + name + "': " + e.getMessage() + ". Skipping this ruleset.");
                    e.printStackTrace();
                }
            }
            System.out.println("Finished loading " + ruleSets.size() + " rulesets from '" + rulesJsonPath + "'.");

        } catch (IOException e) {
            System.err.println("Failed to load rulesets from '" + rulesJsonPath + "': " + e.getMessage());
            // e.printStackTrace();
        }
    }

    /**
     * Creates a default rules.json file in src/main/resources if it doesn't exist.
     * This is primarily for development convenience. In a production environment,
     * the rules file should be provisioned as part of the deployment.
     */
    private void createDefaultRuleSetFileIfNotExistsInResources() {
        try {
            // Check if the file already exists in the classpath (e.g., inside JAR or target/classes)
            Resource resource = new ClassPathResource(RULES_FILE_NAME);
            if (resource.exists()) {
                System.out.println("Default rules file '" + RULES_FILE_NAME + "' already exists in classpath. No action taken.");
                return;
            }

            // If not in classpath, try to create it in src/main/resources
            // This path is relative to the project root when running in an IDE or from Maven.
            Path path = Paths.get("src", "main", "resources", RULES_FILE_NAME);

            if (Files.exists(path)) {
                System.out.println("Default rules file already exists at: " + path.toAbsolutePath());
                return;
            }

            // Ensure parent directory exists
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            List<BarcodeSegmentRule> rulesFor20Char = new ArrayList<>();
            // Order must be sequential, starting from 0 or 1 based on RuleSet validation logic.
            // RuleSet validation expects sequential order from the first rule's order.
            // Let's make them 0-indexed for consistency with typical list/array indexing.
            // The RuleSet validation will check for sequential order from the lowest order number.
            // If RuleSet expects orders to start from 1, adjust here or in RuleSet validation.
            // Current RuleSet.validateRules() implies it starts from whatever the first rule's order is.
            // Let's use 0-indexed for the default. The RuleSet constructor sorts them first.
            rulesFor20Char.add(new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true));  // Word 1
            rulesFor20Char.add(new BarcodeSegmentRule(1, 1, SegmentType.STATIC, "T", false));
            rulesFor20Char.add(new BarcodeSegmentRule(2, 4, SegmentType.NUMERIC, null, true));  // Word 2
            // Example of STATIC_OR
            rulesFor20Char.add(new BarcodeSegmentRule(3, 1, SegmentType.STATIC_OR, Arrays.asList("E", "X", "Y"), false));
            rulesFor20Char.add(new BarcodeSegmentRule(4, 4, SegmentType.NUMERIC, null, true));  // Word 3
            // Example of BASE64
            rulesFor20Char.add(new BarcodeSegmentRule(5, 2, SegmentType.BASE64, null, false)); // Length 2 for Base64, e.g., "QQ==" is 4 chars, "QQ" is 2. Let's use "AA" for placeholder.
            rulesFor20Char.add(new BarcodeSegmentRule(6, 4, SegmentType.NUMERIC, null, true));  // Word 4
            rulesFor20Char.add(new BarcodeSegmentRule(7, 1, SegmentType.STATIC, "T", false));
            // Total length = 4+1+4+1+4+2+4+1 = 21

            // Create a RuleSet object. The constructor will sort rules by order.
            RuleSet defaultRuleSet;
            try {
                 defaultRuleSet = new RuleSet("default-20char", rulesFor20Char);
                 defaultRuleSet.validateRules(); // Validate it here to be sure before writing
            } catch (Exception e) {
                System.err.println("Failed to create and validate default RuleSet object: " + e.getMessage());
                return; // Don't write a broken default
            }


            List<RuleSet> defaultRuleSets = Collections.singletonList(defaultRuleSet);

            // Serialize the default RuleSet list to JSON
            // We need to ensure that BarcodeSegmentRule and RuleSet can be serialized correctly.
            // Jackson typically serializes based on getters. If using fields, need @JsonProperty or field visibility.
            // Our RuleSet(name, rules) constructor and BarcodeSegmentRule(...) constructor might not be used by default for deserialization
            // unless annotated with @JsonCreator and @JsonProperty on parameters.
            // However, for serialization, getters are usually enough.
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), defaultRuleSets);

            System.out.println("Created default rules file '" + RULES_FILE_NAME + "' at: " + path.toAbsolutePath());
            System.out.println("Please rebuild the project or ensure '" + RULES_FILE_NAME + "' is copied to your classpath (e.g., target/classes) for it to be loaded.");

        } catch (IOException e) {
            System.err.println("Could not create default rules file '" + RULES_FILE_NAME + "': " + e.getMessage());
            // e.printStackTrace(); // For debugging
        } catch (Exception e) { // Catch other exceptions like from RuleSet validation
             System.err.println("An unexpected error occurred while trying to create default rules file '" + RULES_FILE_NAME + "': " + e.getMessage());
             // e.printStackTrace();
        }
    }

    public RuleSet getRuleSetByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            if (!ruleSets.isEmpty()) {
                System.out.println("RuleSetName is null/empty, attempting to use the first available ruleset (sorted by name).");
                // Provide a consistent default: the one with the lexicographically smallest name
                return ruleSets.values().stream()
                               .min(java.util.Comparator.comparing(RuleSet::getName))
                               .orElse(null); // Should not be null if ruleSets is not empty
            }
            System.out.println("RuleSetName is null/empty and no rulesets are loaded.");
            return null;
        }
        RuleSet ruleSet = ruleSets.get(name);
        if (ruleSet == null) {
            System.out.println("RuleSet with name '" + name + "' not found.");
        }
        return ruleSet;
    }

    public List<String> getAllRuleSetNames() {
        List<String> names = new ArrayList<>(ruleSets.keySet());
        Collections.sort(names); // Ensure consistent order
        return names;
    }

    /**
     * Saves a RuleSet. This implementation attempts to write back to the src/main/resources/rules.json file.
     * This is suitable for development environments. In production, configuration management
     * or a database would be more appropriate for managing rulesets.
     * Note: Changes to src/main/resources/rules.json might require a project rebuild to be reflected in the classpath.
     * @param ruleSetToSave The RuleSet to save.
     * @throws IOException If an error occurs during file writing.
     * @throws IllegalArgumentException If the ruleSetToSave is invalid or null.
     */
    public synchronized void saveRuleSet(RuleSet ruleSetToSave) throws IOException, IllegalArgumentException {
        if (ruleSetToSave == null) {
            throw new IllegalArgumentException("RuleSet to save cannot be null.");
        }
        if (ruleSetToSave.getName() == null || ruleSetToSave.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("RuleSet name cannot be null or empty.");
        }

        // Ensure the RuleSet is valid before attempting to save
        try {
            ruleSetToSave.validateRules(); // This will throw IllegalStateException if invalid
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("RuleSet '" + ruleSetToSave.getName() + "' is invalid and cannot be saved: " + e.getMessage(), e);
        }

        // Load existing rules
        List<RuleSet> currentRuleSetsList = loadRuleSetsFromFile();

        // Remove old version if it exists, then add new/updated one
        final String nameToSave = ruleSetToSave.getName();
        currentRuleSetsList.removeIf(rs -> nameToSave.equals(rs.getName()));
        currentRuleSetsList.add(ruleSetToSave); // Add the (validated) version from the argument

        // Persist the updated list
        persistRuleSetsToFile(currentRuleSetsList);

        // After saving, reload the rules in memory to reflect the changes immediately
        loadRuleSets(); // This reloads from the file just written
    }

    public synchronized void deleteRuleSet(String nameToDelete) throws IOException, IllegalArgumentException {
        if (nameToDelete == null || nameToDelete.trim().isEmpty()) {
            throw new IllegalArgumentException("RuleSet name to delete cannot be null or empty.");
        }

        List<RuleSet> currentRuleSetsList = loadRuleSetsFromFile();
        boolean removed = currentRuleSetsList.removeIf(rs -> nameToDelete.equals(rs.getName()));

        if (!removed) {
            // Optionally, throw an exception or return a status indicating the ruleset was not found
            System.out.println("RuleSet with name '" + nameToDelete + "' not found for deletion.");
            // Or throw new IllegalArgumentException("RuleSet with name '" + nameToDelete + "' not found.");
            return; // No changes to persist
        }

        persistRuleSetsToFile(currentRuleSetsList);
        loadRuleSets(); // Reload in-memory map
        System.out.println("Successfully deleted RuleSet '" + nameToDelete + "' and updated rules.json.");
    }

    private List<RuleSet> loadRuleSetsFromFile() throws IOException {
        Path rulesFilePath = Paths.get("src", "main", "resources", RULES_FILE_NAME);
        List<RuleSet> ruleSetsList = new ArrayList<>();

        if (Files.exists(rulesFilePath)) {
            try {
                byte[] jsonData = Files.readAllBytes(rulesFilePath);
                List<Map<String, Object>> rawRuleSetList = objectMapper.readValue(jsonData, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> rawRuleSet : rawRuleSetList) {
                    String name = (String) rawRuleSet.get("name");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rawRules = (List<Map<String, Object>>) rawRuleSet.get("rules");
                    if (name != null && rawRules != null) {
                        List<BarcodeSegmentRule> rules = new ArrayList<>();
                        for (Map<String, Object> rawRule : rawRules) {
                            try {
                                rules.add(objectMapper.convertValue(rawRule, BarcodeSegmentRule.class));
                            } catch (Exception e) {
                                System.err.println("Skipping invalid rule data during file load for RuleSet '" + name + "': " + rawRule + ". Error: " + e.getMessage());
                            }
                        }
                        if (!rules.isEmpty()) { // Only add if there are valid rules
                           try {
                                RuleSet rs = new RuleSet(name, rules);
                                rs.validateRules(); // Validate before adding to list from file
                                ruleSetsList.add(rs);
                           } catch (IllegalArgumentException | IllegalStateException e) {
                               System.err.println("Skipping invalid RuleSet '" + name + "' from file during load: " + e.getMessage());
                           }
                        } else if (rawRules.isEmpty()){
                             // Handle case where a ruleset in JSON has an empty rules list
                            System.err.println("RuleSet '" + name + "' in file has no rules. Skipping.");
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Could not read existing rules file at '" + rulesFilePath + "' for modification. Error: " + e.getMessage());
                throw e; // Re-throw as this is a critical part of save/delete
            }
        }
        return ruleSetsList;
    }

    private void persistRuleSetsToFile(List<RuleSet> ruleSetsToPersist) throws IOException {
        Path rulesFilePath = Paths.get("src", "main", "resources", RULES_FILE_NAME);
        Path parentDir = rulesFilePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(rulesFilePath.toFile(), ruleSetsToPersist);
            System.out.println("Successfully persisted rules to '" + rulesFilePath.toAbsolutePath() + "'.");
            System.out.println("Please rebuild/refresh your project for changes to be reflected in the classpath if running in certain IDEs.");
        } catch (IOException e) {
            System.err.println("Failed to persist rules to '" + RULES_FILE_NAME + "': " + e.getMessage());
            throw e;
        }
    }
}
