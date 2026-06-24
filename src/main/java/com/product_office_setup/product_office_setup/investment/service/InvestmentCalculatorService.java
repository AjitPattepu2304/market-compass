package com.product_office_setup.product_office_setup.investment.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Investment calculation service.
 *
 * Teaches core investing math concepts:
 * 1. Compound Interest — how money grows exponentially over time
 * 2. DRIP (Dividend Reinvestment) — compounding with dividend reinvestment
 * 3. Retirement Savings — effect of monthly contributions over time
 */
@Service
public class InvestmentCalculatorService {

    /**
     * Compound Interest: A = P × (1 + r/n)^(n×t)
     *
     * The most important equation in investing.
     * "Compound interest is the eighth wonder of the world." — Albert Einstein
     *
     * @param principal        Initial investment in dollars
     * @param annualRatePercent Annual interest/return rate (e.g. 7.0 for 7%)
     * @param years            Investment horizon in years
     * @param compoundsPerYear How many times interest compounds per year (1=annual, 12=monthly, 365=daily)
     */
    public Map<String, Object> compoundInterest(double principal, double annualRatePercent,
                                                 int years, int compoundsPerYear) {
        double r = annualRatePercent / 100.0;
        List<Map<String, Object>> yearlyData = new ArrayList<>();

        for (int year = 0; year <= years; year++) {
            double value = principal * Math.pow(1 + r / compoundsPerYear, (double) compoundsPerYear * year);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("year", year);
            point.put("value", round2(value));
            point.put("interest", round2(value - principal));
            yearlyData.add(point);
        }

        double finalAmount = principal * Math.pow(1 + r / compoundsPerYear, (double) compoundsPerYear * years);
        double totalInterest = finalAmount - principal;
        double moneyMultiplied = principal > 0 ? finalAmount / principal : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("principal", principal);
        result.put("annualRatePercent", annualRatePercent);
        result.put("years", years);
        result.put("compoundsPerYear", compoundsPerYear);
        result.put("finalAmount", round2(finalAmount));
        result.put("totalInterest", round2(totalInterest));
        result.put("moneyMultiplied", round2(moneyMultiplied));
        result.put("rule72Years", round2(72.0 / annualRatePercent)); // Rule of 72
        result.put("yearlyData", yearlyData);
        result.put("lesson", String.format(
                "$%.0f invested at %.1f%% for %d years becomes $%.0f — a %.1fx return. " +
                "The Rule of 72 says money doubles every %.1f years at this rate.",
                principal, annualRatePercent, years, finalAmount, moneyMultiplied, 72.0 / annualRatePercent));
        return result;
    }

    /**
     * DRIP (Dividend Reinvestment Plan) Projection.
     *
     * Shows the power of reinvesting dividends instead of spending them.
     * Each dividend payment buys more shares, which pay more dividends — exponential growth.
     *
     * @param investment             Initial investment in dollars
     * @param dividendYieldPercent   Starting annual dividend yield (e.g. 3.5 for 3.5%)
     * @param dividendGrowthPercent  Annual dividend per share growth rate (e.g. 6.0 for 6%)
     * @param priceGrowthPercent     Annual stock price appreciation (e.g. 7.0 for 7%)
     * @param years                  Projection horizon in years
     */
    public Map<String, Object> dripProjection(double investment, double dividendYieldPercent,
                                               double dividendGrowthPercent, double priceGrowthPercent,
                                               int years) {
        double pricePerShare = 100.0; // normalize starting price to $100
        double shares = investment / pricePerShare;
        double dividendPerShare = pricePerShare * (dividendYieldPercent / 100);
        double totalDividendsReceived = 0;
        double totalDividendsWithoutDrip = 0;
        double finalPortfolioValue = investment;   // tracked each year to avoid map cast

        List<Map<String, Object>> yearlyData = new ArrayList<>();
        Map<String, Object> y0 = new LinkedHashMap<>();
        y0.put("year", 0);
        y0.put("portfolioValue", round2(investment));
        y0.put("shares", round3(shares));
        y0.put("pricePerShare", round2(pricePerShare));
        y0.put("annualDividendIncome", 0.0);
        y0.put("totalDividendsReceived", 0.0);
        yearlyData.add(y0);

        for (int year = 1; year <= years; year++) {
            // Price appreciates
            pricePerShare *= (1 + priceGrowthPercent / 100);
            // Dividend income this year (before buying new shares)
            double yearlyDividend = shares * dividendPerShare;
            totalDividendsReceived += yearlyDividend;
            totalDividendsWithoutDrip += (investment / 100.0) * dividendPerShare; // original shares only
            // DRIP: buy new shares with dividend
            double newShares = yearlyDividend / pricePerShare;
            shares += newShares;
            // Dividend per share grows
            dividendPerShare *= (1 + dividendGrowthPercent / 100);

            double portfolioValue = shares * pricePerShare;
            finalPortfolioValue = portfolioValue;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("year", year);
            point.put("portfolioValue", round2(portfolioValue));
            point.put("shares", round3(shares));
            point.put("pricePerShare", round2(pricePerShare));
            point.put("annualDividendIncome", round2(yearlyDividend));
            point.put("totalDividendsReceived", round2(totalDividendsReceived));
            yearlyData.add(point);
        }

        double finalValueWithoutDrip = investment * Math.pow(1 + priceGrowthPercent / 100, years);
        double finalValueWithDrip = finalPortfolioValue;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("investment", investment);
        result.put("dividendYieldPercent", dividendYieldPercent);
        result.put("dividendGrowthPercent", dividendGrowthPercent);
        result.put("priceGrowthPercent", priceGrowthPercent);
        result.put("years", years);
        result.put("finalValueWithDRIP", round2(finalValueWithDrip));
        result.put("finalValueWithoutDRIP", round2(finalValueWithoutDrip + totalDividendsWithoutDrip));
        result.put("dripAdvantage", round2(finalValueWithDrip - (finalValueWithoutDrip + totalDividendsWithoutDrip)));
        result.put("totalDividendsReinvested", round2(totalDividendsReceived));
        result.put("yearlyData", yearlyData);
        result.put("lesson", String.format(
                "DRIP turns $%.0f into $%.0f vs $%.0f without reinvestment — a $%.0f advantage from reinvesting dividends alone.",
                investment, finalValueWithDrip, finalValueWithoutDrip + totalDividendsWithoutDrip,
                finalValueWithDrip - (finalValueWithoutDrip + totalDividendsWithoutDrip)));
        return result;
    }

