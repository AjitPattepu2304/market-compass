package com.marketcompass.investment.persistence;

import com.marketcompass.investment.model.PortfolioHolding;
import com.marketcompass.investment.service.ETFService;
import com.marketcompass.investment.service.StockService;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PostgreSQL-backed portfolio implementation used with the postgres profile.
 * Portfolio id 1 represents the single-user paper-trading portfolio until
 * authentication/multi-tenancy is introduced.
 */
@Service
@Profile("postgres")
public class PersistentPortfolioService {

    private static final long PORTFOLIO_ID = 1L;

    private final JdbcTemplate jdbc;
    private final StockService stockService;
    private final ETFService etfService;

    public PersistentPortfolioService(JdbcTemplate jdbc, StockService stockService, ETFService etfService) {
        this.jdbc = jdbc;
        this.stockService = stockService;
        this.etfService = etfService;
    }

    @Transactional
    public void initialize() {
        jdbc.update("""
            INSERT INTO portfolios (id, name, base_currency)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, PORTFOLIO_ID, "My Paper Portfolio", "USD");

        jdbc.update("""
            INSERT INTO paper_wallets (portfolio_id, starting_balance, available_balance)
            VALUES (?, ?, ?)
            ON CONFLICT (portfolio_id) DO NOTHING
            """, PORTFOLIO_ID, 25000.00, 25000.00);
    }

    public List<PortfolioHolding> getHoldings() {
        return jdbc.query("""
            SELECT ticker, name, asset_type, sector, quantity, average_cost,
                   current_price, annual_dividend_per_share
            FROM holdings
            WHERE portfolio_id = ?
            ORDER BY ticker
            """, this::mapHolding, PORTFOLIO_ID);
    }

    public Optional<PortfolioHolding> getHoldingByTicker(String ticker) {
        return jdbc.query("""
            SELECT ticker, name, asset_type, sector, quantity, average_cost,
                   current_price, annual_dividend_per_share
            FROM holdings
            WHERE portfolio_id = ? AND ticker = ?
            """, this::mapHolding, PORTFOLIO_ID, normalize(ticker)).stream().findFirst();
    }

    @Transactional
    public void addHolding(PortfolioHolding input) {
        PortfolioHolding h = enrich(input);
        String ticker = normalize(h.getTicker());

        jdbc.update("""
            INSERT INTO holdings
              (portfolio_id, ticker, name, asset_type, sector, quantity,
               average_cost, current_price, annual_dividend_per_share)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (portfolio_id, ticker) DO UPDATE SET
              name = EXCLUDED.name,
              asset_type = EXCLUDED.asset_type,
              sector = EXCLUDED.sector,
              quantity = EXCLUDED.quantity,
              average_cost = EXCLUDED.average_cost,
              current_price = EXCLUDED.current_price,
              annual_dividend_per_share = EXCLUDED.annual_dividend_per_share,
              updated_at = CURRENT_TIMESTAMP
            """, PORTFOLIO_ID, ticker, h.getName(), h.getType(), h.getSector(),
                h.getShares(), h.getAvgCostBasis(), h.getCurrentPrice(), h.getAnnualDividendPerShare());
    }

    @Transactional
    public boolean removeHolding(String ticker) {
        return jdbc.update("DELETE FROM holdings WHERE portfolio_id = ? AND ticker = ?",
                PORTFOLIO_ID, normalize(ticker)) > 0;
    }

    public Map<String, Object> getSummary() {
        List<PortfolioHolding> list = getHoldings();
        double invested = list.stream().mapToDouble(PortfolioHolding::getTotalCostBasis).sum();
        double value = list.stream().mapToDouble(PortfolioHolding::getMarketValue).sum();
        double gain = value - invested;
        double dividend = list.stream().mapToDouble(PortfolioHolding::getAnnualDividendIncome).sum();

        Map<String, Double> sectorValues = new LinkedHashMap<>();
        list.forEach(h -> sectorValues.merge(nullSafe(h.getSector()), h.getMarketValue(), Double::sum));
        Map<String, Double> sectorAllocation = percentageMap(sectorValues, value);

        Map<String, Double> typeValues = list.stream().collect(Collectors.groupingBy(
                h -> nullSafe(h.getType()), LinkedHashMap::new,
                Collectors.summingDouble(PortfolioHolding::getMarketValue)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("holdings", list);
        result.put("holdingsCount", list.size());
        result.put("totalInvested", round(invested));
        result.put("currentValue", round(value));
        result.put("gainLoss", round(gain));
        result.put("gainLossPercent", invested == 0 ? 0 : round(gain / invested * 100));
        result.put("annualDividendIncome", round(dividend));
        result.put("monthlyDividendIncome", round(dividend / 12));
        result.put("portfolioYieldPercent", value == 0 ? 0 : round(dividend / value * 100));
        result.put("sectorAllocation", sectorAllocation);
        result.put("typeAllocation", percentageMap(typeValues, value));
        return result;
    }

    private PortfolioHolding enrich(PortfolioHolding h) {
        String ticker = normalize(h.getTicker());
        h.setTicker(ticker);
        if ("ETF".equalsIgnoreCase(h.getType())) {
            etfService.getByTicker(ticker).ifPresent(e -> {
                if (h.getName() == null || h.getName().isBlank()) h.setName(e.getName());
                if (h.getSector() == null || h.getSector().isBlank()) h.setSector(e.getCategory());
                if (h.getCurrentPrice() <= 0) h.setCurrentPrice(e.getCurrentPrice());
                if (h.getAnnualDividendPerShare() <= 0) {
                    h.setAnnualDividendPerShare(e.getCurrentPrice() * e.getDistributionYieldPercent() / 100);
                }
            });
        } else {
            stockService.getByTicker(ticker).ifPresent(s -> {
                h.setType("STOCK");
                if (h.getName() == null || h.getName().isBlank()) h.setName(s.getCompanyName());
                if (h.getSector() == null || h.getSector().isBlank()) h.setSector(s.getSector());
                if (h.getCurrentPrice() <= 0) h.setCurrentPrice(s.getCurrentPrice());
                if (h.getAnnualDividendPerShare() <= 0) h.setAnnualDividendPerShare(s.getAnnualDividendPerShare());
            });
        }
        return h;
    }

    private PortfolioHolding mapHolding(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return PortfolioHolding.builder()
                .ticker(rs.getString("ticker"))
                .name(rs.getString("name"))
                .type(rs.getString("asset_type"))
                .sector(rs.getString("sector"))
                .shares(rs.getDouble("quantity"))
                .avgCostBasis(rs.getDouble("average_cost"))
                .currentPrice(rs.getDouble("current_price"))
                .annualDividendPerShare(rs.getDouble("annual_dividend_per_share"))
                .build();
    }

    private Map<String, Double> percentageMap(Map<String, Double> values, double total) {
        Map<String, Double> result = new LinkedHashMap<>();
        values.forEach((k, v) -> result.put(k, total == 0 ? 0 : round(v / total * 100)));
        return result;
    }

    private String normalize(String ticker) {
        if (ticker == null || ticker.isBlank()) throw new IllegalArgumentException("Ticker is required");
        return ticker.trim().toUpperCase(Locale.ROOT);
    }

    private String nullSafe(String value) { return value == null || value.isBlank() ? "UNKNOWN" : value; }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
