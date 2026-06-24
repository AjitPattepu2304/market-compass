package com.product_office_setup.product_office_setup.investment.model;

/**
 * Represents an Exchange-Traded Fund (ETF).
 *
 * Key concepts:
 * - ETF:           A basket of securities traded on an exchange like a stock.
 * - expenseRatio:  Annual fee charged by the fund (e.g. 0.03% = $3/yr per $10,000).
 * - index fund:    ETF tracking a market index passively — beats most active funds long-term.
 * - AUM:           Assets Under Management. Larger = more liquid.
 */
public class ETF {

    private final String ticker;
    private final String name;
    private final String issuer;
    private final String category;
    private final String indexTracked;
    private final double currentPrice;
    private final double expenseRatioPercent;
    private final double aumBillions;
    private final int holdingsCount;
    private final double distributionYieldPercent;
    private final double ytdReturnPercent;
    private final double oneYearReturnPercent;
    private final double fiveYearReturnPercent;
    private final double tenYearReturnPercent;
    private final String description;
    private final String investmentNote;

    private ETF(Builder b) {
        this.ticker = b.ticker;
        this.name = b.name;
        this.issuer = b.issuer;
        this.category = b.category;
        this.indexTracked = b.indexTracked;
        this.currentPrice = b.currentPrice;
        this.expenseRatioPercent = b.expenseRatioPercent;
        this.aumBillions = b.aumBillions;
        this.holdingsCount = b.holdingsCount;
        this.distributionYieldPercent = b.distributionYieldPercent;
        this.ytdReturnPercent = b.ytdReturnPercent;
        this.oneYearReturnPercent = b.oneYearReturnPercent;
        this.fiveYearReturnPercent = b.fiveYearReturnPercent;
        this.tenYearReturnPercent = b.tenYearReturnPercent;
        this.description = b.description;
        this.investmentNote = b.investmentNote;
    }

    public static Builder builder() { return new Builder(); }

    // ── Getters ───────────────────────────────────────────────────────────────────
    public String getTicker()                   { return ticker; }
    public String getName()                     { return name; }
    public String getIssuer()                   { return issuer; }
    public String getCategory()                 { return category; }
    public String getIndexTracked()             { return indexTracked; }
    public double getCurrentPrice()             { return currentPrice; }
    public double getExpenseRatioPercent()      { return expenseRatioPercent; }
    public double getAumBillions()              { return aumBillions; }
    public int getHoldingsCount()               { return holdingsCount; }
    public double getDistributionYieldPercent() { return distributionYieldPercent; }
    public double getYtdReturnPercent()         { return ytdReturnPercent; }
    public double getOneYearReturnPercent()     { return oneYearReturnPercent; }
    public double getFiveYearReturnPercent()    { return fiveYearReturnPercent; }
    public double getTenYearReturnPercent()     { return tenYearReturnPercent; }
    public String getDescription()              { return description; }
    public String getInvestmentNote()           { return investmentNote; }

    // ── Computed ──────────────────────────────────────────────────────────────────
    public double getAnnualCostFor10k() {
        return 10_000 * (expenseRatioPercent / 100);
    }
    public String getExpenseRatingLabel() {
        if (expenseRatioPercent <= 0.05) return "Excellent (Ultra-low cost)";
        if (expenseRatioPercent <= 0.20) return "Good (Low cost)";
        if (expenseRatioPercent <= 0.50) return "Fair (Moderate cost)";
        return "High (Consider alternatives)";
    }

    // ── Builder ───────────────────────────────────────────────────────────────────
    public static class Builder {
        private String ticker, name, issuer, category, indexTracked, description, investmentNote;
        private double currentPrice, expenseRatioPercent, aumBillions, distributionYieldPercent,
                ytdReturnPercent, oneYearReturnPercent, fiveYearReturnPercent, tenYearReturnPercent;
        private int holdingsCount;

        public Builder ticker(String v)                   { ticker = v; return this; }
        public Builder name(String v)                     { name = v; return this; }
        public Builder issuer(String v)                   { issuer = v; return this; }
        public Builder category(String v)                 { category = v; return this; }
        public Builder indexTracked(String v)             { indexTracked = v; return this; }
        public Builder currentPrice(double v)             { currentPrice = v; return this; }
        public Builder expenseRatioPercent(double v)      { expenseRatioPercent = v; return this; }
        public Builder aumBillions(double v)              { aumBillions = v; return this; }
        public Builder holdingsCount(int v)               { holdingsCount = v; return this; }
        public Builder distributionYieldPercent(double v) { distributionYieldPercent = v; return this; }
        public Builder ytdReturnPercent(double v)         { ytdReturnPercent = v; return this; }
        public Builder oneYearReturnPercent(double v)     { oneYearReturnPercent = v; return this; }
        public Builder fiveYearReturnPercent(double v)    { fiveYearReturnPercent = v; return this; }
        public Builder tenYearReturnPercent(double v)     { tenYearReturnPercent = v; return this; }
        public Builder description(String v)              { description = v; return this; }
        public Builder investmentNote(String v)           { investmentNote = v; return this; }
        public ETF build()                                { return new ETF(this); }
    }
}
