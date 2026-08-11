package com.marketcompass.investment.model;

/**
 * NYSE/NASDAQ session states.
 * Times are Eastern Time (ET).
 *
 *  CLOSED      — before 4 AM, after 8 PM, or weekend
 *  PRE_MARKET  — 4:00 AM → 9:29 AM ET
 *  OPEN        — 9:30 AM → 3:59 PM ET  (regular session)
 *  AFTER_HOURS — 4:00 PM → 7:59 PM ET
 */
public enum MarketSession {
    CLOSED,
    PRE_MARKET,
    OPEN,
    AFTER_HOURS;

    public String label() {
        return switch (this) {
            case OPEN        -> "Regular Session";
            case PRE_MARKET  -> "Pre-Market";
            case AFTER_HOURS -> "After Hours";
            case CLOSED      -> "Market Closed";
        };
    }

    public String badge() {
        return switch (this) {
            case OPEN        -> "🟢";
            case PRE_MARKET  -> "🟡";
            case AFTER_HOURS -> "🟠";
            case CLOSED      -> "🔴";
        };
    }
}
