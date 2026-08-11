package com.marketcompass.investment.service;

import com.marketcompass.investment.model.Dividend;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DividendService {

    private final Map<String, Dividend> dividends = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // ── DIVIDEND KINGS (50+ consecutive years of increases) ────────────────────────
        add(Dividend.builder().ticker("KO").companyName("The Coca-Cola Company").sector("Consumer Staples")
                .currentPrice(62.50).annualDividendPerShare(1.84).dividendYieldPercent(3.12)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.46)
                .payoutRatioPercent(72.0).consecutiveYearsOfDividendGrowth(62)
                .fiveYearDividendGrowthRatePercent(4.5).isDividendAristocrat(true).isDividendKing(true)
                .creditRating("A+")
                .investmentNote("Dividend King with 62 years of consecutive increases. Warren Buffett's favorite for passive income. Brand moat provides pricing power.")
                .build());

        add(Dividend.builder().ticker("PG").companyName("Procter & Gamble Co.").sector("Consumer Staples")
                .currentPrice(168.30).annualDividendPerShare(3.76).dividendYieldPercent(2.38)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.94)
                .payoutRatioPercent(59.0).consecutiveYearsOfDividendGrowth(68)
                .fiveYearDividendGrowthRatePercent(5.8).isDividendAristocrat(true).isDividendKing(true)
                .creditRating("AA-")
                .investmentNote("Dividend King with 68 years of consecutive increases — longest streak in consumer staples. 65+ essential brands provide highly stable cash flows.")
                .build());

        add(Dividend.builder().ticker("PEP").companyName("PepsiCo Inc.").sector("Consumer Staples")
                .currentPrice(172.00).annualDividendPerShare(5.06).dividendYieldPercent(2.92)
                .paymentFrequency("QUARTERLY").dividendPerPayment(1.265)
                .payoutRatioPercent(70.0).consecutiveYearsOfDividendGrowth(52)
                .fiveYearDividendGrowthRatePercent(7.2).isDividendAristocrat(true).isDividendKing(true)
                .creditRating("A+")
                .investmentNote("Dividend King — 52 years. Snack and beverage diversification makes cash flows more resilient than a pure beverage company.")
                .build());

        add(Dividend.builder().ticker("JNJ").companyName("Johnson & Johnson").sector("Healthcare")
                .currentPrice(158.20).annualDividendPerShare(4.96).dividendYieldPercent(3.14)
                .paymentFrequency("QUARTERLY").dividendPerPayment(1.24)
                .payoutRatioPercent(55.0).consecutiveYearsOfDividendGrowth(62)
                .fiveYearDividendGrowthRatePercent(5.9).isDividendAristocrat(true).isDividendKing(true)
                .creditRating("AAA")
                .investmentNote("Dividend King — 62 years. One of only 2 companies with a AAA credit rating (same as US govt). Healthcare spending is recession-resistant.")
                .build());

        add(Dividend.builder().ticker("CL").companyName("Colgate-Palmolive Company").sector("Consumer Staples")
                .currentPrice(96.50).annualDividendPerShare(1.96).dividendYieldPercent(2.03)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.49)
                .payoutRatioPercent(60.0).consecutiveYearsOfDividendGrowth(61)
                .fiveYearDividendGrowthRatePercent(4.1).isDividendAristocrat(true).isDividendKing(true)
                .creditRating("AA-")
                .investmentNote("Dividend King — 61 years. Colgate toothpaste has #40%+ global market share. Oral care products are bought regardless of economic conditions.")
                .build());

        add(Dividend.builder().ticker("GPC").companyName("Genuine Parts Company").sector("Consumer Discretionary")
                .currentPrice(132.00).annualDividendPerShare(3.98).dividendYieldPercent(3.02)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.995)
                .payoutRatioPercent(52.0).consecutiveYearsOfDividendGrowth(68)
                .fiveYearDividendGrowthRatePercent(6.5).isDividendAristocrat(true).isDividendKing(true)
                .creditRating("A-")
                .investmentNote("Dividend King — 68 years (tied with PG for longest streak!). Distributes auto and industrial parts. Older cars = more maintenance = more demand.")
                .build());

        // ── DIVIDEND ARISTOCRATS (25-49 consecutive years) ────────────────────────────
        add(Dividend.builder().ticker("XOM").companyName("Exxon Mobil Corporation").sector("Energy")
                .currentPrice(112.50).annualDividendPerShare(3.80).dividendYieldPercent(3.24)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.95)
                .payoutRatioPercent(45.0).consecutiveYearsOfDividendGrowth(42)
                .fiveYearDividendGrowthRatePercent(3.1).isDividendAristocrat(true).isDividendKing(false)
                .creditRating("AA-")
                .investmentNote("Dividend Aristocrat — 42 years. Energy sector provides inflation hedge. Maintained dividend through oil price crashes, showing financial resilience.")
                .build());

        add(Dividend.builder().ticker("O").companyName("Realty Income Corporation").sector("Real Estate")
                .currentPrice(55.20).annualDividendPerShare(3.08).dividendYieldPercent(5.76)
                .paymentFrequency("MONTHLY").dividendPerPayment(0.2567)
                .payoutRatioPercent(75.0).consecutiveYearsOfDividendGrowth(30)
                .fiveYearDividendGrowthRatePercent(3.8).isDividendAristocrat(true).isDividendKing(false)
                .creditRating("A-")
                .investmentNote("Pays MONTHLY dividends — ideal for income investors who like regular cash flow. Dividend Aristocrat and one of the most recognizable REIT names.")
                .build());

        add(Dividend.builder().ticker("MDT").companyName("Medtronic PLC").sector("Healthcare")
                .currentPrice(84.50).annualDividendPerShare(2.80).dividendYieldPercent(3.31)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.70)
                .payoutRatioPercent(52.0).consecutiveYearsOfDividendGrowth(46)
                .fiveYearDividendGrowthRatePercent(5.2).isDividendAristocrat(true).isDividendKing(false)
                .creditRating("A")
                .investmentNote("Dividend Aristocrat — 46 years. World's largest medical device maker. Aging global population drives demand for cardiac, diabetes, surgical devices.")
                .build());

        // ── HIGH-YIELD DIVIDEND PAYERS ────────────────────────────────────────────────
        add(Dividend.builder().ticker("VZ").companyName("Verizon Communications").sector("Communication Services")
                .currentPrice(42.10).annualDividendPerShare(2.66).dividendYieldPercent(6.62)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.665)
                .payoutRatioPercent(59.0).consecutiveYearsOfDividendGrowth(17)
                .fiveYearDividendGrowthRatePercent(2.0).isDividendAristocrat(false).isDividendKing(false)
                .creditRating("BBB+")
                .investmentNote("Very high yield (~6.6%) but slow dividend growth (~2%/yr). Good for income now, but inflation will erode purchasing power over time. Watch debt levels.")
                .build());

        add(Dividend.builder().ticker("ABBV").companyName("AbbVie Inc.").sector("Healthcare")
                .currentPrice(175.00).annualDividendPerShare(6.20).dividendYieldPercent(3.54)
                .paymentFrequency("QUARTERLY").dividendPerPayment(1.55)
                .payoutRatioPercent(55.0).consecutiveYearsOfDividendGrowth(52)
                .fiveYearDividendGrowthRatePercent(7.8).isDividendAristocrat(true).isDividendKing(true)
                .creditRating("BBB+")
                .investmentNote("Dividend King — 52 years (counting Abbott parent years). Humira revenue risk being offset by Skyrizi and Rinvoq. High growth rate makes it compelling.")
                .build());

        add(Dividend.builder().ticker("MAIN").companyName("Main Street Capital Corporation").sector("Financials")
                .currentPrice(52.00).annualDividendPerShare(2.94).dividendYieldPercent(5.65)
                .paymentFrequency("MONTHLY").dividendPerPayment(0.245)
                .payoutRatioPercent(70.0).consecutiveYearsOfDividendGrowth(14)
                .fiveYearDividendGrowthRatePercent(3.5).isDividendAristocrat(false).isDividendKing(false)
                .creditRating("BBB")
                .investmentNote("Business Development Company (BDC) paying MONTHLY dividends. BDCs lend to mid-size businesses and must distribute 90%+ of income. Higher risk than REITs.")
                .build());

        add(Dividend.builder().ticker("T").companyName("AT&T Inc.").sector("Communication Services")
                .currentPrice(19.50).annualDividendPerShare(1.11).dividendYieldPercent(5.69)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.2775)
                .payoutRatioPercent(58.0).consecutiveYearsOfDividendGrowth(0)
                .fiveYearDividendGrowthRatePercent(-7.5).isDividendAristocrat(false).isDividendKing(false)
                .creditRating("BBB")
                .investmentNote("WARNING: AT&T CUT its dividend in 2022 after the WarnerMedia spinoff. High current yield but zero growth history. Lesson: high yield alone is not a buy signal.")
                .build());

        // ── GROWTH WITH DIVIDENDS ─────────────────────────────────────────────────────
        add(Dividend.builder().ticker("MSFT").companyName("Microsoft Corporation").sector("Technology")
                .currentPrice(420.10).annualDividendPerShare(3.00).dividendYieldPercent(0.72)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.75)
                .payoutRatioPercent(24.0).consecutiveYearsOfDividendGrowth(22)
                .fiveYearDividendGrowthRatePercent(10.2).isDividendAristocrat(false).isDividendKing(false)
                .creditRating("AAA")
                .investmentNote("Low yield NOW (0.72%) but growing 10%/yr. $0.75/quarter paid on a $100 cost basis today becomes much more meaningful in 20 years. Classic growth-dividend compounder.")
                .build());

        add(Dividend.builder().ticker("AAPL").companyName("Apple Inc.").sector("Technology")
                .currentPrice(192.35).annualDividendPerShare(0.96).dividendYieldPercent(0.50)
                .paymentFrequency("QUARTERLY").dividendPerPayment(0.24)
                .payoutRatioPercent(15.0).consecutiveYearsOfDividendGrowth(12)
                .fiveYearDividendGrowthRatePercent(5.8).isDividendAristocrat(false).isDividendKing(false)
                .creditRating("AA+")
                .investmentNote("Very low yield but ultra-safe dividend (15% payout ratio). Apple buys back ~$90B of stock/year on top of dividends — total shareholder return is much higher.")
                .build());

        add(Dividend.builder().ticker("JPM").companyName("JPMorgan Chase & Co.").sector("Financials")
                .currentPrice(205.60).annualDividendPerShare(4.60).dividendYieldPercent(2.18)
                .paymentFrequency("QUARTERLY").dividendPerPayment(1.15)
                .payoutRatioPercent(27.0).consecutiveYearsOfDividendGrowth(12)
                .fiveYearDividendGrowthRatePercent(14.5).isDividendAristocrat(false).isDividendKing(false)
                .creditRating("A+")
                .investmentNote("Low payout ratio (27%) despite solid yield. Dividend has grown 14.5%/year for 5 years. Significant room for continued dividend growth.")
                .build());
    }

    private void add(Dividend d) {
        dividends.put(d.getTicker(), d);
    }

    public List<Dividend> getAllDividends() {
        return new ArrayList<>(dividends.values());
    }

    public Optional<Dividend> getByTicker(String ticker) {
        return Optional.ofNullable(dividends.get(ticker.toUpperCase()));
    }

    public List<Dividend> getDividendAristocrats() {
        return dividends.values().stream()
                .filter(Dividend::isDividendAristocrat)
                .sorted(Comparator.comparingInt(Dividend::getConsecutiveYearsOfDividendGrowth).reversed())
                .collect(Collectors.toList());
    }

    public List<Dividend> getDividendKings() {
        return dividends.values().stream()
                .filter(Dividend::isDividendKing)
                .sorted(Comparator.comparingInt(Dividend::getConsecutiveYearsOfDividendGrowth).reversed())
                .collect(Collectors.toList());
    }

    public List<Dividend> getMonthlyPayers() {
        return dividends.values().stream()
                .filter(d -> "MONTHLY".equals(d.getPaymentFrequency()))
                .collect(Collectors.toList());
    }

    /** Calculate annual dividend income for a given investment across all holdings */
    public Map<String, Object> calculateIncome(Map<String, Double> holdingsSharesMap) {
        double totalAnnualIncome = 0;
        List<Map<String, Object>> breakdown = new ArrayList<>();

        for (Map.Entry<String, Double> entry : holdingsSharesMap.entrySet()) {
            String ticker = entry.getKey().toUpperCase();
            double shares = entry.getValue();
            Dividend d = dividends.get(ticker);
            if (d != null) {
                double annualIncome = shares * d.getAnnualDividendPerShare();
                totalAnnualIncome += annualIncome;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("ticker", ticker);
                item.put("shares", shares);
                item.put("annualDividendPerShare", d.getAnnualDividendPerShare());
                item.put("annualIncome", Math.round(annualIncome * 100.0) / 100.0);
                item.put("monthlyIncome", Math.round((annualIncome / 12) * 100.0) / 100.0);
                breakdown.add(item);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAnnualIncome", Math.round(totalAnnualIncome * 100.0) / 100.0);
        result.put("totalMonthlyIncome", Math.round((totalAnnualIncome / 12) * 100.0) / 100.0);
        result.put("breakdown", breakdown);
        return result;
    }
}
