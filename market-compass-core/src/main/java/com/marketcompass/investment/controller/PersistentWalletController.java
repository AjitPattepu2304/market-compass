package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.TradeRecord;
import com.marketcompass.investment.persistence.PersistentTradingService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Profile("postgres")
@RequestMapping("/api/wallet")
public class PersistentWalletController {
    private final PersistentTradingService trading;
    public PersistentWalletController(PersistentTradingService trading){this.trading=trading;}
    @GetMapping public Map<String,Object> wallet(){return trading.walletSummary();}
    @GetMapping("/history") public List<TradeRecord> history(){return trading.history(null);}
    @GetMapping("/history/{ticker}") public List<TradeRecord> history(@PathVariable String ticker){return trading.history(ticker);}
    @PostMapping("/buy") public ResponseEntity<?> buy(@RequestBody Map<String,Object> body){
        try{return ResponseEntity.ok(trading.buy((String)body.get("ticker"),((Number)body.get("shares")).doubleValue(),((Number)body.getOrDefault("price",0)).doubleValue(),"MANUAL"));}
        catch(IllegalArgumentException|IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    }
    @PostMapping("/sell") public ResponseEntity<?> sell(@RequestBody Map<String,Object> body){
        try{return ResponseEntity.ok(trading.sell((String)body.get("ticker"),((Number)body.get("shares")).doubleValue(),((Number)body.getOrDefault("price",0)).doubleValue(),"MANUAL"));}
        catch(IllegalArgumentException|IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    }
}