    /**
     * Retirement / Savings Calculator.
     *
     * Shows how regular monthly contributions + compound growth builds wealth.
     * Uses Future Value of Annuity formula.
     *
     * @param monthlyContribution  Amount added each month in dollars
     * @param annualRatePercent    Expected annual return % (e.g. 7.0 for S&P 500 historical average)
     * @param years                Years until retirement
     * @param initialAmount        Lump sum already invested today
     */
    public Map<String, Object> retirementSavings(double monthlyContribution, double annualRatePercent,
                                                  int years, double initialAmount) {
        double monthlyRate = (annualRatePercent / 100) / 12;
        double balance = initialAmount;
        double totalContributed = initialAmount;

        List<Map<String, Object>> yearlyData = new ArrayList<>();
        Map<String, Object> y0 = new LinkedHashMap<>();
        y0.put("year", 0);
        y0.put("balance", round2(balance));
        y0.put("contributed", round2(totalContributed));
        y0.put("growth", 0.0);
        yearlyData.add(y0);

        for (int year = 1; year <= years; year++) {
            for (int month = 0; month < 12; month++) {
                balance = balance * (1 + monthlyRate) + monthlyContribution;
                totalContributed += monthlyContribution;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("year", year);
            point.put("balance", round2(balance));
            point.put("contributed", round2(totalContributed));
            point.put("growth", round2(balance - totalContributed));
            yearlyData.add(point);
        }

        double totalGrowth = balance - totalContributed;
        double growthPercent = totalContributed > 0 ? (totalGrowth / totalContributed) * 100 : 0;

        // Monthly income in retirement assuming 4% safe withdrawal rate
        double monthlyRetirementIncome = (balance * 0.04) / 12;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthlyContribution", monthlyContribution);
        result.put("annualRatePercent", annualRatePercent);
        result.put("years", years);
        result.put("initialAmount", initialAmount);
        result.put("finalBalance", round2(balance));
        result.put("totalContributed", round2(totalContributed));
        result.put("totalGrowth", round2(totalGrowth));
        result.put("growthPercent", round2(growthPercent));
        result.put("monthlyRetirementIncome4PctRule", round2(monthlyRetirementIncome));
        result.put("yearlyData", yearlyData);
        result.put("lesson", String.format(
                "Contributing $%.0f/month for %d years at %.1f%% grows to $%.0f. " +
                "You contributed $%.0f and compound growth added $%.0f. " +
                "The 4%% rule gives $%.0f/month in retirement.",
                monthlyContribution, years, annualRatePercent, balance,
                totalContributed, totalGrowth, monthlyRetirementIncome));
        return result;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
