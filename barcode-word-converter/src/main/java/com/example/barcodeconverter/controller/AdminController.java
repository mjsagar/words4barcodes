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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final RuleService ruleService;

    @Autowired
    public AdminController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/rules")
    public String listRuleSets(Model model) {
        List<String> ruleSetNames = ruleService.getAllRuleSetNames();
        List<RuleSet> ruleSets = ruleSetNames.stream()
                                             .map(ruleService::getRuleSetByName)
                                             .collect(Collectors.toList());
        model.addAttribute("ruleSets", ruleSets);
        return "admin/rules-list"; // Thymeleaf template for listing rules
    }

    @GetMapping("/rules/edit")
    public String editRuleSetForm(@RequestParam(name = "name", required = false) String name, Model model) {
        RuleSet ruleSet;
        if (name != null && !name.isEmpty()) {
            ruleSet = ruleService.getRuleSetByName(name);
            if (ruleSet == null) {
                // Handle case where ruleset to edit isn't found, perhaps redirect with error
                // For now, create a new one with that name
                ruleSet = new RuleSet(name, new ArrayList<>());
            }
        } else {
            ruleSet = new RuleSet("", new ArrayList<>()); // For creating a new RuleSet
        }
        // Ensure there are always enough empty rules for a typical UI form (e.g., 8-10 rows for segments)
        // Or let the UI dynamically add them. For simplicity, let's ensure a minimum for display.
        // The actual saving logic will filter out empty/incomplete rules.
        List<BarcodeSegmentRule> displayRules = new ArrayList<>(ruleSet.getRules());
        while(displayRules.size() < 8) { // Ensure at least 8 rows for the form
            displayRules.add(new BarcodeSegmentRule());
        }

        model.addAttribute("ruleSet", ruleSet); // Pass the original ruleset for its name
        model.addAttribute("displayRules", displayRules); // Pass potentially padded rules for form display
        model.addAttribute("segmentTypes", SegmentType.values());
        return "admin/rule-edit-form"; // Thymeleaf template for editing/creating a rule
    }

    @PostMapping("/rules/save")
    public String saveRuleSet(@ModelAttribute RuleSet ruleSetData, // Spring automatically populates this from form fields if names match
                              RedirectAttributes redirectAttributes) {

        // The @ModelAttribute RuleSet ruleSetData might not fully work for the list of rules
        // if they are dynamically added or indexed in the form.
        // It's often more robust to bind the list of rules separately or parse request parameters.
        // For this iteration, assuming simple binding or will adjust if form data is complex.
        // We need to reconstruct the RuleSet from potentially sparse form data.

        List<BarcodeSegmentRule> newRules = new ArrayList<>();
        if (ruleSetData.getRules() != null) { // Check if rules were submitted
            int order = 0;
            for (BarcodeSegmentRule submittedRule : ruleSetData.getRules()) {
                // Only add rules that have a type and length specified (basic check for non-empty rule)
                if (submittedRule.getType() != null && submittedRule.getLength() > 0) {
                    BarcodeSegmentRule rule = new BarcodeSegmentRule();
                    rule.setOrder(order++); // Assign order sequentially
                    rule.setLength(submittedRule.getLength());
                    rule.setType(submittedRule.getType());
                    rule.setMapsToWord(submittedRule.isMapsToWord());
                    if (submittedRule.getType() == SegmentType.STATIC) {
                        rule.setStaticValue(submittedRule.getStaticValue() != null ? submittedRule.getStaticValue() : "");
                    } else {
                        rule.setStaticValue(null);
                    }

                    // Validate individual rule constraints before adding
                    try {
                        if (rule.getType() == SegmentType.STATIC && (rule.getStaticValue() == null || rule.getStaticValue().isEmpty())) {
                           // This rule is incomplete for STATIC, skip or handle as error
                           // For now, we might let RuleSet validation catch this, or add error to redirectAttributes
                           System.out.println("Skipping incomplete STATIC rule at order " + (order-1));
                           order--; // revert order increment
                           continue;
                        }
                         if (rule.getType() == SegmentType.STATIC && rule.getStaticValue().length() != rule.getLength()) {
                            System.out.println("Static value length mismatch for rule at order " + (order-1) + ". Static: '"+rule.getStaticValue()+"' len: "+rule.getStaticValue().length()+", Rule len: "+rule.getLength());
                            // This will be caught by RuleSet validation too.
                        }
                        if (rule.isMapsToWord() && rule.getType() != SegmentType.NUMERIC) {
                            // This rule is invalid, skip or handle as error
                            System.out.println("Skipping invalid word mapping for non-NUMERIC rule at order " + (order-1));
                            order--; // revert order increment
                            continue;
                        }
                    } catch (IllegalArgumentException e) {
                        // Catch issues from BarcodeSegmentRule constructor if it throws them
                        redirectAttributes.addFlashAttribute("errorMessage", "Error in segment rule definition: " + e.getMessage());
                        return "redirect:/admin/rules/edit?name=" + java.net.URLEncoder.encode(ruleSetData.getName(), StandardCharsets.UTF_8);
                    }

                    newRules.add(rule);
                }
            }
        }

        RuleSet finalRuleSet = new RuleSet();
        finalRuleSet.setName(ruleSetData.getName());
        finalRuleSet.setRules(newRules); // This will sort and mark for re-validation

        try {
            finalRuleSet.validateRules(); // Explicitly validate before saving
            ruleService.saveRuleSet(finalRuleSet);
            redirectAttributes.addFlashAttribute("successMessage", "RuleSet '" + finalRuleSet.getName() + "' saved successfully.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save RuleSet: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid RuleSet configuration: " + e.getMessage());
             // To repopulate the form, we need to send back the attempted data.
             // This is more complex with redirects. For now, just an error message.
             // Ideally, one would return to the form view directly with error messages and original data.
            return "redirect:/admin/rules/edit?name=" + java.net.URLEncoder.encode(ruleSetData.getName(), StandardCharsets.UTF_8);
        }

        return "redirect:/admin/rules";
    }
}
