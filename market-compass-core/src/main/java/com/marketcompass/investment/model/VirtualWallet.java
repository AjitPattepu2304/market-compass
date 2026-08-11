package com.marketcompass.investment.model;

/**
 * Virtual paper-trading wallet with a fixed starting balance of $25,000.
 *
 * No real money — used for simulated investing, SIP, and agent-driven trades.
 *
 * - startingBalance:  Fixed seed amount ($25,000).
 * - availableBalance: Cash on hand — decreases on buy, increases on sell.
 * - investedAmount:   Total cost basis of open positions.
 * - totalPortfolioValue: investedAmount at current market prices (updated on summary calls).
 * - realizedGainLoss: Locked-in profit/loss from completed sell trades.
 */
public class VirtualWallet {

    private final double startingBalance;
    private double availableBalance;
    private double investedAmount;
    private double totalPortfolioValue;
    private double realizedGainLoss;

    public VirtualWallet(double startingBalance) {
        this.startingBalance = startingBalance;
        this.availableBalance = startingBalance;
        this.investedAmount = 0;
        this.totalPortfolioValue = 0;
        this.realizedGainLoss = 0;
    }

    public double getStartingBalance()      { return startingBalance; }
    public double getAvailableBalance()     { return availableBalance; }
    public double getInvestedAmount()       { return investedAmount; }
    public double getTotalPortfolioValue()  { return totalPortfolioValue; }
    public double getRealizedGainLoss()     { return realizedGainLoss; }

    public double getTotalAccountValue()    { return availableBalance + totalPortfolioValue; }
    public double getTotalReturnPercent() {
        return startingBalance > 0
                ? ((getTotalAccountValue() - startingBalance) / startingBalance) * 100
                : 0;
    }

    public void debit(double amount) {
        if (amount > availableBalance) throw new IllegalStateException("Insufficient balance");
        availableBalance = round(availableBalance - amount);
        investedAmount = round(investedAmount + amount);
    }

    public void credit(double proceeds, double costBasis) {
        availableBalance = round(availableBalance + proceeds);
        investedAmount = round(Math.max(0, investedAmount - costBasis));
        realizedGainLoss = round(realizedGainLoss + (proceeds - costBasis));
    }

    public void setTotalPortfolioValue(double v) { totalPortfolioValue = round(v); }

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
