package com.marketcompass.investment.model;

/**
 * Dividend information for a stock.
 *
 * Key concepts:
 * - Dividend Aristocrat: S&P 500 company with 25+ consecutive years of dividend increases.
 * - Dividend King:       Company with 50+ consecutive years of dividend increases.
 * - Payout Ratio:        Dividends / Earnings. Under 60% = sustainable, over 80% = risky.
 * - DRIP:               Dividend Reinvestment Plan — reinvest dividends to buy more shares.
 */
public class Dividend {

    private final String ticker;
    private final String companyName;
    private final String sector;
    private final double currentPrice;
    private final double annualDividendPerShare;
    private final double dividendYieldPercent;
    private final String paymentFrequency;
    private final double dividendPerPayment;
    private final double payoutRatioPercent;
    private final int consecutiveYearsOfDividendGrowth;
    private final double fiveYearDividendGrowthRatePercent;
    private final boolean isDividendAristocrat;
    private final boolean isDividendKing;
    private final String creditRating;
    private final String investmentNote;

    private Dividend(Builder b) {
        this.ticker = b.ticker;
        this.companyName = b.companyName;
        this.sector = b.sector;
        this.currentPrice = b.currentPrice;
        this.annualDividendPerShare = b.annualDividendPerShare;
        this.dividendYieldPercent = b.dividendYieldPercent;
        this.paymentFrequency = b.paymentFrequency;
        this.dividendPerPayment = b.dividendPerPayment;
        this.payoutRatioPercent = b.payoutRatioPercent;
        this.consecutiveYearsOfDividendGrowth = b.consecutiveYearsOfDividendGrowth;
        this.fiveYearDividendGrowthRatePercent = b.fiveYearDividendGrowthRatePercent;
        this.isDividendAristocrat = b.isDividendAristocrat;
        this.isDividendKing = b.isDividendKing;
        this.creditRating = b.creditRating;
        this.investmentNote = b.investmentNote;
    }

    public static Builder builder() { return new Builder(); }

    // ── Getters ───────────────────────────────────────────────────────────────────
    public String getTicker()                           { return ticker; }
    public String getCompanyName()                      { return companyName; }
    public String getSector()                           { return sector; }
    public double getCurrentPrice()                     { return currentPrice; }
    public double getAnnualDividendPerShare()           { return annualDividendPerShare; }
    public double getDividendYieldPercent()             { return dividendYieldPercent; }
    public String getPaymentFrequency()                 { return paymentFrequency; }
    public double getDividendPerPayment()               { return dividendPerPayment; }
    public double getPayoutRatioPercent()               { return payoutRatioPercent; }
    public int getConsecutiveYearsOfDividendGrowth()   { return consecutiveYearsOfDividendGrowth; }
    public double getFiveYearDividendGrowthRatePercent(){ return fiveYearDividendGrowthRatePercent; }
    public boolean isDividendAristocrat()               { return isDividendAristocrat; }
    public boolean isDividendKing()                     { return isDividendKing; }
    public String getCreditRating()                     { return creditRating; }
    public String getInvestmentNote()                   { return investmentNote; }

    // ── Computed ──────────────────────────────────────────────────────────────────
    public String getDividendAchievementLabel() {
        if (isDividendKing)        return "Dividend King (50+ years of growth)";
        if (isDividendAristocrat)  return "Dividend Aristocrat (25+ years of growth)";
        if (consecutiveYearsOfDividendGrowth >= 10) return "Dividend Contender (10+ years)";
        if (consecutiveYearsOfDividendGrowth >= 5)  return "Dividend Challenger (5+ years)";
        return "Dividend Payer";
    }
    public String getPayoutRatioHealth() {
        if (payoutRatioPercent < 30) return "Very Low — lots of room to grow";
        if (payoutRatioPercent < 60) return "Healthy — sustainable";
        if (payoutRatioPercent < 80) return "Moderate — watch earnings";
        return "High — dividend may be at risk";
    }
    public double getAnnualIncomeOn10k() {
        return 10_000 * (dividendYieldPercent / 100);
    }
    public int getPaymentsPerYear() {
        return switch (paymentFrequency) {
            case "MONTHLY" -> 12;
            case "SEMI_ANNUAL" -> 2;
            case "ANNUAL" -> 1;
            default -> 4;
        };
    }

    // ── Builder ───────────────────────────────────────────────────────────────────
    public static class Builder {
        private String ticker, companyName, sector, paymentFrequency, creditRating, investmentNote;
        private double currentPrice, annualDividendPerShare, dividendYieldPercent, dividendPerPayment,
                payoutRatioPercent, fiveYearDividendGrowthRatePercent;
        private int consecutiveYearsOfDividendGrowth;
        private boolean isDividendAristocrat, isDividendKing;

        public Builder ticker(String v)                             { ticker = v; return this; }
        public Builder companyName(String v)                        { companyName = v; return this; }
        public Builder sector(String v)                             { sector = v; return this; }
        public Builder currentPrice(double v)                       { currentPrice = v; return this; }
        public Builder annualDividendPerShare(double v)             { annualDividendPerShare = v; return this; }
        public Builder dividendYieldPercent(double v)               { dividendYieldPercent = v; return this; }
        public Builder paymentFrequency(String v)                   { paymentFrequency = v; return this; }
        public Builder dividendPerPayment(double v)                 { dividendPerPayment = v; return this; }
        public Builder payoutRatioPercent(double v)                 { payoutRatioPercent = v; return this; }
        public Builder consecutiveYearsOfDividendGrowth(int v)     { consecutiveYearsOfDividendGrowth = v; return this; }
        public Builder fiveYearDividendGrowthRatePercent(double v)  { fiveYearDividendGrowthRatePercent = v; return this; }
        public Builder isDividendAristocrat(boolean v)              { isDividendAristocrat = v; return this; }
        public Builder isDividendKing(boolean v)                    { isDividendKing = v; return this; }
        public Builder creditRating(String v)                       { creditRating = v; return this; }
        public Builder investmentNote(String v)                     { investmentNote = v; return this; }
        public Dividend build()                                     { return new Dividend(this); }
    }
}
