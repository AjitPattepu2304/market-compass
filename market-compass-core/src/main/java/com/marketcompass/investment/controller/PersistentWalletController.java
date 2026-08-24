package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.TradeRecord;
import com.marketcompass.investment.persistence.PersistentTradingService;
import com.marketcompass.investment.service.ETFService;
import com.marketcompass.investment.service.LivePriceService;
import com.marketcompass.investment.service.StockService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@Profile("postgres")
@RequestMapping("/api/wallet")
public class PersistentWalletController {
    private final PersistentTradingService trading; private final LivePriceService livePrices; private final StockService stocks; private final ETFService etfs;
    public PersistentWalletController(PersistentTradingService trading,LivePriceService livePrices,StockService stocks,ETFService etfs){this.trading=trading;this.livePrices=livePrices;this.stocks=stocks;this.etfs=etfs;}
    @GetMapping public Map<String,Object> wallet(){return trading.walletSummary();}
    @GetMapping("/history") public List<TradeRecord> history(){return trading.history(null);}
    @GetMapping("/history/{ticker}") public List<TradeRecord> history(@PathVariable String ticker){return trading.history(ticker);}
    @PostMapping("/buy") public ResponseEntity<?> buy(@RequestBody Map<String,Object> body){try{String t=(String)body.get("ticker");double s=((Number)body.get("shares")).doubleValue();return ResponseEntity.ok(trading.buy(t,s,resolvePrice(t),"MANUAL"));}catch(IllegalArgumentException|IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}}
    @PostMapping("/buy/amount") public ResponseEntity<?> buyAmount(@RequestBody Map<String,Object> body){try{String t=(String)body.get("ticker");double amount=((Number)body.get("amount")).doubleValue();if(amount<=0)throw new IllegalArgumentException("Amount must be positive");double price=resolvePrice(t);double shares=Math.round(amount/price*10000.0)/10000.0;return ResponseEntity.ok(trading.buy(t,shares,price,"MANUAL"));}catch(IllegalArgumentException|IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}}
    @PostMapping("/sell") public ResponseEntity<?> sell(@RequestBody Map<String,Object> body){try{String t=(String)body.get("ticker");double s=((Number)body.get("shares")).doubleValue();return ResponseEntity.ok(trading.sell(t,s,resolvePrice(t),"MANUAL"));}catch(IllegalArgumentException|IllegalStateException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}}
    private double resolvePrice(String ticker){if(ticker==null||ticker.isBlank())throw new IllegalArgumentException("Ticker is required");String t=ticker.trim().toUpperCase(Locale.ROOT);double live=livePrices.getPrice(t);if(live>0)return live;return stocks.getByTicker(t).map(s->s.getCurrentPrice()).orElseGet(()->etfs.getByTicker(t).map(e->e.getCurrentPrice()).orElseThrow(()->new IllegalArgumentException("Unknown ticker: "+t)));}
}
