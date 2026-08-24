package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.TradeRecord;
import com.marketcompass.investment.service.VirtualWalletService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Profile("!postgres")
@RequestMapping("/api/wallet")
public class VirtualWalletController {
    private final VirtualWalletService walletService;
    public VirtualWalletController(VirtualWalletService walletService) { this.walletService = walletService; }
    @GetMapping public ResponseEntity<Map<String,Object>> getWallet(){ return ResponseEntity.ok(walletService.getWalletSummary()); }
    @GetMapping("/history") public ResponseEntity<List<TradeRecord>> getHistory(){ return ResponseEntity.ok(walletService.getTradeHistory()); }
    @GetMapping("/history/{ticker}") public ResponseEntity<List<TradeRecord>> getHistoryByTicker(@PathVariable String ticker){ return ResponseEntity.ok(walletService.getTradeHistoryByTicker(ticker)); }
    @PostMapping("/buy") public ResponseEntity<?> buy(@RequestBody Map<String,Object> body){
        try{return ResponseEntity.ok(walletService.buy((String)body.get("ticker"),((Number)body.get("shares")).doubleValue(),"MANUAL"));}
        catch(IllegalArgumentException|IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    }
    @PostMapping("/buy/amount") public ResponseEntity<?> buyByAmount(@RequestBody Map<String,Object> body){
        try{return ResponseEntity.ok(walletService.buyByAmount((String)body.get("ticker"),((Number)body.get("amount")).doubleValue(),"MANUAL"));}
        catch(IllegalArgumentException|IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    }
    @PostMapping("/sell") public ResponseEntity<?> sell(@RequestBody Map<String,Object> body){
        try{return ResponseEntity.ok(walletService.sell((String)body.get("ticker"),((Number)body.get("shares")).doubleValue(),"MANUAL"));}
        catch(IllegalArgumentException|IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    }
}
