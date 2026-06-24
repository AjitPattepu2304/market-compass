package com.marketcompass.investment.service;

import com.marketcompass.investment.model.PortfolioHolding;
import com.marketcompass.investment.model.TradeRecord;
import com.marketcompass.investment.model.VirtualWallet;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Manages the virtual paper-trading wallet.
 *
 * Starting balance: $25,000
 * Supports: BUY, SELL, balance summary, trade history.
 * All prices pulled from StockService / ETFService (mock data).
 */
@Service
public class VirtualWalletService {

    private static final double STARTING_BALANCE = 25_000.00;

    private VirtualWallet wallet;
    private final List<TradeRecord> tradeHistory = new ArrayList<>();
    private long tradeCounter = 0;

    private final StockService stockService;
    private final ETFService etfService;
    private final PortfolioService portfolioService;

    public VirtualWalletService(StockService stockService,
                                ETFService etfService,
                                PortfolioService portfolioService) {
        this.stockService = stockService;
        this.etfService = etfService;
        this.portfolioService = portfolioService;
    }

    @PostConstruct
    public void init() {
        wallet = new VirtualWallet(STARTING_BALANCE);
        refreshPortfolioValue();
    }

    // ─── Wallet Summary ────────────────────────────────────────────────────────

    public Map<String, Object> getWalletSummary() {
        refreshPortfolioValue();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("startingBalance",     wallet.getStartingBalance());
        summary.put("availableBalance",    wallet.getAvailableBalance());
        summary.put("investedAmount",      wallet.getInvestedAmount());
        summary.put("totalPortfolioValue", wallet.getTotalPortfolioValue());
        summary.put("totalAccountValue",   wallet.getTotalAccountValue());
        summary.put("realizedGainLoss",    wallet.getRealizedGainLoss());
        summary.put("totalReturnPercent",  Math.round(wallet.getTotalReturnPercent() * 100.0) / 100.0);
        summary.put("tradeCount",          tradeHistory.size());
        return summary;
    }

    // ─── BUY ───────────────────────────────────────────────────────────────────

    /**
     * Buy by share count. e.g. buy("AAPL", 5, "MANUAL")
     */
    public TradeRecord buy(String ticker, double shares, String source) {
        final String t = ticker.toUpperCase();
        double price = resolvePrice(t);
        double total = Math.round(shares * price * 100.0) / 100.0;

        wallet.debit(total);

        // Update or create holding in portfolio
        updateHoldingOnBuy(t, shares, price);

        TradeRecord record = TradeRecord.builder()
                .id(nextTradeId())
                .side("BUY")
                .ticker(t)
                .assetName(resolveName(t))
                .assetType(resolveType(t))
                .shares(shares)
                .executedPrice(price)
                .gainLoss(0)
                .source(source)
                .balanceAfter(wallet.getAvailableBalance())
                .build();

        tradeHistory.add(record);
        refreshPortfolioValue();
        return record;
    }

    /**
     * Buy by dollar amount (SIP-style). e.g. invest $500 in AAPL
     */
    public TradeRecord buyByAmount(String ticker, double dollarAmount, String source) {
        ticker = ticker.toUpperCase();
        double price = resolvePrice(ticker);
        double shares = Math.round((dollarAmount / price) * 10000.0) / 10000.0; // 4 decimal places
        return buy(ticker, shares, source);
    }

    // ─── SELL ──────────────────────────────────────────────────────────────────

    /**
     * Sell by share count. Calculates realized gain/loss vs. avg cost basis.
     */
    public TradeRecord sell(String ticker, double sharesToSell, String source) {
        final String t = ticker.toUpperCase();

        Optional<PortfolioHolding> holdingOpt = portfolioService.getHoldings().stream()
                .filter(h -> h.getTicker().equals(t))
                .findFirst();

        if (holdingOpt.isEmpty()) {
            throw new IllegalArgumentException("No holding found for ticker: " + t);
        }

        PortfolioHolding holding = holdingOpt.get();
        if (sharesToSell > holding.getShares()) {
            throw new IllegalArgumentException(
                    "Cannot sell " + sharesToSell + " shares — only " + holding.getShares() + " held");
        }

        double price = resolvePrice(t);
        double proceeds = Math.round(sharesToSell * price * 100.0) / 100.0;
        double costBasis = Math.round(sharesToSell * holding.getAvgCostBasis() * 100.0) / 100.0;
        double gainLoss = Math.round((proceeds - costBasis) * 100.0) / 100.0;

        wallet.credit(proceeds, costBasis);
        updateHoldingOnSell(t, sharesToSell, holding);

        TradeRecord record = TradeRecord.builder()
                .id(nextTradeId())
                .side("SELL")
                .ticker(t)
                .assetName(holding.getName())
                .assetType(holding.getType())
                .shares(sharesToSell)
                .executedPrice(price)
                .gainLoss(gainLoss)
                .source(source)
                .balanceAfter(wallet.getAvailableBalance())
                .build();

        tradeHistory.add(record);
        refreshPortfolioValue();
        return record;
    }

