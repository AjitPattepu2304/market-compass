package com.marketcompass.investment.service;

import com.marketcompass.investment.model.ETF;
import com.marketcompass.investment.model.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches real-time stock and ETF prices from Finnhub (free tier).
 *
 * Setup:
 *  1. Sign up free at https://finnhub.io
 *  2. Copy your API key
 *  3. Set FINNHUB_API_KEY environment variable (Render → Environment settings)
 *
 * Behaviour:
 *  - If FINNHUB_API_KEY is not set: returns hardcoded base prices (static mode)
 *  - If set: refreshes all ~50 tickers every 5 minutes (well within 60 req/min limit)
 *  - Always falls back to hardcoded price on any fetch error
 *
 * Finnhub quote endpoint:
 *  GET https://finnhub.io/api/v1/quote?symbol=AAPL&token={key}
 *  Response field "c" = current price
 */
@Service
public class LivePriceService {

    private static final Logger log = LoggerFactory.getLogger(LivePriceService.class);

    private static final String FINNHUB_URL =
            "https://finnhub.io/api/v1/quote?symbol={symbol}&token={token}";

    @Value("${finnhub.api.key:}")
    private String apiKey;

    private final StockService  stockService;
    private final ETFService    etfService;
    private final RestTemplate  rest = new RestTemplate();

    /** ticker → live price cache. Updated every 5 minutes. */
    private final Map<String, Double> cache = new ConcurrentHashMap<>();

    private volatile boolean liveEnabled = false;

    public LivePriceService(StockService stockService, ETFService etfService) {
        this.stockService = stockService;
        this.etfService   = etfService;
    }

    // ─── Scheduled refresh ────────────────────────────────────────────────────

    /**
     * Starts 1 second after app boot, then refreshes every 5 minutes.
     * 100 ms gap between calls → ~5s to fetch 50 tickers → 60 req/min safe.
     */
    @Scheduled(initialDelay = 1_000, fixedRate = 300_000)
    public void refreshAll() {
        if (apiKey == null || apiKey.isBlank()) {
            if (!liveEnabled) log.info("LivePriceService: FINNHUB_API_KEY not set — using static prices");
            return;
        }

        if (!liveEnabled) {
            log.info("LivePriceService: Finnhub API key present — fetching live prices for {} tickers",
                     getAllTickers().size());
            liveEnabled = true;
        }

        int fetched = 0;
        for (String ticker : getAllTickers()) {
            try {
                double price = fetchFromFinnhub(ticker);
                if (price > 0) {
                    cache.put(ticker, price);
                    fetched++;
                }
                Thread.sleep(100); // 100 ms gap → max 600 req/min headroom
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.debug("LivePriceService: fetch failed for {} — {}", ticker, e.getMessage());
            }
        }
        log.debug("LivePriceService: refreshed {} / {} tickers", fetched, getAllTickers().size());
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns live price for a ticker.
     * Falls back to hardcoded price if Finnhub is not configured or fetch failed.
     */
    public double getPrice(String ticker) {
        String t = ticker.toUpperCase();
        Double cached = cache.get(t);
        if (cached != null && cached > 0) return cached;
        return hardcodedPrice(t);
    }

    /** True once Finnhub key is set and at least one price has been cached. */
    public boolean isLive() {
        return liveEnabled && !cache.isEmpty();
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private double fetchFromFinnhub(String ticker) {
        // Finnhub uses BRK-B for Berkshire class B; all others match directly
        String symbol = ticker.replace(".", "-");
        Map<String, Object> body = rest.getForObject(
                FINNHUB_URL, Map.class, symbol, apiKey);

        if (body == null || body.get("c") == null) return 0;
        Number price = (Number) body.get("c");
        return price.doubleValue();
    }

    private double hardcodedPrice(String ticker) {
        return stockService.getByTicker(ticker)
                .map(Stock::getCurrentPrice)
                .orElseGet(() -> etfService.getByTicker(ticker)
                        .map(ETF::getCurrentPrice)
                        .orElse(0.0));
    }

    private List<String> getAllTickers() {
        List<String> tickers = new ArrayList<>();
        stockService.getAllStocks().forEach(s -> tickers.add(s.getTicker()));
        etfService.getAllETFs().forEach(e -> tickers.add(e.getTicker()));
        return tickers;
    }
}
