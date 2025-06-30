package com.example.barcodeconverter.controller;

import com.example.barcodeconverter.model.BarcodeSegmentRule;
import com.example.barcodeconverter.model.RuleSet;
import com.example.barcodeconverter.model.SegmentType;
import com.example.barcodeconverter.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets; // Added import
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final RuleService ruleService;
    private static final int MIN_DISPLAY_RULES = 8;


    @Autowired
    public AdminController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/rules")
    public String listRuleSets(Model model) {
        List<String> ruleSetNames = ruleService.getAllRuleSetNames();
        // Fetch full RuleSet objects if needed by the template, or just names
        model.addAttribute("ruleSetNames", ruleSetNames);
        return "admin/rules-list";
    }

    @GetMapping("/rules/edit")
    public String editRuleSetForm(@RequestParam(name = "name", required = false) String name, Model model) {
        RuleSet ruleSetToEdit;
        boolean isNew = (name == null || name.trim().isEmpty());

        if (!isNew) {
            ruleSetToEdit = ruleService.getRuleSetByName(name);
            if (ruleSetToEdit == null) {
                // RuleSet not found, treat as new but with a pre-filled name for user convenience
                // Or redirect with error: redirectAttributes.addFlashAttribute("errorMessage", "RuleSet not found: " + name); return "redirect:/admin/rules";
                // For now, creating a new one with the given name if not found.
                // The RuleSet constructor requires a non-empty list of rules.
                // So, we provide a dummy rule to satisfy constructor, it will be replaced by form.
                List<BarcodeSegmentRule> placeholderRules = new ArrayList<>();
                // Add a minimal valid rule to satisfy RuleSet constructor if it requires non-empty rules.
                // This rule won't be saved unless the user explicitly keeps/modifies it.
                // The form should ideally allow adding rules from scratch.
                // For simplicity, if a ruleset name is given but not found, we could redirect or error.
                // For now, let's assume if name is provided, it's for editing an existing one.
                // If it's not found, redirecting might be better.
                // However, the current plan implies creating a new one if name is given but not found.
                // This part needs careful handling to avoid RuleSet constructor errors.
                // A better approach for "new with name" might be to pass the name and an empty rule list wrapper to the view.

                // Let's adjust: if name is given but not found, we could show an error or redirect.
                // For creating a new RuleSet, 'name' should be null or empty.
                 model.addAttribute("errorMessage", "RuleSet '" + name + "' not found. Creating a new one.");
                 ruleSetToEdit = new RuleSet(name, createEmptyDisplayRules(0)); // name can be pre-filled
            }
        } else {
            // For a completely new RuleSet
            ruleSetToEdit = new RuleSet("NewRuleSetName", createEmptyDisplayRules(0)); // Temporary name
        }

        // For display purposes, ensure there are enough BarcodeSegmentRule objects
        // for the form rows. This list is for the form, not the actual ruleset's rules yet.
        List<BarcodeSegmentRule> displayRules = new ArrayList<>();
        if (ruleSetToEdit != null && !ruleSetToEdit.getRules().isEmpty()) {
            displayRules.addAll(ruleSetToEdit.getRules());
        }
        while (displayRules.size() < MIN_DISPLAY_RULES) {
            displayRules.add(new BarcodeSegmentRule()); // Add empty rules for form
        }

        model.addAttribute("ruleSetForm", new RuleSetForm(ruleSetToEdit, displayRules));
        model.addAttribute("isNew", isNew); // To conditionally enable/disable name editing
        model.addAttribute("segmentTypes", SegmentType.values());
        return "admin/rule-edit-form";
    }

    private List<BarcodeSegmentRule> createEmptyDisplayRules(int count) {
        List<BarcodeSegmentRule> rules = new ArrayList<>();
        if (count == 0) { // RuleSet constructor needs at least one rule if we use it directly.
            // This is a temporary placeholder for the form if no rules exist yet.
            // The actual save logic will filter out empty rules.
            // To satisfy RuleSet constructor if it's used directly with an empty list of rules
            // which it now disallows.
            // So, when creating a "new" RuleSet for the form, we should not use the RuleSet constructor
            // with an empty list directly if it's going to be validated immediately.
            // The RuleSetForm DTO helps here.
        }
        for (int i = 0; i < count; i++) {
            rules.add(new BarcodeSegmentRule());
        }
        return rules;
    }


    @PostMapping("/rules/save")
    public String saveRuleSet(@ModelAttribute("ruleSetForm") RuleSetForm ruleSetForm,
                              RedirectAttributes redirectAttributes) {

        List<BarcodeSegmentRule> submittedRules = new ArrayList<>();
        int order = 0;
        if (ruleSetForm.getDisplayRules() != null) {
            for (BarcodeSegmentRule tempRule : ruleSetForm.getDisplayRules()) {
                // Only process rules that have a type and positive length
                if (tempRule.getType() != null && tempRule.getLength() > 0) {
                    String staticValue = tempRule.getStaticValue();
                    List<String> staticOrValues = null;

                    if (tempRule.getType() == SegmentType.STATIC_OR) {
                        // Assuming staticOrValuesString is a field in BarcodeSegmentRule or passed separately
                        // For now, let's assume BarcodeSegmentRule was somehow populated with a String for staticOrValues
                        // that needs parsing. This part is tricky with direct @ModelAttribute binding for complex lists.
                        // A better way is to have a specific DTO for the form rule that has String staticOrValuesString.
                        // For this iteration, we'll assume the form somehow submits this as a list or we adjust BarcodeSegmentRule.
                        // Let's assume `tempRule.getStaticOrValues()` somehow got populated if it was a list input.
                        // If it's a single string from a text field, it needs parsing.
                        // For simplicity, if `getStaticOrValues()` returns a list with a single comma-separated string, parse it.
                        // This is a common way to handle list input from a single text field.
                        if (tempRule.getStaticOrValues() != null && !tempRule.getStaticOrValues().isEmpty() && tempRule.getStaticOrValues().get(0) != null && !tempRule.getStaticOrValues().get(0).trim().isEmpty()) {
                            staticOrValues = Arrays.asList(tempRule.getStaticOrValues().get(0).split("\\s*,\\s*"));
                        } else {
                            staticOrValues = new ArrayList<>(); // Ensure it's an empty list not null
                        }
                    }

                    try {
                        BarcodeSegmentRule actualRule;
                        if (tempRule.getType() == SegmentType.STATIC_OR) {
                            actualRule = new BarcodeSegmentRule(order, tempRule.getLength(), tempRule.getType(), staticOrValues, tempRule.isMapsToWord());
                        } else {
                            actualRule = new BarcodeSegmentRule(order, tempRule.getLength(), tempRule.getType(), staticValue, tempRule.isMapsToWord());
                        }
                        submittedRules.add(actualRule);
                        order++;
                    } catch (IllegalArgumentException e) {
                        // This catches validation errors from BarcodeSegmentRule constructor
                        redirectAttributes.addFlashAttribute("errorMessage", "Error in rule (order " + order + "): " + e.getMessage());
                        return "redirect:/admin/rules/edit" + (ruleSetForm.getName() != null && !ruleSetForm.getName().isEmpty() ? "?name=" + java.net.URLEncoder.encode(ruleSetForm.getName(), StandardCharsets.UTF_8) : "");
                    }
                }
            }
        }

        if (ruleSetForm.getName() == null || ruleSetForm.getName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "RuleSet name cannot be empty.");
            return "redirect:/admin/rules/edit" + (ruleSetForm.getName() != null && !ruleSetForm.getName().isEmpty() ? "?name=" + java.net.URLEncoder.encode(ruleSetForm.getName(), StandardCharsets.UTF_8) : "");
        }
        if (submittedRules.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A RuleSet must contain at least one segment rule.");
            return "redirect:/admin/rules/edit?name=" + java.net.URLEncoder.encode(ruleSetForm.getName(), StandardCharsets.UTF_8);
        }

        RuleSet finalRuleSet;
        try {
            finalRuleSet = new RuleSet(ruleSetForm.getName(), submittedRules);
            finalRuleSet.validateRules(); // This also calculates total length
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating/validating RuleSet: " + e.getMessage());
            return "redirect:/admin/rules/edit?name=" + java.net.URLEncoder.encode(ruleSetForm.getName(), StandardCharsets.UTF_8);
        }

        try {
            ruleService.saveRuleSet(finalRuleSet);
            redirectAttributes.addFlashAttribute("successMessage", "RuleSet '" + finalRuleSet.getName() + "' saved successfully.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save RuleSet: " + e.getMessage());
            e.printStackTrace(); // Log to server console
        } catch (IllegalArgumentException | IllegalStateException e) { // Catch validation errors from saveRuleSet if any
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid RuleSet configuration during save: " + e.getMessage());
            return "redirect:/admin/rules/edit?name=" + java.net.URLEncoder.encode(ruleSetForm.getName(), StandardCharsets.UTF_8);
        }

        return "redirect:/admin/rules";
    }

    @PostMapping("/rules/delete")
    public String deleteRuleSet(@RequestParam String name, RedirectAttributes redirectAttributes) {
        try {
            ruleService.deleteRuleSet(name);
            redirectAttributes.addFlashAttribute("successMessage", "RuleSet '" + name + "' deleted successfully.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete RuleSet '" + name + "': " + e.getMessage());
            e.printStackTrace(); // Log to server console
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting RuleSet '" + name + "': " + e.getMessage());
        }
        return "redirect:/admin/rules";
    }

    // Inner class to act as a form backing object
    public static class RuleSetForm {
        private String name;
        private List<BarcodeSegmentRule> displayRules; // Used for form binding

        public RuleSetForm() {
            this.displayRules = new ArrayList<>();
            for(int i=0; i<MIN_DISPLAY_RULES; i++) { // pre-populate with empty rules for new form
                this.displayRules.add(new BarcodeSegmentRule());
            }
        }

        public RuleSetForm(RuleSet ruleSet, List<BarcodeSegmentRule> displayRules) {
            this.name = ruleSet.getName();
            this.displayRules = new ArrayList<>(displayRules);
             // Ensure enough rows for display, even if actual rules are fewer
            while(this.displayRules.size() < MIN_DISPLAY_RULES) {
                this.displayRules.add(new BarcodeSegmentRule());
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<BarcodeSegmentRule> getDisplayRules() {
            return displayRules;
        }

        public void setDisplayRules(List<BarcodeSegmentRule> displayRules) {
            this.displayRules = displayRules;
        }
    }
}
