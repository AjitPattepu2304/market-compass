package com.marketcompass.investment.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An immutable record of a completed buy or sell trade.
 *
 * - side:         "BUY" or "SELL"
 * - shares:       Number of shares traded (can be fractional for SIP)
 * - executedPrice: Price per share at execution
 * - totalAmount:   shares × executedPrice
 * - gainLoss:      Only meaningful for SELL — profit/loss vs. avg cost basis
 */
public class TradeRecord {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final String side;       // BUY | SELL
    private final String ticker;
    private final String assetName;
    private final String assetType;  // STOCK | ETF
    private final double shares;
    private final double executedPrice;
    private final double totalAmount;
    private final double gainLoss;   // 0 for BUY
    private final String source;     // MANUAL | SIP | AGENT
    private final String executedAt;
    private final double balanceAfter;

    private TradeRecord(Builder b) {
        this.id = b.id;
        this.side = b.side;
        this.ticker = b.ticker;
        this.assetName = b.assetName;
        this.assetType = b.assetType;
        this.shares = b.shares;
        this.executedPrice = b.executedPrice;
        this.totalAmount = Math.round(b.shares * b.executedPrice * 100.0) / 100.0;
        this.gainLoss = b.gainLoss;
        this.source = b.source;
        this.executedAt = LocalDateTime.now().format(FMT);
        this.balanceAfter = b.balanceAfter;
    }

    public String getId()             { return id; }
    public String getSide()           { return side; }
    public String getTicker()         { return ticker; }
    public String getAssetName()      { return assetName; }
    public String getAssetType()      { return assetType; }
    public double getShares()         { return shares; }
    public double getExecutedPrice()  { return executedPrice; }
    public double getTotalAmount()    { return totalAmount; }
    public double getGainLoss()       { return gainLoss; }
    public String getSource()         { return source; }
    public String getExecutedAt()     { return executedAt; }
    public double getBalanceAfter()   { return balanceAfter; }

    public static Builder builder()   { return new Builder(); }

    public static class Builder {
        private String id, side, ticker, assetName, assetType, source;
        private double shares, executedPrice, gainLoss, balanceAfter;

        public Builder id(String v)            { id = v; return this; }
        public Builder side(String v)          { side = v; return this; }
        public Builder ticker(String v)        { ticker = v; return this; }
        public Builder assetName(String v)     { assetName = v; return this; }
        public Builder assetType(String v)     { assetType = v; return this; }
        public Builder shares(double v)        { shares = v; return this; }
        public Builder executedPrice(double v) { executedPrice = v; return this; }
        public Builder gainLoss(double v)      { gainLoss = v; return this; }
        public Builder source(String v)        { source = v; return this; }
        public Builder balanceAfter(double v)  { balanceAfter = v; return this; }
        public TradeRecord build()             { return new TradeRecord(this); }
    }
}
