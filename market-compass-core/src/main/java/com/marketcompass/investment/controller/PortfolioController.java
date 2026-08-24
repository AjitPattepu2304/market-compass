package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.PortfolioHolding;
import com.marketcompass.investment.service.PortfolioService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Profile("!postgres")
@RequestMapping("/api/portfolio")
public class PortfolioController {
    private final PortfolioService portfolioService;
    public PortfolioController(PortfolioService portfolioService){this.portfolioService=portfolioService;}
    @GetMapping public List<PortfolioHolding> getHoldings(){return portfolioService.getHoldings();}
    @GetMapping("/summary") public Map<String,Object> getSummary(){return portfolioService.getSummary();}
    @PostMapping("/holdings") public ResponseEntity<Map<String,Object>> addHolding(@RequestBody PortfolioHolding holding){portfolioService.addHolding(holding);return ResponseEntity.ok(portfolioService.getSummary());}
    @DeleteMapping("/holdings/{ticker}") public ResponseEntity<Map<String,Object>> removeHolding(@PathVariable String ticker){boolean removed=portfolioService.removeHolding(ticker);if(!removed)return ResponseEntity.notFound().build();return ResponseEntity.ok(portfolioService.getSummary());}
}
