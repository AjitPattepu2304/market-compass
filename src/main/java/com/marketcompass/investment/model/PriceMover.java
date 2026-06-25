package com.marketcompass.investment.model;

/**
 * Represents a ticker's price movement relative to the session open.
 * Used by the market simulation to surface top gainers / losers.
 */
public class PriceMover {

    private final String ticker;
    private final String name;
    private final double openPrice;
    private final double currentPrice;
    private final double changePercent;

    public PriceMover(String ticker, String name, double openPrice,
                      double currentPrice, double changePercent) {
        this.ticker        = ticker;
        this.name          = name;
        this.openPrice     = openPrice;
        this.currentPrice  = currentPrice;
        this.changePercent = changePercent;
    }

    public String getTicker()        { return ticker; }
    public String getName()          { return name; }
    public double getOpenPrice()     { return openPrice; }
    public double getCurrentPrice()  { return currentPrice; }
    public double getChangePercent() { return changePercent; }

    /** UP / DOWN / FLAT based on % change magnitude. */
    public String getDirection() {
        if (changePercent >  0.05) return "UP";
        if (changePercent < -0.05) return "DOWN";
        return "FLAT";
    }
}
