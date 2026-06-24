package com.product_office_setup.product_office_setup.investment.service;

import com.product_office_setup.product_office_setup.investment.model.ETF;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ETFService {

    private final Map<String, ETF> etfs = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // ── BROAD MARKET ─────────────────────────────────────────────────────────────
        add(ETF.builder().ticker("VTI").name("Vanguard Total Stock Market ETF").issuer("Vanguard").category("BROAD_MARKET")
                .indexTracked("CRSP US Total Market Index").currentPrice(237.00).expenseRatioPercent(0.03)
                .aumBillions(400).holdingsCount(3700).distributionYieldPercent(1.38)
                .ytdReturnPercent(12.5).oneYearReturnPercent(24.8).fiveYearReturnPercent(14.2).tenYearReturnPercent(12.8)
                .description("Holds virtually every publicly traded US stock — large, mid, small, and micro-cap. True one-stop exposure to the entire US economy.")
                .investmentNote("Often called the 'set it and forget it' ETF for US investors. The lowest cost, broadest diversification. A core holding in most passive portfolios.")
                .build());

        add(ETF.builder().ticker("SCHB").name("Schwab US Broad Market ETF").issuer("Schwab").category("BROAD_MARKET")
                .indexTracked("Dow Jones US Broad Stock Market Index").currentPrice(58.40).expenseRatioPercent(0.03)
                .aumBillions(28).holdingsCount(2500).distributionYieldPercent(1.35)
                .ytdReturnPercent(12.3).oneYearReturnPercent(24.5).fiveYearReturnPercent(14.0).tenYearReturnPercent(12.5)
                .description("Schwab's total US market ETF. Covers 2,500 stocks across all market caps — nearly equivalent to VTI at the same ultra-low cost.")
                .investmentNote("Great VTI alternative for Schwab brokerage users. Same concept, same cost, slightly fewer holdings.")
                .build());

        // ── S&P 500 ───────────────────────────────────────────────────────────────────
        add(ETF.builder().ticker("VOO").name("Vanguard S&P 500 ETF").issuer("Vanguard").category("SP500")
                .indexTracked("S&P 500 Index").currentPrice(480.00).expenseRatioPercent(0.03)
                .aumBillions(450).holdingsCount(503).distributionYieldPercent(1.42)
                .ytdReturnPercent(12.8).oneYearReturnPercent(25.2).fiveYearReturnPercent(14.8).tenYearReturnPercent(13.0)
                .description("Tracks the S&P 500 — the 500 largest US companies. Represents ~80% of total US stock market value.")
                .investmentNote("Warren Buffett has publicly recommended S&P 500 index funds for most individual investors. At 0.03% expense ratio, hard to beat.")
                .build());

        add(ETF.builder().ticker("SPY").name("SPDR S&P 500 ETF Trust").issuer("State Street").category("SP500")
                .indexTracked("S&P 500 Index").currentPrice(525.00).expenseRatioPercent(0.0945)
                .aumBillions(520).holdingsCount(503).distributionYieldPercent(1.40)
                .ytdReturnPercent(12.8).oneYearReturnPercent(25.1).fiveYearReturnPercent(14.7).tenYearReturnPercent(12.9)
                .description("The world's oldest and largest ETF by trading volume. Launched in 1993. Same S&P 500 exposure as VOO but slightly higher expense ratio.")
                .investmentNote("Preferred by traders due to high liquidity. For long-term buy-and-hold, VOO is cheaper. Both track the same index.")
                .build());

        add(ETF.builder().ticker("IVV").name("iShares Core S&P 500 ETF").issuer("BlackRock").category("SP500")
                .indexTracked("S&P 500 Index").currentPrice(533.00).expenseRatioPercent(0.03)
                .aumBillions(480).holdingsCount(503).distributionYieldPercent(1.41)
                .ytdReturnPercent(12.8).oneYearReturnPercent(25.1).fiveYearReturnPercent(14.8).tenYearReturnPercent(12.9)
                .description("BlackRock's S&P 500 ETF — same index as VOO and SPY, at the same ultra-low 0.03% cost as VOO. Part of iShares Core series.")
                .investmentNote("Great for Fidelity/Schwab brokerage users. Essentially interchangeable with VOO for long-term investors.")
                .build());

        // ── TECHNOLOGY ────────────────────────────────────────────────────────────────
        add(ETF.builder().ticker("QQQ").name("Invesco QQQ Trust").issuer("Invesco").category("TECH")
                .indexTracked("Nasdaq-100 Index").currentPrice(450.00).expenseRatioPercent(0.20)
                .aumBillions(240).holdingsCount(101).distributionYieldPercent(0.65)
                .ytdReturnPercent(16.4).oneYearReturnPercent(32.8).fiveYearReturnPercent(19.5).tenYearReturnPercent(17.8)
                .description("Tracks the Nasdaq-100 — the 100 largest non-financial Nasdaq companies. Heavy tech concentration: ~60% tech (AAPL, MSFT, NVDA, GOOGL, META, AMZN).")
                .investmentNote("Higher growth potential but also higher volatility than broad market. Expense ratio is 6.7x higher than VOO — worth it only if you want concentrated tech.")
                .build());

        add(ETF.builder().ticker("VGT").name("Vanguard Information Technology ETF").issuer("Vanguard").category("TECH")
                .indexTracked("MSCI US IMI Info Tech 25/50 Index").currentPrice(490.00).expenseRatioPercent(0.10)
                .aumBillions(70).holdingsCount(318).distributionYieldPercent(0.62)
                .ytdReturnPercent(15.8).oneYearReturnPercent(30.5).fiveYearReturnPercent(20.1).tenYearReturnPercent(18.9)
                .description("Pure-play US technology sector ETF. Holds 318 tech stocks across semiconductors, software, hardware, and IT services.")
                .investmentNote("More diversified within tech than QQQ. Lower expense ratio at 0.10%. Better choice if you specifically want tech sector exposure.")
                .build());

        // ── DIVIDEND ──────────────────────────────────────────────────────────────────
        add(ETF.builder().ticker("SCHD").name("Schwab US Dividend Equity ETF").issuer("Schwab").category("DIVIDEND")
                .indexTracked("Dow Jones US Dividend 100 Index").currentPrice(78.00).expenseRatioPercent(0.06)
                .aumBillions(55).holdingsCount(100).distributionYieldPercent(3.50)
                .ytdReturnPercent(8.2).oneYearReturnPercent(16.5).fiveYearReturnPercent(12.8).tenYearReturnPercent(11.5)
                .description("Tracks 100 US stocks with strong dividend growth history. Screens for dividend consistency, cash flow, return on equity, and dividend yield.")
                .investmentNote("Fan favorite for dividend investors. High quality + income + low cost. Top holdings: HD, AVGO, PEP, KO, MO. Outperforms many active dividend funds.")
                .build());

        add(ETF.builder().ticker("VYM").name("Vanguard High Dividend Yield ETF").issuer("Vanguard").category("DIVIDEND")
                .indexTracked("FTSE High Dividend Yield Index").currentPrice(120.00).expenseRatioPercent(0.06)
                .aumBillions(45).holdingsCount(550).distributionYieldPercent(2.90)
                .ytdReturnPercent(9.5).oneYearReturnPercent(18.2).fiveYearReturnPercent(11.2).tenYearReturnPercent(10.8)
                .description("Holds 550 high dividend-yielding US stocks, excluding REITs. More diversified than SCHD but with slightly lower yield and quality screen.")
                .investmentNote("Good for income diversification. More holdings than SCHD provides extra stability. Lower yield than SCHD but broader exposure.")
                .build());

        add(ETF.builder().ticker("DVY").name("iShares Select Dividend ETF").issuer("BlackRock").category("DIVIDEND")
                .indexTracked("Dow Jones US Select Dividend Index").currentPrice(118.00).expenseRatioPercent(0.38)
                .aumBillions(19).holdingsCount(100).distributionYieldPercent(4.50)
                .ytdReturnPercent(6.8).oneYearReturnPercent(12.5).fiveYearReturnPercent(8.5).tenYearReturnPercent(9.2)
                .description("Selects 100 US stocks with the highest dividend yields that have maintained or grown dividends. Tilted toward utilities, financials, energy.")
                .investmentNote("Highest yield of the major dividend ETFs (~4.5%), but higher expense ratio. Heavier sector concentration — less diversified than SCHD/VYM.")
                .build());

        // ── BONDS ─────────────────────────────────────────────────────────────────────
        add(ETF.builder().ticker("BND").name("Vanguard Total Bond Market ETF").issuer("Vanguard").category("BOND")
                .indexTracked("Bloomberg US Aggregate Float Adjusted Index").currentPrice(73.50).expenseRatioPercent(0.03)
                .aumBillions(110).holdingsCount(10000).distributionYieldPercent(4.20)
                .ytdReturnPercent(1.8).oneYearReturnPercent(4.5).fiveYearReturnPercent(-0.5).tenYearReturnPercent(1.8)
                .description("Holds 10,000+ US bonds: government, corporate, mortgage-backed. Avg maturity ~8 years. The bond equivalent of VTI — broadest US bond exposure.")
                .investmentNote("Core bond holding for portfolio stability. Bonds zig when stocks zag — reduces portfolio volatility. Typically 20-40% of a balanced portfolio.")
                .build());

        add(ETF.builder().ticker("AGG").name("iShares Core US Aggregate Bond ETF").issuer("BlackRock").category("BOND")
                .indexTracked("Bloomberg US Aggregate Bond Index").currentPrice(96.50).expenseRatioPercent(0.03)
                .aumBillions(105).holdingsCount(10000).distributionYieldPercent(4.15)
                .ytdReturnPercent(1.8).oneYearReturnPercent(4.4).fiveYearReturnPercent(-0.5).tenYearReturnPercent(1.7)
                .description("BlackRock's total bond ETF — essentially the same as BND, tracking the Bloomberg US Aggregate Bond Index.")
                .investmentNote("Interchangeable with BND. Choose based on your brokerage. Both are excellent low-cost core bond holdings.")
                .build());

        add(ETF.builder().ticker("TLT").name("iShares 20+ Year Treasury Bond ETF").issuer("BlackRock").category("BOND")
                .indexTracked("ICE US Treasury 20+ Year Index").currentPrice(94.00).expenseRatioPercent(0.15)
                .aumBillions(50).holdingsCount(40).distributionYieldPercent(4.50)
                .ytdReturnPercent(-2.5).oneYearReturnPercent(-5.2).fiveYearReturnPercent(-8.0).tenYearReturnPercent(0.2)
                .description("Long-term US Treasury bonds only (20+ year maturity). Very sensitive to interest rate changes — prices fall sharply when rates rise.")
                .investmentNote("High-risk bond ETF. Best as a hedge or speculation on falling rates. NOT suitable as a stable bond holding for most investors.")
                .build());

        // ── INTERNATIONAL ─────────────────────────────────────────────────────────────
        add(ETF.builder().ticker("VXUS").name("Vanguard Total International Stock ETF").issuer("Vanguard").category("INTERNATIONAL")
                .indexTracked("FTSE Global All Cap ex US Index").currentPrice(60.50).expenseRatioPercent(0.07)
                .aumBillions(70).holdingsCount(8500).distributionYieldPercent(3.02)
                .ytdReturnPercent(10.2).oneYearReturnPercent(15.8).fiveYearReturnPercent(5.5).tenYearReturnPercent(4.8)
                .description("Holds 8,500+ stocks in 47 countries excluding the US. Covers developed markets (Europe, Japan, UK, Canada) and emerging markets (China, India, Brazil).")
                .investmentNote("VTI + VXUS = complete global coverage. Classic 3-fund portfolio approach. Adds geographic diversification when US stocks are expensive relative to global.")
                .build());

        add(ETF.builder().ticker("EFA").name("iShares MSCI EAFE ETF").issuer("BlackRock").category("INTERNATIONAL")
                .indexTracked("MSCI EAFE Index").currentPrice(80.00).expenseRatioPercent(0.32)
                .aumBillions(55).holdingsCount(770).distributionYieldPercent(3.20)
                .ytdReturnPercent(11.0).oneYearReturnPercent(17.5).fiveYearReturnPercent(6.2).tenYearReturnPercent(5.5)
                .description("Developed markets excluding US & Canada: Europe, Australasia, Far East. Japan, UK, France, Germany, Switzerland are top country exposures.")
                .investmentNote("Higher expense ratio than VXUS. VXUS is generally preferred for lower cost and broader coverage including emerging markets.")
                .build());

        // ── VALUE / GROWTH ────────────────────────────────────────────────────────────
        add(ETF.builder().ticker("VTV").name("Vanguard Value ETF").issuer("Vanguard").category("VALUE")
                .indexTracked("CRSP US Large Cap Value Index").currentPrice(145.00).expenseRatioPercent(0.04)
                .aumBillions(120).holdingsCount(340).distributionYieldPercent(2.25)
                .ytdReturnPercent(9.8).oneYearReturnPercent(19.5).fiveYearReturnPercent(11.0).tenYearReturnPercent(10.5)
                .description("US large-cap value stocks — companies trading at lower valuations relative to earnings, book value, and dividends. Top sectors: Financials, Healthcare, Consumer Staples.")
                .investmentNote("Value historically outperforms growth over very long periods. Lower P/E holdings mean less downside risk in market corrections.")
                .build());

        add(ETF.builder().ticker("VUG").name("Vanguard Growth ETF").issuer("Vanguard").category("GROWTH")
                .indexTracked("CRSP US Large Cap Growth Index").currentPrice(330.00).expenseRatioPercent(0.04)
                .aumBillions(130).holdingsCount(185).distributionYieldPercent(0.48)
                .ytdReturnPercent(17.5).oneYearReturnPercent(32.0).fiveYearReturnPercent(18.8).tenYearReturnPercent(16.2)
                .description("US large-cap growth stocks — companies with high earnings growth expectations. Concentrated in tech: AAPL, MSFT, NVDA, AMZN, META, GOOGL make up ~50%.")
                .investmentNote("Higher growth, higher volatility. Performed exceptionally in 2010s bull market. Be prepared for sharper drawdowns during corrections.")
                .build());

        // ── REAL ESTATE ───────────────────────────────────────────────────────────────
        add(ETF.builder().ticker("VNQ").name("Vanguard Real Estate ETF").issuer("Vanguard").category("REIT")
                .indexTracked("MSCI US Investable Market Real Estate 25/50 Index").currentPrice(85.00).expenseRatioPercent(0.12)
                .aumBillions(35).holdingsCount(160).distributionYieldPercent(4.10)
                .ytdReturnPercent(5.5).oneYearReturnPercent(10.8).fiveYearReturnPercent(4.2).tenYearReturnPercent(7.5)
                .description("Diversified US REIT exposure: residential (apartments), commercial (offices, malls), industrial (warehouses), healthcare (hospitals), data centers.")
                .investmentNote("Adds real estate to portfolio without owning property. REITs pay high dividends by law. Sensitive to interest rates — prices fall when rates rise.")
                .build());
    }

    private void add(ETF e) {
        etfs.put(e.getTicker(), e);
    }

    public List<ETF> getAllETFs() {
        return new ArrayList<>(etfs.values());
    }

    public Optional<ETF> getByTicker(String ticker) {
        return Optional.ofNullable(etfs.get(ticker.toUpperCase()));
    }

    public List<ETF> getByCategory(String category) {
        return etfs.values().stream()
                .filter(e -> e.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public List<ETF> compare(List<String> tickers) {
        return tickers.stream()
                .map(t -> etfs.get(t.toUpperCase()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<String> getAllCategories() {
        return etfs.values().stream()
                .map(ETF::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
