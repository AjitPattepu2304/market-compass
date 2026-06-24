package com.product_office_setup.product_office_setup.investment.controller;

import com.product_office_setup.product_office_setup.investment.model.Opportunity;
import com.product_office_setup.product_office_setup.investment.service.BrokerageAgentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class BrokerageAgentController {

    private final BrokerageAgentService agentService;

    public BrokerageAgentController(BrokerageAgentService agentService) {
        this.agentService = agentService;
    }

    /** All opportunities sorted by score descending */
    @GetMapping("/opportunities")
    public List<Opportunity> getAllOpportunities() {
        return agentService.getAllOpportunities();
    }

    /** Filter by type: VALUE, INCOME, CHIP_SECTOR, ETF */
    @GetMapping("/opportunities/{type}")
    public List<Opportunity> getByType(@PathVariable String type) {
        return agentService.getByType(type);
    }

    /** Market insights and sector narrative */
    @GetMapping("/insights")
    public Map<String, Object> getInsights() {
        return agentService.getMarketInsights();
    }
}
