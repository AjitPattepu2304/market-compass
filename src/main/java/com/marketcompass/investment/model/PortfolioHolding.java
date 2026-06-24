package com.marketcompass.investment.model;

/**
 * A single holding in a portfolio.
 *
 * Key concepts:
 * - Cost Basis:      The original price you paid per share.
 * - Market Value:    Current value = current price × shares.
 * - Unrealized G/L:  (currentPrice - avgCostBasis) × shares — paper profit/loss.
 * - Yield on Cost:   Dividend income / what you originally paid (grows over time with DRIP).
 */
public class PortfolioHolding {

    private String ticker;
    private String name;
    private String type;   // "STOCK" or "ETF"
    private String sector;
    private double shares;
    private double avgCostBasis;
    private double currentPrice;
    private double annualDividendPerShare;

    public PortfolioHolding() {}

    private PortfolioHolding(Builder b) {
        this.ticker = b.ticker;
        this.name = b.name;
        this.type = b.type;
        this.sector = b.sector;
        this.shares = b.shares;
        this.avgCostBasis = b.avgCostBasis;
        this.currentPrice = b.currentPrice;
        this.annualDividendPerShare = b.annualDividendPerShare;
    }

    public static Builder builder() { return new Builder(); }

    // ── Getters & Setters (mutable — current price is updated from service layer) ─
    public String getTicker()               { return ticker; }
    public void setTicker(String v)         { ticker = v; }
    public String getName()                 { return name; }
    public void setName(String v)           { name = v; }
    public String getType()                 { return type; }
    public void setType(String v)           { type = v; }
    public String getSector()               { return sector; }
    public void setSector(String v)         { sector = v; }
    public double getShares()               { return shares; }
    public void setShares(double v)         { shares = v; }
    public double getAvgCostBasis()         { return avgCostBasis; }
    public void setAvgCostBasis(double v)   { avgCostBasis = v; }
    public double getCurrentPrice()         { return currentPrice; }
    public void setCurrentPrice(double v)   { currentPrice = v; }
    public double getAnnualDividendPerShare(){ return annualDividendPerShare; }
    public void setAnnualDividendPerShare(double v){ annualDividendPerShare = v; }

    // ── Computed (Jackson serializes all public getters) ──────────────────────────
    public double getMarketValue()          { return shares * currentPrice; }
    public double getTotalCostBasis()       { return shares * avgCostBasis; }
    public double getUnrealizedGainLoss()   { return getMarketValue() - getTotalCostBasis(); }
    public double getTotalReturnPercent()   { return avgCostBasis == 0 ? 0 : ((currentPrice - avgCostBasis) / avgCostBasis) * 100; }
    public double getAnnualDividendIncome() { return shares * annualDividendPerShare; }
    public double getYieldOnCost()          { return avgCostBasis == 0 ? 0 : (annualDividendPerShare / avgCostBasis) * 100; }

    // ── Builder ───────────────────────────────────────────────────────────────────
    public static class Builder {
        private String ticker, name, type, sector;
        private double shares, avgCostBasis, currentPrice, annualDividendPerShare;

        public Builder ticker(String v)               { ticker = v; return this; }
        public Builder name(String v)                 { name = v; return this; }
        public Builder type(String v)                 { type = v; return this; }
        public Builder sector(String v)               { sector = v; return this; }
        public Builder shares(double v)               { shares = v; return this; }
        public Builder avgCostBasis(double v)         { avgCostBasis = v; return this; }
        public Builder currentPrice(double v)         { currentPrice = v; return this; }
        public Builder annualDividendPerShare(double v){ annualDividendPerShare = v; return this; }
        public PortfolioHolding build()               { return new PortfolioHolding(this); }
    }
}
