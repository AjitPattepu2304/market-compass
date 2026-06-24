package com.marketcompass.investment.controller;

import com.marketcompass.investment.service.InvestmentCalculatorService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/calculator")
public class InvestmentCalculatorController {

    private final InvestmentCalculatorService calculatorService;

    public InvestmentCalculatorController(InvestmentCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    /**
     * Compound Interest Calculator.
     * Example: GET /api/calculator/compound?principal=10000&rate=7&years=30&compounds=1
     */
    @GetMapping("/compound")
    public Map<String, Object> compoundInterest(
            @RequestParam(defaultValue = "10000") double principal,
            @RequestParam(defaultValue = "7") double rate,
            @RequestParam(defaultValue = "30") int years,
            @RequestParam(defaultValue = "1") int compounds) {
        return calculatorService.compoundInterest(principal, rate, years, compounds);
    }

    /**
     * DRIP Projection Calculator.
     * Example: GET /api/calculator/drip?investment=10000&yield=3.5&dividendGrowth=6&priceGrowth=7&years=20
     */
    @GetMapping("/drip")
    public Map<String, Object> dripProjection(
            @RequestParam(defaultValue = "10000") double investment,
            @RequestParam(defaultValue = "3.5") double yield,
            @RequestParam(defaultValue = "6") double dividendGrowth,
            @RequestParam(defaultValue = "7") double priceGrowth,
            @RequestParam(defaultValue = "20") int years) {
        return calculatorService.dripProjection(investment, yield, dividendGrowth, priceGrowth, years);
    }

    /**
     * Retirement Savings Calculator.
     * Example: GET /api/calculator/retirement?monthly=500&rate=7&years=30&initial=5000
     */
    @GetMapping("/retirement")
    public Map<String, Object> retirementSavings(
            @RequestParam(defaultValue = "500") double monthly,
            @RequestParam(defaultValue = "7") double rate,
            @RequestParam(defaultValue = "30") int years,
            @RequestParam(defaultValue = "0") double initial) {
        return calculatorService.retirementSavings(monthly, rate, years, initial);
    }
}
