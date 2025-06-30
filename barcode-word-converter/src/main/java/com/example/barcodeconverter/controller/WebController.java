package com.example.barcodeconverter.controller;

import com.example.barcodeconverter.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class WebController {

    private final RuleService ruleService;

    @Autowired
    public WebController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<String> ruleSetNames = ruleService.getAllRuleSetNames();
        model.addAttribute("ruleSetNames", ruleSetNames);
        if (!ruleSetNames.isEmpty()) {
            model.addAttribute("defaultRuleSet", ruleSetNames.get(0));
        } else {
            model.addAttribute("defaultRuleSet", "");
        }
        return "index"; // This will look for src/main/resources/templates/index.html
    }
}
