package com.product_office_setup.product_office_setup.investment.model;

import java.util.List;

/**
 * An investment opportunity identified by the MarketCompass Brokerage Agent.
 *
 * The agent scans all stocks, ETFs, and dividends and surfaces
 * actionable opportunities across four strategies:
 *
 *   VALUE       — stocks trading at low P/E, near 52-week lows, with stable fundamentals
 *   INCOME      — high-yield dividend stocks with sustainable payout and growth history
 *   CHIP_SECTOR — semiconductor opportunities based on chip-cycle and AI demand analysis
 *   ETF         — most efficient ETF in each category by cost vs. performance
 *
 * Signals:
 *   STRONG_BUY  — Score ≥ 80  — high conviction, multiple indicators aligned
 *   BUY         — Score 65-79 — good opportunity, worth allocating
 *   WATCH       — Score 50-64 — on the radar, wait for better entry or more confirmation
 *   HOLD        — Score < 50  — no compelling action at current levels
 */
public class Opportunity {

    private final String ticker;
    private final String companyName;
    private final String sector;
    private final String opportunityType;   // VALUE, INCOME, CHIP_SECTOR, ETF
    private final String signal;            // STRONG_BUY, BUY, WATCH, HOLD
    private final int score;                // 0–100
    private final String reasoning;        // One-paragraph agent reasoning
    private final List<String> keyPoints;  // Bullet-point highlights
    private final double currentPrice;
    private final String chipType;         // null for non-chip stocks

    private Opportunity(Builder b) {
        this.ticker        = b.ticker;
        this.companyName   = b.companyName;
        this.sector        = b.sector;
        this.opportunityType = b.opportunityType;
        this.signal        = b.signal;
        this.score         = b.score;
        this.reasoning     = b.reasoning;
        this.keyPoints     = b.keyPoints;
        this.currentPrice  = b.currentPrice;
        this.chipType      = b.chipType;
    }

    public static Builder builder() { return new Builder(); }

    public String getTicker()          { return ticker; }
    public String getCompanyName()     { return companyName; }
    public String getSector()          { return sector; }
    public String getOpportunityType() { return opportunityType; }
    public String getSignal()          { return signal; }
    public int    getScore()           { return score; }
    public String getReasoning()       { return reasoning; }
    public List<String> getKeyPoints() { return keyPoints; }
    public double getCurrentPrice()    { return currentPrice; }
    public String getChipType()        { return chipType; }

    public String getSignalEmoji() {
        return switch (signal) {
            case "STRONG_BUY" -> "🟢";
            case "BUY"        -> "🔵";
            case "WATCH"      -> "🟡";
            default           -> "⚪";
        };
    }

    public static class Builder {
        private String ticker, companyName, sector, opportunityType, signal, reasoning, chipType;
        private int score;
        private List<String> keyPoints;
        private double currentPrice;

        public Builder ticker(String v)          { ticker = v; return this; }
        public Builder companyName(String v)     { companyName = v; return this; }
        public Builder sector(String v)          { sector = v; return this; }
        public Builder opportunityType(String v) { opportunityType = v; return this; }
        public Builder signal(String v)          { signal = v; return this; }
        public Builder score(int v)              { score = v; return this; }
        public Builder reasoning(String v)       { reasoning = v; return this; }
        public Builder keyPoints(List<String> v) { keyPoints = v; return this; }
        public Builder currentPrice(double v)    { currentPrice = v; return this; }
        public Builder chipType(String v)        { chipType = v; return this; }
        public Opportunity build()               { return new Opportunity(this); }
    }
}
