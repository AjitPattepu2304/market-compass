package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.PortfolioHolding;
import com.marketcompass.investment.persistence.PersistentPortfolioService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Profile("postgres")
@RequestMapping("/api/portfolio")
public class PersistentPortfolioController {
    private final PersistentPortfolioService portfolio;
    public PersistentPortfolioController(PersistentPortfolioService portfolio){this.portfolio=portfolio;}
    @GetMapping public List<PortfolioHolding> holdings(){return portfolio.getHoldings();}
    @GetMapping("/summary") public Map<String,Object> summary(){return portfolio.getSummary();}
    @PostMapping("/holdings") public ResponseEntity<Map<String,Object>> add(@RequestBody PortfolioHolding holding){portfolio.addHolding(holding);return ResponseEntity.ok(portfolio.getSummary());}
    @DeleteMapping("/holdings/{ticker}") public ResponseEntity<Map<String,Object>> remove(@PathVariable String ticker){if(!portfolio.removeHolding(ticker))return ResponseEntity.notFound().build();return ResponseEntity.ok(portfolio.getSummary());}
}
