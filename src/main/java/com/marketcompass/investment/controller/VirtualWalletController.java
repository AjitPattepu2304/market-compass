package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.TradeRecord;
import com.marketcompass.investment.service.VirtualWalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the virtual paper-trading wallet.
 *
 * GET  /api/wallet                          — wallet summary + balance
 * GET  /api/wallet/history                  — full trade history
 * GET  /api/wallet/history/{ticker}         — trades for a specific ticker
 * POST /api/wallet/buy                      — buy by share count
 * POST /api/wallet/buy/amount               — buy by dollar amount (SIP-style)
 * POST /api/wallet/sell                     — sell by share count
 */
@RestController
@RequestMapping("/api/wallet")
public class VirtualWalletController {

    private final VirtualWalletService walletService;

    public VirtualWalletController(VirtualWalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWallet() {
        return ResponseEntity.ok(walletService.getWalletSummary());
    }

    @GetMapping("/history")
    public ResponseEntity<List<TradeRecord>> getHistory() {
        return ResponseEntity.ok(walletService.getTradeHistory());
    }

    @GetMapping("/history/{ticker}")
    public ResponseEntity<List<TradeRecord>> getHistoryByTicker(@PathVariable String ticker) {
        return ResponseEntity.ok(walletService.getTradeHistoryByTicker(ticker));
    }

    /**
     * Buy by share count.
     * Body: { "ticker": "AAPL", "shares": 5 }
     */
    @PostMapping("/buy")
    public ResponseEntity<?> buy(@RequestBody Map<String, Object> body) {
        try {
            String ticker = (String) body.get("ticker");
            double shares = ((Number) body.get("shares")).doubleValue();
            TradeRecord record = walletService.buy(ticker, shares, "MANUAL");
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Buy by dollar amount — fractional shares allowed (SIP-style).
     * Body: { "ticker": "NVDA", "amount": 500 }
     */
    @PostMapping("/buy/amount")
    public ResponseEntity<?> buyByAmount(@RequestBody Map<String, Object> body) {
        try {
            String ticker = (String) body.get("ticker");
            double amount = ((Number) body.get("amount")).doubleValue();
            TradeRecord record = walletService.buyByAmount(ticker, amount, "MANUAL");
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Sell by share count.
     * Body: { "ticker": "AAPL", "shares": 3 }
     */
    @PostMapping("/sell")
    public ResponseEntity<?> sell(@RequestBody Map<String, Object> body) {
        try {
            String ticker = (String) body.get("ticker");
            double shares = ((Number) body.get("shares")).doubleValue();
            TradeRecord record = walletService.sell(ticker, shares, "MANUAL");
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
