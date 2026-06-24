package com.marketcompass.investment.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A single ticker on the watchlist.
 *
 * - ticker:          Stock/ETF symbol being watched
 * - priceAtAdd:      Price when added — used to track movement since watching
 * - targetPrice:     Optional buy target — agent alerts when price drops to this
 * - alertThreshold:  Optional % drop from priceAtAdd to trigger alert (e.g. 5.0 = alert at -5%)
 * - notes:           Personal notes (why you're watching, thesis, etc.)
 * - currentPrice:    Refreshed on each list call from StockService/ETFService
 * - changeFromAdd:   % change from priceAtAdd to currentPrice
 */
public class WatchlistItem {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String ticker;
    private final String assetName;
    private final String assetType;   // STOCK | ETF
    private final String sector;
    private final double priceAtAdd;
    private final double targetPrice;  // 0 = not set
    private final double alertThreshold; // % drop — 0 = not set
    private final String notes;
    private final String addedAt;

    // mutable — updated on each fetch
    private double currentPrice;

    public WatchlistItem(String ticker, String assetName, String assetType, String sector,
                         double priceAtAdd, double targetPrice, double alertThreshold,
                         String notes, double currentPrice) {
        this.ticker = ticker.toUpperCase();
        this.assetName = assetName;
        this.assetType = assetType;
        this.sector = sector;
        this.priceAtAdd = priceAtAdd;
        this.targetPrice = targetPrice;
        this.alertThreshold = alertThreshold;
        this.notes = notes;
        this.currentPrice = currentPrice;
        this.addedAt = LocalDateTime.now().format(FMT);
    }

    public String getTicker()           { return ticker; }
    public String getAssetName()        { return assetName; }
    public String getAssetType()        { return assetType; }
    public String getSector()           { return sector; }
    public double getPriceAtAdd()       { return priceAtAdd; }
    public double getTargetPrice()      { return targetPrice; }
    public double getAlertThreshold()   { return alertThreshold; }
    public String getNotes()            { return notes; }
    public String getAddedAt()          { return addedAt; }
    public double getCurrentPrice()     { return currentPrice; }
    public void setCurrentPrice(double v) { currentPrice = v; }

    public double getChangeFromAddPercent() {
        if (priceAtAdd == 0) return 0;
        return Math.round(((currentPrice - priceAtAdd) / priceAtAdd) * 10000.0) / 100.0;
    }

    public boolean isAtOrBelowTarget() {
        return targetPrice > 0 && currentPrice <= targetPrice;
    }

    public boolean isAlertTriggered() {
        if (alertThreshold <= 0) return false;
        return getChangeFromAddPercent() <= -alertThreshold;
    }
}
