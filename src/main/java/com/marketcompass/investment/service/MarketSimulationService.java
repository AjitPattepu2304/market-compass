package com.marketcompass.investment.service;

import com.marketcompass.investment.model.ETF;
import com.marketcompass.investment.model.MarketSession;
import com.marketcompass.investment.model.PriceMover;
import com.marketcompass.investment.model.Stock;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simulates intraday price movements for all tracked instruments.
 *
 * Behaviour:
 *   - When market is OPEN: applies a small Gaussian random walk (±0.25% std dev)
 *     to each ticker every 15 seconds, clamped within ±4% of the base price.
 *   - When market transitions OPEN → anything else: prices snap back to base.
 *   - Session open prices are captured at the moment the OPEN session starts,
 *     enabling % change calculations for the "Top Movers" board.
 */
@Service
public class MarketSimulationService {

    private final StockService       stockService;
    private final ETFService         etfService;
    private final MarketClockService clock;
    private final LivePriceService   livePriceService;

    /** Current simulated prices — updated every tick. */
    private final Map<String, Double> livePrices        = new ConcurrentHashMap<>();
    /** Prices at session open — baseline for % change display. */
    private final Map<String, Double> sessionOpenPrices = new ConcurrentHashMap<>();

    private String lastSession = "";
    private static final Random RNG = new Random();

    public MarketSimulationService(StockService stockService,
                                   ETFService etfService,
                                   MarketClockService clock,
                                   LivePriceService livePriceService) {
        this.stockService    = stockService;
        this.etfService      = etfService;
        this.clock           = clock;
        this.livePriceService = livePriceService;
    }

    @PostConstruct
    public void init() {
        resetToBasePrices();
        lastSession = clock.getSession().name();
    }

    // ─── Scheduled tick ───────────────────────────────────────────────────────

    @Scheduled(fixedRate = 15_000)   // every 15 seconds
    public void tick() {
        MarketSession session     = clock.getSession();
        String        sessionName = session.name();

        // Detect session transitions
        if (!sessionName.equals(lastSession)) {
            if (session == MarketSession.OPEN) {
                // Snapshot prices at the moment regular trading begins
                sessionOpenPrices.clear();
                sessionOpenPrices.putAll(livePrices);
            } else if ("OPEN".equals(lastSession)) {
                // Market just closed — prices back to static base
                resetToBasePrices();
            }
            lastSession = sessionName;
        }

        if (session == MarketSession.OPEN) {
            simulatePriceMovements();
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Live price for a ticker (base price when market is closed). */
    public double getLivePrice(String ticker) {
        return livePrices.getOrDefault(ticker.toUpperCase(), basePrice(ticker));
    }

    /** Top N movers by absolute % change from session open. */
    public List<PriceMover> getTopMovers(int n) {
        return livePrices.entrySet().stream()
                .filter(e -> {
                    Double open = sessionOpenPrices.get(e.getKey());
                    return open != null && open > 0;
                })
                .map(e -> {
                    String ticker  = e.getKey();
                    double current = e.getValue();
                    double open    = sessionOpenPrices.get(ticker);
                    double pct     = Math.round(((current - open) / open * 100) * 100.0) / 100.0;
                    return new PriceMover(ticker, resolveName(ticker), open, current, pct);
                })
                .sorted(Comparator.comparingDouble(m -> -Math.abs(m.getChangePercent())))
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Overall market direction based on advancers vs. decliners.
     * Returns "UP", "DOWN", or "MIXED".
     */
    public String getMarketDirection() {
        List<PriceMover> all  = getTopMovers(livePrices.size());
        long up   = all.stream().filter(m -> m.getChangePercent() >  0.0).count();
        long down = all.stream().filter(m -> m.getChangePercent() < -0.0).count();
        if (up   > down * 1.5) return "UP";
        if (down > up  * 1.5) return "DOWN";
        return "MIXED";
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private void resetToBasePrices() {
        stockService.getAllStocks().forEach(s ->
                livePrices.put(s.getTicker(), s.getCurrentPrice()));
        etfService.getAllETFs().forEach(e ->
                livePrices.put(e.getTicker(), e.getCurrentPrice()));
        sessionOpenPrices.clear();
        sessionOpenPrices.putAll(livePrices);
    }

    private void simulatePriceMovements() {
        livePrices.replaceAll((ticker, price) -> {
            double drift  = RNG.nextGaussian() * 0.0025;   // ±0.25% std dev per tick
            double newPx  = price * (1.0 + drift);
            double base   = basePrice(ticker);
            // Clamp to ±4% from base so prices stay realistic
            newPx = Math.max(base * 0.96, Math.min(base * 1.04, newPx));
            return Math.round(newPx * 100.0) / 100.0;
        });
    }

    private double basePrice(String ticker) {
        // Use live price when available, fall back to hardcoded
        double live = livePriceService.getPrice(ticker);
        if (live > 0) return live;
        return stockService.getByTicker(ticker)
                .map(Stock::getCurrentPrice)
                .orElseGet(() -> etfService.getByTicker(ticker)
                        .map(ETF::getCurrentPrice)
                        .orElse(0.0));
    }

    private String resolveName(String ticker) {
        return stockService.getByTicker(ticker).map(Stock::getCompanyName)
                .orElseGet(() -> etfService.getByTicker(ticker)
                        .map(ETF::getName)
                        .orElse(ticker));
    }
}
