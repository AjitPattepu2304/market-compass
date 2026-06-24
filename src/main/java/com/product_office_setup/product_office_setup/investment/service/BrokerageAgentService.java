package com.product_office_setup.product_office_setup.investment.service;

import com.product_office_setup.product_office_setup.investment.model.Dividend;
import com.product_office_setup.product_office_setup.investment.model.ETF;
import com.product_office_setup.product_office_setup.investment.model.Opportunity;
import com.product_office_setup.product_office_setup.investment.model.Stock;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MarketCompass Brokerage Agent — scans all instruments and surfaces
 * the most compelling investment opportunities across four strategies.
 *
 * Think of this as your personal analyst running four separate screens:
 *
 *  1. VALUE SCREEN     — Who is cheap relative to earnings + near 52W lows?
 *  2. INCOME SCREEN    — Who pays well AND has a safe, growing dividend?
 *  3. CHIP SCREEN      — Which semiconductors have the best risk/reward now?
 *  4. ETF SCREEN       — Which ETF wins in each category (best return for cost)?
 */
@Service
public class BrokerageAgentService {

    private final StockService   stockService;
    private final ETFService     etfService;
    private final DividendService dividendService;

    public BrokerageAgentService(StockService stockService,
                                  ETFService etfService,
                                  DividendService dividendService) {
        this.stockService    = stockService;
        this.etfService      = etfService;
        this.dividendService = dividendService;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ══════════════════════════════════════════════════════════════════════════════

    public List<Opportunity> getAllOpportunities() {
        List<Opportunity> all = new ArrayList<>();
        all.addAll(scanValueOpportunities());
        all.addAll(scanIncomeOpportunities());
        all.addAll(scanChipOpportunities());
        all.addAll(scanETFOpportunities());
        all.sort(Comparator.comparingInt(Opportunity::getScore).reversed());
        return all;
    }

    public List<Opportunity> getByType(String type) {
        return getAllOpportunities().stream()
                .filter(o -> o.getOpportunityType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getMarketInsights() {
        List<Stock> chips     = stockService.getChipStocks();
        List<Stock> allStocks = stockService.getAllStocks();
        List<ETF>   allETFs   = etfService.getAllETFs();

        // Average semiconductor P/E (exclude 0 = losses)
        double avgChipPE = chips.stream()
                .filter(s -> s.getPeRatio() > 0)
                .mapToDouble(Stock::getPeRatio)
                .average().orElse(30);

        // Best / worst performers this year
        Stock topPerformer = allStocks.stream()
                .max(Comparator.comparingDouble(Stock::getOneYearReturnPercent)).orElse(null);
        Stock worstPerformer = allStocks.stream()
                .min(Comparator.comparingDouble(Stock::getOneYearReturnPercent)).orElse(null);

        // Cheapest and most expensive ETF overall
        ETF cheapestETF = allETFs.stream()
                .min(Comparator.comparingDouble(ETF::getExpenseRatioPercent)).orElse(null);

        // Count of chips in each tier
        long largeCap = chips.stream().filter(s -> s.getMarketCapBillions() >= 100).count();
        long midCap   = chips.stream().filter(s -> s.getMarketCapBillions() >= 10 && s.getMarketCapBillions() < 100).count();
        long smallCap = chips.stream().filter(s -> s.getMarketCapBillions() < 10).count();

        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("totalStocksTracked", allStocks.size());
        insights.put("totalETFsTracked", allETFs.size());
        insights.put("totalChipStocksTracked", chips.size());
        insights.put("chipCapDistribution", Map.of(
                "largeCap (>$100B)", largeCap,
                "midCap ($10-100B)", midCap,
                "smallCap (<$10B)",  smallCap
        ));
        insights.put("avgSemiconductorPE", Math.round(avgChipPE * 10.0) / 10.0);
        insights.put("topPerformer1Y", topPerformer  != null ? topPerformer.getTicker()  + " (" + topPerformer.getOneYearReturnPercent()  + "%)" : "N/A");
        insights.put("worstPerformer1Y", worstPerformer != null ? worstPerformer.getTicker() + " (" + worstPerformer.getOneYearReturnPercent() + "%)" : "N/A");
        insights.put("cheapestETF", cheapestETF != null ? cheapestETF.getTicker() + " (" + cheapestETF.getExpenseRatioPercent() + "%)" : "N/A");
        insights.put("agentNarrative", buildNarrative(chips, topPerformer, worstPerformer));
        return insights;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // SCREEN 1 — VALUE
    // ══════════════════════════════════════════════════════════════════════════════

    private List<Opportunity> scanValueOpportunities() {
        return stockService.getAllStocks().stream()
                .map(this::scoreValue)
                .filter(o -> o.getScore() >= 50)
                .sorted(Comparator.comparingInt(Opportunity::getScore).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    private Opportunity scoreValue(Stock s) {
        int score = 0;
        List<String> points = new ArrayList<>();

        // P/E scoring — lower is better (but 0 means unprofitable)
        if (s.getPeRatio() > 0 && s.getPeRatio() < 12) {
            score += 35; points.add("Very low P/E of " + s.getPeRatio() + "× — deeply undervalued vs. market average of ~22×");
        } else if (s.getPeRatio() < 18) {
            score += 22; points.add("Below-average P/E of " + s.getPeRatio() + "× — trading cheaper than most S&P 500 stocks");
        } else if (s.getPeRatio() < 25) {
            score += 10; points.add("Reasonable P/E of " + s.getPeRatio() + "× — fairly valued");
        }

        // Distance from 52-week low — closer to low = more upside
        double pctFromLow = (s.getCurrentPrice() - s.getFiftyTwoWeekLow()) / s.getFiftyTwoWeekLow() * 100;
        if (pctFromLow < 15) {
            score += 30; points.add(String.format("Only %.1f%% above 52-week low — near bottom, limited downside", pctFromLow));
        } else if (pctFromLow < 30) {
            score += 15; points.add(String.format("%.1f%% above 52-week low — still near recent trough", pctFromLow));
        }

        // Low beta = stability
        if (s.getBeta() < 0.7) {
            score += 15; points.add("Beta " + s.getBeta() + " — very low volatility, defensive characteristics");
        } else if (s.getBeta() < 1.0) {
            score += 8; points.add("Beta " + s.getBeta() + " — less volatile than the overall market");
        }

        // Defensive sectors get a bonus
        if (Set.of("Consumer Staples", "Healthcare").contains(s.getSector())) {
            score += 12; points.add(s.getSector() + " sector is recession-resistant — earnings hold up in downturns");
        }

        // Pays dividend = real cash return even while waiting for price recovery
        if (s.getDividendYieldPercent() >= 2.5) {
            score += 8; points.add(String.format("%.2f%% dividend yield — get paid to wait for value to be realised", s.getDividendYieldPercent()));
        }

        String signal = score >= 80 ? "STRONG_BUY" : score >= 65 ? "BUY" : score >= 50 ? "WATCH" : "HOLD";
        String reasoning = String.format(
                "%s is trading at $%.2f — %s. P/E of %.1f× is %s the S&P 500 average. " +
                "At %.1f%% above its 52-week low of $%.2f, the stock offers an attractive entry point " +
                "for a value-focused investor. %s",
                s.getCompanyName(), s.getCurrentPrice(),
                s.getPeRatio() > 0 && s.getPeRatio() < 20 ? "a classic value setup" : "showing value characteristics",
                s.getPeRatio(),
                s.getPeRatio() > 0 && s.getPeRatio() < 22 ? "below" : "near",
                pctFromLow, s.getFiftyTwoWeekLow(),
                s.getInvestmentNote()
        );

        return Opportunity.builder()
                .ticker(s.getTicker()).companyName(s.getCompanyName()).sector(s.getSector())
                .opportunityType("VALUE").signal(signal).score(Math.min(100, score))
                .reasoning(reasoning).keyPoints(points).currentPrice(s.getCurrentPrice())
                .chipType(s.getChipType()).build();
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // SCREEN 2 — INCOME
    // ══════════════════════════════════════════════════════════════════════════════

    private List<Opportunity> scanIncomeOpportunities() {
        return dividendService.getAllDividends().stream()
                .map(this::scoreIncome)
                .filter(o -> o.getScore() >= 50)
                .sorted(Comparator.comparingInt(Opportunity::getScore).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    private Opportunity scoreIncome(Dividend d) {
        int score = 0;
        List<String> points = new ArrayList<>();

        // Yield scoring
        if (d.getDividendYieldPercent() >= 5.0) {
            score += 25; points.add(String.format("Exceptional yield of %.2f%% — well above market and savings rates", d.getDividendYieldPercent()));
        } else if (d.getDividendYieldPercent() >= 3.0) {
            score += 18; points.add(String.format("Strong yield of %.2f%% — significantly above S&P 500 average ~1.4%%", d.getDividendYieldPercent()));
        } else if (d.getDividendYieldPercent() >= 1.5) {
            score += 8; points.add(String.format("Modest yield of %.2f%% — below average but may grow substantially", d.getDividendYieldPercent()));
        }

        // Payout ratio — sustainable sweet spot is 30-60%
        if (d.getPayoutRatioPercent() > 0 && d.getPayoutRatioPercent() <= 60) {
            score += 22; points.add(String.format("Healthy payout ratio of %.0f%% — well-covered by earnings, room to grow", d.getPayoutRatioPercent()));
        } else if (d.getPayoutRatioPercent() <= 75) {
            score += 10; points.add(String.format("Moderate payout ratio of %.0f%% — dividend is covered but leaves less room", d.getPayoutRatioPercent()));
        } else if (d.getPayoutRatioPercent() > 85) {
            score -= 10; points.add(String.format("⚠️ High payout ratio of %.0f%% — dividend could be at risk if earnings dip", d.getPayoutRatioPercent()));
        }

        // Consecutive years of growth
        if (d.getConsecutiveYearsOfDividendGrowth() >= 50) {
            score += 25; points.add("👑 Dividend King — " + d.getConsecutiveYearsOfDividendGrowth() + " consecutive years of increases (survived recessions, wars, inflation)");
        } else if (d.getConsecutiveYearsOfDividendGrowth() >= 25) {
            score += 18; points.add("🏅 Dividend Aristocrat — " + d.getConsecutiveYearsOfDividendGrowth() + " consecutive years of increases");
        } else if (d.getConsecutiveYearsOfDividendGrowth() >= 10) {
            score += 10; points.add(d.getConsecutiveYearsOfDividendGrowth() + " consecutive years of increases — proven commitment to shareholders");
        }

        // Dividend growth rate — growing dividend beats inflation
        if (d.getFiveYearDividendGrowthRatePercent() >= 10) {
            score += 15; points.add(String.format("%.1f%%/yr 5-year dividend growth — doubling every ~%.0f years (Rule of 72)", d.getFiveYearDividendGrowthRatePercent(), 72 / d.getFiveYearDividendGrowthRatePercent()));
        } else if (d.getFiveYearDividendGrowthRatePercent() >= 5) {
            score += 8; points.add(String.format("%.1f%%/yr 5-year dividend growth — keeping ahead of inflation", d.getFiveYearDividendGrowthRatePercent()));
        } else if (d.getFiveYearDividendGrowthRatePercent() < 0) {
            score -= 15; points.add(String.format("⚠️ Dividend growth rate of %.1f%%/yr — dividend has been cut or reduced", d.getFiveYearDividendGrowthRatePercent()));
        }

        // Monthly payers get a bonus
        if ("MONTHLY".equals(d.getPaymentFrequency())) {
            score += 5; points.add("💰 Pays monthly — 12 income events per year, great for cash flow management");
        }

        String signal = score >= 80 ? "STRONG_BUY" : score >= 65 ? "BUY" : score >= 50 ? "WATCH" : "HOLD";
        String reasoning = String.format(
                "%s yields %.2f%% annually ($%.2f/share). With a payout ratio of %.0f%% and %d consecutive " +
                "years of dividend increases, this is %s. " +
                "At the current 5-year growth rate of %.1f%%/yr, the dividend doubles roughly every %.0f years — " +
                "meaning your yield-on-cost grows significantly if you hold long-term. %s",
                d.getCompanyName(), d.getDividendYieldPercent(), d.getAnnualDividendPerShare(),
                d.getPayoutRatioPercent(), d.getConsecutiveYearsOfDividendGrowth(),
                d.isDividendKing() ? "a Dividend King with exceptional income stability" :
                        d.isDividendAristocrat() ? "a Dividend Aristocrat with proven income reliability" :
                                "a solid income payer",
                d.getFiveYearDividendGrowthRatePercent(),
                d.getFiveYearDividendGrowthRatePercent() > 0 ? 72 / d.getFiveYearDividendGrowthRatePercent() : 999,
                d.getInvestmentNote()
        );

        return Opportunity.builder()
                .ticker(d.getTicker()).companyName(d.getCompanyName()).sector(d.getSector())
                .opportunityType("INCOME").signal(signal).score(Math.min(100, score))
                .reasoning(reasoning).keyPoints(points).currentPrice(d.getCurrentPrice())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // SCREEN 3 — CHIP SECTOR
    // ══════════════════════════════════════════════════════════════════════════════

    private List<Opportunity> scanChipOpportunities() {
        return stockService.getChipStocks().stream()
                .map(this::scoreChip)
                .filter(o -> o.getScore() >= 50)
                .sorted(Comparator.comparingInt(Opportunity::getScore).reversed())
                .limit(8)
                .collect(Collectors.toList());
    }

    private Opportunity scoreChip(Stock s) {
        int score = 0;
        List<String> points = new ArrayList<>();

        // Moat / structural advantage
        if ("EQUIPMENT".equals(s.getChipType())) {
            score += 20; points.add("Equipment maker — benefits from ALL chip spending regardless of which design wins");
            if ("ASML".equals(s.getTicker())) {
                score += 25; points.add("🏰 Absolute monopoly on EUV lithography — only company in the world that can make these machines");
            }
        }
        if ("FOUNDRY".equals(s.getChipType())) {
            score += 18; points.add("Pure-play foundry — critical infrastructure, massive switching costs for customers");
        }
        if ("ANALOG".equals(s.getChipType())) {
            score += 12; points.add("Analog chips — less cyclical than logic/memory, 10-20 year product life cycles");
        }

        // AI / data-centre exposure
        Set<String> aiExposed = Set.of("NVDA", "AMD", "AVGO", "MRVL", "MPWR", "MU", "TSM");
        if (aiExposed.contains(s.getTicker())) {
            score += 20; points.add("🤖 Direct AI/data-centre exposure — the fastest-growing chip end-market");
        }

        // EV / automotive chip exposure
        Set<String> evExposed = Set.of("ON", "TXN", "ADI", "STM", "MCHP");
        if (evExposed.contains(s.getTicker())) {
            score += 12; points.add("🚗 Automotive/EV chip exposure — EVs use 2-5× more semiconductors than ICE vehicles");
        }

        // Pullback from high creates opportunity
        double pctFromHigh = s.getPercentFromHigh();
        if (pctFromHigh >= 35) {
            score += 22; points.add(String.format("%.0f%% below 52-week high — significant correction creates re-entry opportunity", pctFromHigh));
        } else if (pctFromHigh >= 20) {
            score += 12; points.add(String.format("%.0f%% below 52-week high — notable pullback from peak", pctFromHigh));
        }

        // Valuation — chips can have high PE so use broader threshold
        if (s.getPeRatio() > 0 && s.getPeRatio() < 20) {
            score += 15; points.add("P/E " + s.getPeRatio() + "× — cheap for a semiconductor company, below sector average");
        } else if (s.getPeRatio() > 0 && s.getPeRatio() < 35) {
            score += 5; points.add("P/E " + s.getPeRatio() + "× — reasonable for high-growth chip company");
        }

        // Market cap: smaller = more upside potential (higher risk)
        if (s.getMarketCapBillions() < 10) {
            score += 8; points.add(String.format("Small-cap ($%.1fB) — higher risk, higher potential upside vs. large-cap peers", s.getMarketCapBillions()));
        } else if (s.getMarketCapBillions() < 50) {
            score += 4; points.add(String.format("Mid-cap ($%.0fB) — more upside than mega-caps, less risk than small-caps", s.getMarketCapBillions()));
        }

        String chipLabel = switch (s.getChipType()) {
            case "FABLESS"   -> "fabless chip designer";
            case "IDM"       -> "integrated device manufacturer";
            case "FOUNDRY"   -> "pure-play foundry";
            case "EQUIPMENT" -> "chip equipment maker";
            case "MEMORY"    -> "memory chip specialist";
            case "ANALOG"    -> "analog/mixed-signal chip maker";
            default          -> "semiconductor company";
        };

        String signal = score >= 80 ? "STRONG_BUY" : score >= 65 ? "BUY" : score >= 50 ? "WATCH" : "HOLD";
        String reasoning = String.format(
                "%s is a %s with market cap of $%.0fB. Currently trading at $%.2f — %.0f%% below its 52-week high. " +
                "%s The semiconductor industry is in a multi-year AI supercycle, with data-centre chip demand " +
                "growing at 30-50%%/yr. The agent flags this as a %s opportunity based on structural position, " +
                "valuation, and sector tailwinds.",
                s.getCompanyName(), chipLabel, s.getMarketCapBillions(),
                s.getCurrentPrice(), pctFromHigh,
                s.getInvestmentNote(),
                signal.replace("_", " ").toLowerCase()
        );

        return Opportunity.builder()
                .ticker(s.getTicker()).companyName(s.getCompanyName()).sector(s.getSector())
                .opportunityType("CHIP_SECTOR").signal(signal).score(Math.min(100, score))
                .reasoning(reasoning).keyPoints(points).currentPrice(s.getCurrentPrice())
                .chipType(s.getChipType()).build();
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // SCREEN 4 — ETF EFFICIENCY
    // ══════════════════════════════════════════════════════════════════════════════

    private List<Opportunity> scanETFOpportunities() {
        Map<String, List<ETF>> byCategory = etfService.getAllETFs().stream()
                .collect(Collectors.groupingBy(ETF::getCategory));

        List<Opportunity> results = new ArrayList<>();
        for (Map.Entry<String, List<ETF>> entry : byCategory.entrySet()) {
            List<ETF> group = entry.getValue();
            if (group.size() < 2) continue; // only flag when there's a comparison to make

            // Best 5-year return
            ETF bestReturn = group.stream().max(Comparator.comparingDouble(ETF::getFiveYearReturnPercent)).get();
            // Lowest expense ratio
            ETF cheapest = group.stream().min(Comparator.comparingDouble(ETF::getExpenseRatioPercent)).get();

            // Score the cheapest one in the category
            int score = 60; // base — it's at least the cheapest
            List<String> points = new ArrayList<>();
            points.add(String.format("Lowest expense ratio in %s category: %.4f%%", entry.getKey(), cheapest.getExpenseRatioPercent()));
            points.add(String.format("Annual cost on $10,000: $%.2f — vs. category average of $%.2f",
                    cheapest.getAnnualCostFor10k(),
                    group.stream().mapToDouble(ETF::getAnnualCostFor10k).average().orElse(0)));

            if (cheapest.getTicker().equals(bestReturn.getTicker())) {
                score = 85;
                points.add("🏆 Best of both worlds — lowest cost AND best 5-year return in category");
            } else {
                double returnGap = bestReturn.getFiveYearReturnPercent() - cheapest.getFiveYearReturnPercent();
                if (returnGap < 1.0) {
                    score = 78;
                    points.add(String.format("Return gap vs. best performer is only %.1f%%/yr — cheap wins", returnGap));
                } else {
                    score = 62;
                    points.add(String.format("Best-return ETF is %s (+%.1f%%/yr over 5Y) — consider if the extra cost is worth it", bestReturn.getTicker(), returnGap));
                }
            }
            points.add(String.format("%d holdings — %s", cheapest.getHoldingsCount(),
                    cheapest.getHoldingsCount() > 1000 ? "broadly diversified" : "concentrated exposure"));

            String reasoning = String.format(
                    "In the %s ETF category, %s (%s) offers the lowest expense ratio at %.4f%% — " +
                    "that's $%.2f per year on a $10,000 investment. Over 30 years, the difference in fees " +
                    "between a 0.03%% fund and a 0.50%% fund compounds to thousands of dollars. " +
                    "5-year annualised return: %.1f%%. %s",
                    entry.getKey(), cheapest.getTicker(), cheapest.getName(),
                    cheapest.getExpenseRatioPercent(), cheapest.getAnnualCostFor10k(),
                    cheapest.getFiveYearReturnPercent(),
                    cheapest.getInvestmentNote()
            );

            results.add(Opportunity.builder()
                    .ticker(cheapest.getTicker()).companyName(cheapest.getName()).sector(entry.getKey())
                    .opportunityType("ETF").signal(score >= 80 ? "STRONG_BUY" : score >= 65 ? "BUY" : "WATCH")
                    .score(score).reasoning(reasoning).keyPoints(points)
                    .currentPrice(cheapest.getCurrentPrice()).build());
        }
        return results.stream().sorted(Comparator.comparingInt(Opportunity::getScore).reversed()).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // MARKET NARRATIVE
    // ══════════════════════════════════════════════════════════════════════════════

    private String buildNarrative(List<Stock> chips, Stock top, Stock worst) {
        long fabless  = chips.stream().filter(s -> "FABLESS".equals(s.getChipType())).count();
        long equip    = chips.stream().filter(s -> "EQUIPMENT".equals(s.getChipType())).count();
        long memory   = chips.stream().filter(s -> "MEMORY".equals(s.getChipType())).count();

        return String.format(
            "📡 MarketCompass is tracking %d semiconductor stocks across the full chip value chain: " +
            "%d fabless designers (like NVDA and AMD), %d equipment makers (ASML, AMAT, LRCX, KLAC, FORM), " +
            "and %d memory specialists (MU). " +
            "The AI supercycle is reshaping chip demand — data-centre GPU revenue grew ~200%% YoY for leading players. " +
            "Equipment makers represent a picks-and-shovels play: they profit from every dollar of fab investment globally. " +
            "ASML's EUV monopoly remains the most powerful moat in technology. " +
            "Analysts flag INTC as a high-risk turnaround and ON Semiconductor for EV tailwinds. " +
            "Best 1-year performer in the portfolio: %s. Watch list: %s.",
            chips.size(), fabless, equip, memory,
            top  != null ? top.getTicker()   + " (+" + top.getOneYearReturnPercent()   + "%%)" : "N/A",
            worst != null ? worst.getTicker() + " ("  + worst.getOneYearReturnPercent() + "%%)" : "N/A"
        );
    }
}
