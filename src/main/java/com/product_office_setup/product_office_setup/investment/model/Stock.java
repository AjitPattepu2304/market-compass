package com.product_office_setup.product_office_setup.investment.model;

/**
 * Represents a publicly traded stock.
 *
 * Key concepts:
 * - ticker:        Short symbol used on exchanges (e.g. "AAPL" for Apple)
 * - peRatio:       Price-to-Earnings ratio — how much investors pay per $1 of earnings.
 *                  Lower = potentially undervalued; higher = growth expectations priced in.
 * - dividendYield: Annual dividend / current price. e.g. 2.5% means $2.50/yr per $100 invested.
 * - marketCapBillions: Total company value (price × shares outstanding).
 *                  Mega-cap >$200B, Large-cap $10-200B, Mid-cap $2-10B, Small-cap <$2B.
 * - beta:          Volatility vs the market. Beta >1 = more volatile, <1 = more stable.
 */
public class Stock {

    private final String ticker;
    private final String companyName;
    private final String sector;
    private final String industry;
    private final double currentPrice;
    private final double marketCapBillions;
    private final double peRatio;
    private final double dividendYieldPercent;
    private final double annualDividendPerShare;
    private final double fiftyTwoWeekHigh;
    private final double fiftyTwoWeekLow;
    private final double oneYearReturnPercent;
    private final double beta;
    private final String description;
    private final String investmentNote;

    /**
     * Chip type — only set for semiconductor stocks. Null for all others.
     *
     * FABLESS:    Designs chips, outsources manufacturing (e.g. NVDA, AMD, QCOM).
     *             Lower capex, higher margins, dependent on foundries.
     * IDM:        Integrated Device Manufacturer — designs AND manufactures (e.g. INTC, TXN).
     *             More control, higher capex, can be slower to adapt.
     * FOUNDRY:    Pure-play manufacturer — only makes chips for others (e.g. TSM/TSMC).
     *             Extremely capital-intensive. Owns the most advanced fabs.
     * EQUIPMENT:  Makes the machines that manufacture chips (e.g. ASML, AMAT, LRCX).
     *             Picks-and-shovels play — profits regardless of which chip wins.
     * MEMORY:     Specializes in DRAM/NAND memory (e.g. MU/Micron). Highly cyclical.
     * ANALOG:     Analog/mixed-signal chips for industrial/automotive (e.g. TXN, ADI, ON).
     *             More stable, less cyclical than logic chips.
     */
    private final String chipType;  // null for non-chip stocks

    private Stock(Builder b) {
        this.ticker = b.ticker;
        this.companyName = b.companyName;
        this.sector = b.sector;
        this.industry = b.industry;
        this.currentPrice = b.currentPrice;
        this.marketCapBillions = b.marketCapBillions;
        this.peRatio = b.peRatio;
        this.dividendYieldPercent = b.dividendYieldPercent;
        this.annualDividendPerShare = b.annualDividendPerShare;
        this.fiftyTwoWeekHigh = b.fiftyTwoWeekHigh;
        this.fiftyTwoWeekLow = b.fiftyTwoWeekLow;
        this.oneYearReturnPercent = b.oneYearReturnPercent;
        this.beta = b.beta;
        this.description = b.description;
        this.investmentNote = b.investmentNote;
        this.chipType = b.chipType;
    }

    public static Builder builder() { return new Builder(); }

    // ── Getters ───────────────────────────────────────────────────────────────────
    public String getTicker()               { return ticker; }
    public String getCompanyName()          { return companyName; }
    public String getSector()               { return sector; }
    public String getIndustry()             { return industry; }
    public double getCurrentPrice()         { return currentPrice; }
    public double getMarketCapBillions()    { return marketCapBillions; }
    public double getPeRatio()              { return peRatio; }
    public double getDividendYieldPercent() { return dividendYieldPercent; }
    public double getAnnualDividendPerShare(){ return annualDividendPerShare; }
    public double getFiftyTwoWeekHigh()     { return fiftyTwoWeekHigh; }
    public double getFiftyTwoWeekLow()      { return fiftyTwoWeekLow; }
    public double getOneYearReturnPercent() { return oneYearReturnPercent; }
    public double getBeta()                 { return beta; }
    public String getDescription()          { return description; }
    public String getInvestmentNote()       { return investmentNote; }
    public String getChipType()             { return chipType; }
    public boolean isChipStock()            { return chipType != null; }

    // ── Computed properties (serialized to JSON by Jackson) ───────────────────────
    public String getSizeCategory() {
        if (marketCapBillions >= 200) return "Mega-Cap";
        if (marketCapBillions >= 10)  return "Large-Cap";
        if (marketCapBillions >= 2)   return "Mid-Cap";
        return "Small-Cap";
    }
    public boolean isPaysDividend() { return annualDividendPerShare > 0; }
    public double getPercentFromHigh() {
        return ((fiftyTwoWeekHigh - currentPrice) / fiftyTwoWeekHigh) * 100;
    }

    // ── Builder ───────────────────────────────────────────────────────────────────
    public static class Builder {
        private String ticker, companyName, sector, industry, description, investmentNote, chipType;
        private double currentPrice, marketCapBillions, peRatio, dividendYieldPercent,
                annualDividendPerShare, fiftyTwoWeekHigh, fiftyTwoWeekLow,
                oneYearReturnPercent, beta;

        public Builder ticker(String v)               { ticker = v; return this; }
        public Builder companyName(String v)          { companyName = v; return this; }
        public Builder sector(String v)               { sector = v; return this; }
        public Builder industry(String v)             { industry = v; return this; }
        public Builder currentPrice(double v)         { currentPrice = v; return this; }
        public Builder marketCapBillions(double v)    { marketCapBillions = v; return this; }
        public Builder peRatio(double v)              { peRatio = v; return this; }
        public Builder dividendYieldPercent(double v) { dividendYieldPercent = v; return this; }
        public Builder annualDividendPerShare(double v){ annualDividendPerShare = v; return this; }
        public Builder fiftyTwoWeekHigh(double v)     { fiftyTwoWeekHigh = v; return this; }
        public Builder fiftyTwoWeekLow(double v)      { fiftyTwoWeekLow = v; return this; }
        public Builder oneYearReturnPercent(double v) { oneYearReturnPercent = v; return this; }
        public Builder beta(double v)                 { beta = v; return this; }
        public Builder description(String v)          { description = v; return this; }
        public Builder investmentNote(String v)       { investmentNote = v; return this; }
        public Builder chipType(String v)             { chipType = v; return this; }
        public Stock build()                          { return new Stock(this); }
    }
}
