package com.marketcompass.investment.service;

import com.marketcompass.investment.model.WatchlistItem;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the watchlist — tickers the user is monitoring for buying opportunities.
 *
 * - Add a ticker with optional buy target, alert threshold, and notes
 * - Remove a ticker
 * - List all items with refreshed current prices
 * - Get triggered alerts (price at/below target or drop % exceeded)
 */
@Service
public class WatchlistService {

    private final Map<String, WatchlistItem> watchlist = new LinkedHashMap<>();
    private final StockService    stockService;
    private final ETFService      etfService;
    private final LivePriceService livePriceService;

    public WatchlistService(StockService stockService, ETFService etfService,
                            LivePriceService livePriceService) {
        this.stockService     = stockService;
        this.etfService       = etfService;
        this.livePriceService = livePriceService;
    }

    // ─── Add ───────────────────────────────────────────────────────────────────

    /**
     * Add a ticker to the watchlist.
     *
     * @param ticker         Stock or ETF symbol
     * @param targetPrice    Buy target price (0 = not set)
     * @param alertThreshold % drop from add-price to alert (0 = not set)
     * @param notes          Personal note/thesis
     */
    public WatchlistItem add(String ticker, double targetPrice, double alertThreshold, String notes) {
        final String t = ticker.toUpperCase();

        if (watchlist.containsKey(t)) {
            throw new IllegalArgumentException(t + " is already on your watchlist");
        }

        double price = resolvePrice(t);
        String name = resolveName(t);
        String type = resolveType(t);
        String sector = resolveSector(t);

        WatchlistItem item = new WatchlistItem(t, name, type, sector, price, targetPrice, alertThreshold, notes, price);
        watchlist.put(t, item);
        return item;
    }

    // ─── Remove ────────────────────────────────────────────────────────────────

    public boolean remove(String ticker) {
        return watchlist.remove(ticker.toUpperCase()) != null;
    }

    // ─── List ──────────────────────────────────────────────────────────────────

    /** Returns all watchlist items with refreshed current prices. */
    public List<WatchlistItem> getAll() {
        watchlist.values().forEach(item -> {
            try {
                item.setCurrentPrice(resolvePrice(item.getTicker()));
            } catch (Exception ignored) {}
        });
        return new ArrayList<>(watchlist.values());
    }

    public Optional<WatchlistItem> getByTicker(String ticker) {
        WatchlistItem item = watchlist.get(ticker.toUpperCase());
        if (item != null) {
            try { item.setCurrentPrice(resolvePrice(item.getTicker())); } catch (Exception ignored) {}
        }
        return Optional.ofNullable(item);
    }

    // ─── Alerts ────────────────────────────────────────────────────────────────

    /** Returns items where price has hit the buy target or drop threshold. */
    public List<Map<String, Object>> getAlerts() {
        return getAll().stream()
                .filter(item -> item.isAtOrBelowTarget() || item.isAlertTriggered())
                .map(item -> {
                    Map<String, Object> alert = new LinkedHashMap<>();
                    alert.put("ticker", item.getTicker());
                    alert.put("assetName", item.getAssetName());
                    alert.put("currentPrice", item.getCurrentPrice());
                    alert.put("priceAtAdd", item.getPriceAtAdd());
                    alert.put("changeFromAddPercent", item.getChangeFromAddPercent());
                    alert.put("targetPrice", item.getTargetPrice());
                    alert.put("alertThreshold", item.getAlertThreshold());
                    alert.put("atOrBelowTarget", item.isAtOrBelowTarget());
                    alert.put("dropAlertTriggered", item.isAlertTriggered());
                    return alert;
                })
                .collect(Collectors.toList());
    }

    // ─── Summary ───────────────────────────────────────────────────────────────

    public Map<String, Object> getSummary() {
        List<WatchlistItem> all = getAll();
        long alertCount = all.stream().filter(i -> i.isAtOrBelowTarget() || i.isAlertTriggered()).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalWatching", all.size());
        summary.put("activeAlerts", alertCount);
        summary.put("items", all);
        return summary;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private double resolvePrice(String ticker) {
        double live = livePriceService.getPrice(ticker);
        if (live > 0) return live;
        return stockService.getByTicker(ticker)
                .map(s -> s.getCurrentPrice())
                .orElseGet(() -> etfService.getByTicker(ticker)
                        .map(e -> e.getCurrentPrice())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + ticker)));
    }

    private String resolveName(String ticker) {
        return stockService.getByTicker(ticker).map(s -> s.getCompanyName())
                .orElseGet(() -> etfService.getByTicker(ticker).map(e -> e.getName()).orElse(ticker));
    }

    private String resolveType(String ticker) {
        return stockService.getByTicker(ticker).isPresent() ? "STOCK" : "ETF";
    }

    private String resolveSector(String ticker) {
        return stockService.getByTicker(ticker).map(s -> s.getSector())
                .orElseGet(() -> etfService.getByTicker(ticker).map(e -> e.getCategory()).orElse("Unknown"));
    }
}
