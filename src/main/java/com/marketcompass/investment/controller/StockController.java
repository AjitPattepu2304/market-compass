package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.Stock;
import com.marketcompass.investment.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockService.getAllStocks();
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<Stock> getByTicker(@PathVariable String ticker) {
        return stockService.getByTicker(ticker)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sector/{sector}")
    public List<Stock> getBySector(@PathVariable String sector) {
        return stockService.getBySector(sector);
    }

    @GetMapping("/search")
    public List<Stock> search(@RequestParam String q) {
        return stockService.search(q);
    }

    @GetMapping("/sectors")
    public List<String> getSectors() {
        return stockService.getAllSectors();
    }

    @GetMapping("/chips")
    public List<Stock> getChipStocks() {
        return stockService.getChipStocks();
    }

    @GetMapping("/chips/type/{type}")
    public List<Stock> getChipsByType(@PathVariable String type) {
        return stockService.getChipStocksByType(type);
    }

    @GetMapping("/chips/by-size")
    public Map<String, List<Stock>> getChipsBySize() {
        return stockService.getChipStocksBySize();
    }
}
