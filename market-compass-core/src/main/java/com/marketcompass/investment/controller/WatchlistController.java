package com.marketcompass.investment.controller;

import com.marketcompass.investment.service.WatchlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for the watchlist.
 *
 * GET  /api/watchlist              — all watched tickers with current prices
 * GET  /api/watchlist/alerts       — items that hit target or drop threshold
 * GET  /api/watchlist/{ticker}     — single ticker detail
 * POST /api/watchlist              — add a ticker to watchlist
 * DELETE /api/watchlist/{ticker}   — remove from watchlist
 *
 * POST body example:
 * {
 *   "ticker": "NVDA",
 *   "targetPrice": 850.00,      // optional — 0 or omit to skip
 *   "alertThreshold": 5.0,      // optional — alert if drops 5% from add price
 *   "notes": "Waiting for dip before AI earnings"
 * }
 */
@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(watchlistService.getSummary());
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        return ResponseEntity.ok(watchlistService.getAlerts());
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<?> getByTicker(@PathVariable String ticker) {
        return watchlistService.getByTicker(ticker)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Map<String, Object> body) {
        try {
            String ticker = (String) body.get("ticker");
            double targetPrice = body.containsKey("targetPrice")
                    ? ((Number) body.get("targetPrice")).doubleValue() : 0;
            double alertThreshold = body.containsKey("alertThreshold")
                    ? ((Number) body.get("alertThreshold")).doubleValue() : 0;
            String notes = body.containsKey("notes") ? (String) body.get("notes") : "";

            return ResponseEntity.ok(watchlistService.add(ticker, targetPrice, alertThreshold, notes));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{ticker}")
    public ResponseEntity<?> remove(@PathVariable String ticker) {
        boolean removed = watchlistService.remove(ticker);
        if (removed) return ResponseEntity.ok(Map.of("removed", ticker.toUpperCase()));
        return ResponseEntity.notFound().build();
    }
}
