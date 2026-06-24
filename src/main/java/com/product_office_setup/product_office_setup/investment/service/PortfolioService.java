package com.product_office_setup.product_office_setup.investment.service;

import com.product_office_setup.product_office_setup.investment.model.PortfolioHolding;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final Map<String, PortfolioHolding> holdings = new LinkedHashMap<>();
    private final StockService stockService;
    private final ETFService etfService;

    public PortfolioService(StockService stockService, ETFService etfService) {
        this.stockService = stockService;
        this.etfService = etfService;
    }

    /** Seed with a sample portfolio so the user sees data right away */
    @PostConstruct
    public void init() {
        addHolding(PortfolioHolding.builder()
                .ticker("VTI").name("Vanguard Total Stock Market ETF").type("ETF").sector("Broad Market")
                .shares(15).avgCostBasis(210.00).currentPrice(237.00).annualDividendPerShare(3.27).build());

        addHolding(PortfolioHolding.builder()
                .ticker("SCHD").name("Schwab US Dividend Equity ETF").type("ETF").sector("Dividend")
                .shares(30).avgCostBasis(70.00).currentPrice(78.00).annualDividendPerShare(2.73).build());

        addHolding(PortfolioHolding.builder()
                .ticker("AAPL").name("Apple Inc.").type("STOCK").sector("Technology")
                .shares(10).avgCostBasis(145.00).currentPrice(192.35).annualDividendPerShare(0.96).build());

        addHolding(PortfolioHolding.builder()
                .ticker("KO").name("The Coca-Cola Company").type("STOCK").sector("Consumer Staples")
                .shares(25).avgCostBasis(55.00).currentPrice(62.50).annualDividendPerShare(1.84).build());

        addHolding(PortfolioHolding.builder()
                .ticker("JNJ").name("Johnson & Johnson").type("STOCK").sector("Healthcare")
                .shares(8).avgCostBasis(165.00).currentPrice(158.20).annualDividendPerShare(4.96).build());

        addHolding(PortfolioHolding.builder()
                .ticker("BND").name("Vanguard Total Bond Market ETF").type("ETF").sector("Bonds")
                .shares(20).avgCostBasis(80.00).currentPrice(73.50).annualDividendPerShare(3.09).build());
    }

    public List<PortfolioHolding> getHoldings() {
        return new ArrayList<>(holdings.values());
    }

    public void addHolding(PortfolioHolding holding) {
        // Try to enrich with live price from our data stores
        String ticker = holding.getTicker().toUpperCase();
        holding.setTicker(ticker);

        if ("STOCK".equalsIgnoreCase(holding.getType())) {
            stockService.getByTicker(ticker).ifPresent(s -> {
                holding.setCurrentPrice(s.getCurrentPrice());
                holding.setAnnualDividendPerShare(s.getAnnualDividendPerShare());
                if (holding.getName() == null || holding.getName().isBlank()) holding.setName(s.getCompanyName());
                if (holding.getSector() == null || holding.getSector().isBlank()) holding.setSector(s.getSector());
            });
        } else {
            etfService.getByTicker(ticker).ifPresent(e -> {
                holding.setCurrentPrice(e.getCurrentPrice());
                double annualDiv = e.getCurrentPrice() * (e.getDistributionYieldPercent() / 100);
                holding.setAnnualDividendPerShare(annualDiv);
                if (holding.getName() == null || holding.getName().isBlank()) holding.setName(e.getName());
                if (holding.getSector() == null || holding.getSector().isBlank()) holding.setSector(e.getCategory());
            });
        }

        holdings.put(ticker, holding);
    }

    public boolean removeHolding(String ticker) {
        return holdings.remove(ticker.toUpperCase()) != null;
    }

    public Map<String, Object> getSummary() {
        List<PortfolioHolding> list = getHoldings();

        double totalInvested = list.stream().mapToDouble(PortfolioHolding::getTotalCostBasis).sum();
        double currentValue = list.stream().mapToDouble(PortfolioHolding::getMarketValue).sum();
        double gainLoss = currentValue - totalInvested;
        double gainLossPercent = totalInvested > 0 ? (gainLoss / totalInvested) * 100 : 0;
        double annualDividendIncome = list.stream().mapToDouble(PortfolioHolding::getAnnualDividendIncome).sum();
        double portfolioYield = currentValue > 0 ? (annualDividendIncome / currentValue) * 100 : 0;

        // Sector allocation as % of portfolio
        Map<String, Double> sectorValues = new LinkedHashMap<>();
        for (PortfolioHolding h : list) {
            sectorValues.merge(h.getSector(), h.getMarketValue(), Double::sum);
        }
        Map<String, Double> sectorAllocation = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : sectorValues.entrySet()) {
            double pct = currentValue > 0 ? (e.getValue() / currentValue) * 100 : 0;
            sectorAllocation.put(e.getKey(), Math.round(pct * 10.0) / 10.0);
        }

        // Type allocation
        Map<String, Double> typeValues = list.stream()
                .collect(Collectors.groupingBy(PortfolioHolding::getType,
                        Collectors.summingDouble(PortfolioHolding::getMarketValue)));
        Map<String, Double> typeAllocation = new LinkedHashMap<>();
        typeValues.forEach((type, val) -> typeAllocation.put(type, Math.round((val / currentValue) * 1000.0) / 10.0));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("holdings", list);
        result.put("holdingsCount", list.size());
        result.put("totalInvested", Math.round(totalInvested * 100.0) / 100.0);
        result.put("currentValue", Math.round(currentValue * 100.0) / 100.0);
        result.put("gainLoss", Math.round(gainLoss * 100.0) / 100.0);
        result.put("gainLossPercent", Math.round(gainLossPercent * 100.0) / 100.0);
        result.put("annualDividendIncome", Math.round(annualDividendIncome * 100.0) / 100.0);
        result.put("monthlyDividendIncome", Math.round((annualDividendIncome / 12) * 100.0) / 100.0);
        result.put("portfolioYieldPercent", Math.round(portfolioYield * 100.0) / 100.0);
        result.put("sectorAllocation", sectorAllocation);
        result.put("typeAllocation", typeAllocation);
        return result;
    }
}
