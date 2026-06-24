package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.Dividend;
import com.marketcompass.investment.service.DividendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dividends")
public class DividendController {

    private final DividendService dividendService;

    public DividendController(DividendService dividendService) {
        this.dividendService = dividendService;
    }

    @GetMapping
    public List<Dividend> getAllDividends() {
        return dividendService.getAllDividends();
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<Dividend> getByTicker(@PathVariable String ticker) {
        return dividendService.getByTicker(ticker)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/aristocrats")
    public List<Dividend> getAristocrats() {
        return dividendService.getDividendAristocrats();
    }

    @GetMapping("/kings")
    public List<Dividend> getKings() {
        return dividendService.getDividendKings();
    }

    @GetMapping("/monthly")
    public List<Dividend> getMonthlyPayers() {
        return dividendService.getMonthlyPayers();
    }

    /**
     * Calculate dividend income for a set of holdings.
     * Body: { "AAPL": 10, "KO": 25, "O": 50 }  (ticker → shares)
     */
    @PostMapping("/income")
    public Map<String, Object> calculateIncome(@RequestBody Map<String, Double> holdingsSharesMap) {
        return dividendService.calculateIncome(holdingsSharesMap);
    }
}
