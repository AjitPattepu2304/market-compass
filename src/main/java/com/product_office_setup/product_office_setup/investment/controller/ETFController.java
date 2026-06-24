package com.product_office_setup.product_office_setup.investment.controller;

import com.product_office_setup.product_office_setup.investment.model.ETF;
import com.product_office_setup.product_office_setup.investment.service.ETFService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/etfs")
public class ETFController {

    private final ETFService etfService;

    public ETFController(ETFService etfService) {
        this.etfService = etfService;
    }

    @GetMapping
    public List<ETF> getAllETFs() {
        return etfService.getAllETFs();
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<ETF> getByTicker(@PathVariable String ticker) {
        return etfService.getByTicker(ticker)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public List<ETF> getByCategory(@PathVariable String category) {
        return etfService.getByCategory(category);
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return etfService.getAllCategories();
    }

    @GetMapping("/compare")
    public List<ETF> compare(@RequestParam String tickers) {
        List<String> tickerList = Arrays.asList(tickers.split(","));
        return etfService.compare(tickerList);
    }
}