    // ─── History ───────────────────────────────────────────────────────────────

    public List<TradeRecord> getTradeHistory() {
        return Collections.unmodifiableList(tradeHistory);
    }

    public List<TradeRecord> getTradeHistoryByTicker(String ticker) {
        return tradeHistory.stream()
                .filter(t -> t.getTicker().equalsIgnoreCase(ticker))
                .toList();
    }

    // ─── Internal Helpers ──────────────────────────────────────────────────────

    private void updateHoldingOnBuy(String ticker, double shares, double price) {
        Optional<PortfolioHolding> existing = portfolioService.getHoldings().stream()
                .filter(h -> h.getTicker().equals(ticker))
                .findFirst();

        if (existing.isPresent()) {
            PortfolioHolding h = existing.get();
            double totalShares = h.getShares() + shares;
            double newAvgCost = ((h.getShares() * h.getAvgCostBasis()) + (shares * price)) / totalShares;
            h.setShares(Math.round(totalShares * 10000.0) / 10000.0);
            h.setAvgCostBasis(Math.round(newAvgCost * 100.0) / 100.0);
            h.setCurrentPrice(price);
        } else {
            PortfolioHolding holding = new PortfolioHolding();
            holding.setTicker(ticker);
            holding.setName(resolveName(ticker));
            holding.setType(resolveType(ticker));
            holding.setSector(resolveSector(ticker));
            holding.setShares(shares);
            holding.setAvgCostBasis(price);
            holding.setCurrentPrice(price);
            holding.setAnnualDividendPerShare(resolveDividend(ticker));
            portfolioService.addHolding(holding);
        }
    }

    private void updateHoldingOnSell(String ticker, double sharesToSell, PortfolioHolding holding) {
        double remaining = Math.round((holding.getShares() - sharesToSell) * 10000.0) / 10000.0;
        if (remaining <= 0.0001) {
            portfolioService.removeHolding(ticker);
        } else {
            holding.setShares(remaining);
            holding.setCurrentPrice(resolvePrice(ticker));
        }
    }

    private void refreshPortfolioValue() {
        double total = portfolioService.getHoldings().stream()
                .mapToDouble(PortfolioHolding::getMarketValue)
                .sum();
        wallet.setTotalPortfolioValue(total);
    }

    private double resolvePrice(String ticker) {
        return stockService.getByTicker(ticker)
                .map(s -> s.getCurrentPrice())
                .orElseGet(() -> etfService.getByTicker(ticker)
                        .map(e -> e.getCurrentPrice())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + ticker)));
    }

    private String resolveName(String ticker) {
        return stockService.getByTicker(ticker).map(s -> s.getCompanyName())
                .orElseGet(() -> etfService.getByTicker(ticker).map(e -> e.getName()).orElse(ticker));
    }

    private String resolveType(String ticker) {
        return stockService.getByTicker(ticker).isPresent() ? "STOCK" : "ETF";
    }

    private String resolveSector(String ticker) {
        return stockService.getByTicker(ticker).map(s -> s.getSector())
                .orElseGet(() -> etfService.getByTicker(ticker).map(e -> e.getCategory()).orElse("Unknown"));
    }

    private double resolveDividend(String ticker) {
        return stockService.getByTicker(ticker).map(s -> s.getAnnualDividendPerShare())
                .orElseGet(() -> etfService.getByTicker(ticker)
                        .map(e -> e.getCurrentPrice() * (e.getDistributionYieldPercent() / 100))
                        .orElse(0.0));
    }

    private String nextTradeId() {
        return "TRD-" + String.format("%04d", ++tradeCounter);
    }
}
