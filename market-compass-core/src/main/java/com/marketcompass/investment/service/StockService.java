package com.marketcompass.investment.service;

import com.marketcompass.investment.model.Stock;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final Map<String, Stock> stocks = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        addChipStocks();
        add(Stock.builder().ticker("AAPL").companyName("Apple Inc.").sector("Technology").industry("Consumer Electronics")
                .currentPrice(192.35).marketCapBillions(2950).peRatio(29.5).dividendYieldPercent(0.5)
                .annualDividendPerShare(0.96).fiftyTwoWeekHigh(198.23).fiftyTwoWeekLow(164.08)
                .oneYearReturnPercent(14.2).beta(1.24)
                .description("World's largest company by market cap. Makes iPhone, Mac, iPad, Apple Watch, and Services (App Store, iCloud, Apple TV+). Massive ecosystem of 2B+ active devices.")
                .investmentNote("Blue-chip tech with strong ecosystem lock-in and huge cash flows. Consistent dividend grower and aggressive buybacks make it a core holding.")
                .build());

        add(Stock.builder().ticker("MSFT").companyName("Microsoft Corporation").sector("Technology").industry("Software")
                .currentPrice(420.10).marketCapBillions(3120).peRatio(34.2).dividendYieldPercent(0.72)
                .annualDividendPerShare(3.00).fiftyTwoWeekHigh(430.82).fiftyTwoWeekLow(309.45)
                .oneYearReturnPercent(28.5).beta(0.90)
                .description("Dominant in enterprise software (Office 365, Azure cloud, Teams), gaming (Xbox, Activision), and LinkedIn. Major AI investor via OpenAI partnership.")
                .investmentNote("Azure cloud growth is a long-term tailwind. Consistent dividend grower — often called the 'safe' tech blue chip due to lower beta.")
                .build());

        add(Stock.builder().ticker("NVDA").companyName("NVIDIA Corporation").sector("Technology").industry("Semiconductors")
                .currentPrice(875.35).marketCapBillions(2150).peRatio(65.0).dividendYieldPercent(0.04)
                .annualDividendPerShare(0.16).fiftyTwoWeekHigh(974.00).fiftyTwoWeekLow(408.50)
                .oneYearReturnPercent(196.5).beta(1.66)
                .description("Leading designer of GPUs for gaming, AI/ML training, data centers, and autonomous vehicles. H100/H200 chips dominate the AI infrastructure market.")
                .investmentNote("High-growth, high-volatility. Dominant AI infrastructure play. High P/E means the market expects continued explosive growth — higher risk.")
                .build());

        add(Stock.builder().ticker("GOOGL").companyName("Alphabet Inc.").sector("Communication Services").industry("Internet Services")
                .currentPrice(172.40).marketCapBillions(2150).peRatio(24.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(181.15).fiftyTwoWeekLow(115.80)
                .oneYearReturnPercent(42.2).beta(1.06)
                .description("Parent of Google Search, YouTube, Google Cloud, Android, Maps, and Waymo (self-driving). Dominant digital advertising platform.")
                .investmentNote("Relatively low P/E for mega-cap tech. Google Cloud growing fast. Initiated its first-ever dividend in 2024.")
                .build());

        add(Stock.builder().ticker("AMZN").companyName("Amazon.com Inc.").sector("Consumer Discretionary").industry("E-Commerce / Cloud")
                .currentPrice(188.40).marketCapBillions(1960).peRatio(55.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(201.20).fiftyTwoWeekLow(118.35)
                .oneYearReturnPercent(51.3).beta(1.28)
                .description("World's largest e-commerce and cloud computing company. AWS is the #1 cloud provider. Also owns Whole Foods, Prime Video, Twitch, and Ring.")
                .investmentNote("No dividend — reinvests everything. AWS is the real profit engine. Long-term growth in cloud, advertising, and logistics automation.")
                .build());

        add(Stock.builder().ticker("JPM").companyName("JPMorgan Chase & Co.").sector("Financials").industry("Banking")
                .currentPrice(205.60).marketCapBillions(598).peRatio(12.5).dividendYieldPercent(2.18)
                .annualDividendPerShare(4.60).fiftyTwoWeekHigh(220.82).fiftyTwoWeekLow(135.19)
                .oneYearReturnPercent(38.5).beta(1.10)
                .description("Largest US bank by assets. Consumer banking, investment banking, asset management, and commercial banking across 100+ countries.")
                .investmentNote("Low P/E relative to market. Strong dividend with consistent growth. Financially benefits from higher interest rate environments.")
                .build());

        add(Stock.builder().ticker("JNJ").companyName("Johnson & Johnson").sector("Healthcare").industry("Pharmaceuticals / Medical Devices")
                .currentPrice(158.20).marketCapBillions(380).peRatio(17.5).dividendYieldPercent(3.14)
                .annualDividendPerShare(4.96).fiftyTwoWeekHigh(175.97).fiftyTwoWeekLow(143.13)
                .oneYearReturnPercent(-4.2).beta(0.54)
                .description("Healthcare giant in pharmaceuticals (Stelara, Darzalex) and medical devices (Depuy Synthes, Ethicon). Spun off Kenvue (consumer health brands) in 2023.")
                .investmentNote("Dividend King — 62 consecutive years of dividend growth. Defensive play; healthcare spending is relatively recession-resistant.")
                .build());

        add(Stock.builder().ticker("UNH").companyName("UnitedHealth Group").sector("Healthcare").industry("Health Insurance")
                .currentPrice(492.00).marketCapBillions(454).peRatio(18.8).dividendYieldPercent(1.55)
                .annualDividendPerShare(7.52).fiftyTwoWeekHigh(560.83).fiftyTwoWeekLow(445.42)
                .oneYearReturnPercent(-2.5).beta(0.58)
                .description("Largest US health insurer. Operates UnitedHealthcare (insurance) and Optum (pharmacy benefits, data analytics, and care delivery).")
                .investmentNote("Consistent earnings grower. Healthcare services are sticky and essential. Lower beta than most large-caps — smoother ride.")
                .build());

        add(Stock.builder().ticker("PG").companyName("Procter & Gamble Co.").sector("Consumer Staples").industry("Household Products")
                .currentPrice(168.30).marketCapBillions(396).peRatio(25.0).dividendYieldPercent(2.38)
                .annualDividendPerShare(3.76).fiftyTwoWeekHigh(173.85).fiftyTwoWeekLow(140.73)
                .oneYearReturnPercent(6.5).beta(0.52)
                .description("Owns 65+ consumer brands: Tide, Pampers, Gillette, Oral-B, Pantene, Head & Shoulders. Products sold in 180+ countries every day.")
                .investmentNote("Dividend King — 68 years of consecutive dividend growth. The textbook defensive / recession-proof investment. Low beta, steady income.")
                .build());

        add(Stock.builder().ticker("KO").companyName("The Coca-Cola Company").sector("Consumer Staples").industry("Beverages")
                .currentPrice(62.50).marketCapBillions(269).peRatio(23.0).dividendYieldPercent(3.12)
                .annualDividendPerShare(1.84).fiftyTwoWeekHigh(64.35).fiftyTwoWeekLow(51.55)
                .oneYearReturnPercent(7.8).beta(0.55)
                .description("World's most recognized brand. 200+ beverages in 200+ countries. Also owns Sprite, Fanta, Minute Maid, Dasani, Powerade, and Costa Coffee.")
                .investmentNote("Warren Buffett's famous holding. Dividend King — 62 consecutive years of dividend growth. Very low volatility, extremely reliable income.")
                .build());

        add(Stock.builder().ticker("PEP").companyName("PepsiCo Inc.").sector("Consumer Staples").industry("Beverages / Snacks")
                .currentPrice(172.00).marketCapBillions(235).peRatio(24.0).dividendYieldPercent(2.92)
                .annualDividendPerShare(5.06).fiftyTwoWeekHigh(184.08).fiftyTwoWeekLow(155.83)
                .oneYearReturnPercent(-3.2).beta(0.52)
                .description("Beverages (Pepsi, Mountain Dew, Gatorade, Lipton) AND snacks (Lay's, Doritos, Cheetos, Quaker Oats). More diversified revenue than Coca-Cola.")
                .investmentNote("Dividend King — 52 consecutive years of dividend growth. Snacks business provides stability when beverage volumes dip.")
                .build());

        add(Stock.builder().ticker("VZ").companyName("Verizon Communications").sector("Communication Services").industry("Telecom")
                .currentPrice(42.10).marketCapBillions(177).peRatio(9.0).dividendYieldPercent(6.62)
                .annualDividendPerShare(2.66).fiftyTwoWeekHigh(43.42).fiftyTwoWeekLow(30.14)
                .oneYearReturnPercent(26.4).beta(0.39)
                .description("Largest US wireless carrier by revenue. Operates Verizon Wireless and Fios broadband. Investing heavily in nationwide 5G network buildout.")
                .investmentNote("Very high yield (~6.6%). Slow growth but very reliable income. Suitable for income portfolios. Monitor debt levels — telecom is capital-intensive.")
                .build());

        add(Stock.builder().ticker("XOM").companyName("Exxon Mobil Corporation").sector("Energy").industry("Oil & Gas")
                .currentPrice(112.50).marketCapBillions(450).peRatio(14.0).dividendYieldPercent(3.24)
                .annualDividendPerShare(3.80).fiftyTwoWeekHigh(123.75).fiftyTwoWeekLow(94.15)
                .oneYearReturnPercent(6.2).beta(1.26)
                .description("World's largest publicly traded energy company. Produces crude oil, natural gas, and petrochemicals. Acquired Pioneer Natural Resources in 2024.")
                .investmentNote("Dividend Aristocrat. Energy provides portfolio inflation hedge. Cyclical — returns are tied to oil & gas commodity prices.")
                .build());

        add(Stock.builder().ticker("O").companyName("Realty Income Corporation").sector("Real Estate").industry("REIT - Retail")
                .currentPrice(55.20).marketCapBillions(46).peRatio(42.0).dividendYieldPercent(5.76)
                .annualDividendPerShare(3.08).fiftyTwoWeekHigh(58.19).fiftyTwoWeekLow(46.68)
                .oneYearReturnPercent(8.4).beta(0.87)
                .description("'The Monthly Dividend Company.' REIT owning 15,000+ commercial properties leased to tenants like Walgreens, Dollar General, and FedEx. Pays monthly dividends.")
                .investmentNote("Dividend Aristocrat paying MONTHLY dividends. Popular for income investors. REITs must distribute 90%+ of taxable income — high yields are structural.")
                .build());

        add(Stock.builder().ticker("BRK.B").companyName("Berkshire Hathaway Inc.").sector("Financials").industry("Holding Company / Insurance")
                .currentPrice(368.50).marketCapBillions(803).peRatio(21.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(394.56).fiftyTwoWeekLow(313.33)
                .oneYearReturnPercent(24.8).beta(0.91)
                .description("Warren Buffett's conglomerate. Wholly owns GEICO, BNSF Railway, and Berkshire Hathaway Energy. Holds large equity stakes in AAPL, BAC, AXP, and KO.")
                .investmentNote("No dividend — Buffett reinvests everything. Acts as a diversified one-stop holding. B shares (~$370) are accessible vs. A shares (~$550,000).")
                .build());
    }

    private void addChipStocks() {
        // ══════════════════════════════════════════════════════════════════════════════
        // SEMICONDUCTOR STOCKS — Organised by chip type and market cap tier
        //
        // The chip industry value chain (top to bottom):
        //   Equipment (ASML, AMAT, LRCX) → Foundry (TSM) → Fabless design (NVDA, AMD)
        //   IDM = companies that do both design + manufacturing (INTC, TXN, MU)
        //
        // Why chips matter: Every smartphone, car, data centre, and appliance needs
        // semiconductors. AI has created a supercycle of demand for advanced chips.
        // ══════════════════════════════════════════════════════════════════════════════

        // ── LARGE CAP  Foundry ───────────────────────────────────────────────────────
        add(Stock.builder().ticker("TSM").companyName("Taiwan Semiconductor Manufacturing").sector("Technology")
                .industry("Semiconductors").chipType("FOUNDRY")
                .currentPrice(175.00).marketCapBillions(910).peRatio(28.0).dividendYieldPercent(1.35)
                .annualDividendPerShare(2.32).fiftyTwoWeekHigh(226.40).fiftyTwoWeekLow(127.40)
                .oneYearReturnPercent(38.5).beta(1.32)
                .description("The world's most important company you've never heard of. TSMC manufactures chips for Apple, NVIDIA, AMD, Qualcomm — anyone who designs but doesn't fab. Controls ~90% of advanced chip manufacturing (sub-5nm). Without TSMC, the modern world stops.")
                .investmentNote("Pure-play foundry with an irreplaceable moat. Geographic risk (Taiwan/China tension) is the primary concern. US/Japan/Europe are funding TSMC fab expansion globally (Arizona, Germany, Japan).")
                .build());

        // ── LARGE CAP  Equipment ─────────────────────────────────────────────────────
        add(Stock.builder().ticker("ASML").companyName("ASML Holding N.V.").sector("Technology")
                .industry("Semiconductor Equipment").chipType("EQUIPMENT")
                .currentPrice(780.00).marketCapBillions(307).peRatio(32.0).dividendYieldPercent(0.88)
                .annualDividendPerShare(6.85).fiftyTwoWeekHigh(1110.09).fiftyTwoWeekLow(658.01)
                .oneYearReturnPercent(-18.5).beta(1.45)
                .description("ASML is the ONLY company in the world that makes EUV (Extreme Ultraviolet) lithography machines — the $400M machines required to print the most advanced chips. Without ASML, no one can make chips below 7nm. True monopoly.")
                .investmentNote("Most powerful moat in all of technology — a literal monopoly on EUV machines. Recent pullback from highs due to China export restrictions. Long-term demand for advanced chips makes this a strategic pick-and-shovels play.")
                .build());

        add(Stock.builder().ticker("AMAT").companyName("Applied Materials Inc.").sector("Technology")
                .industry("Semiconductor Equipment").chipType("EQUIPMENT")
                .currentPrice(195.00).marketCapBillions(163).peRatio(20.0).dividendYieldPercent(0.90)
                .annualDividendPerShare(1.68).fiftyTwoWeekHigh(255.89).fiftyTwoWeekLow(171.11)
                .oneYearReturnPercent(-8.2).beta(1.55)
                .description("World's largest semiconductor equipment company. Makes machines for depositing, etching, and testing chip materials. Every new fab in the world buys Applied Materials equipment.")
                .investmentNote("Picks-and-shovels play on the global chip buildout. Benefits from both leading-edge (TSMC, Samsung) and trailing-edge (China, automotive) fab investments.")
                .build());

        add(Stock.builder().ticker("LRCX").companyName("Lam Research Corporation").sector("Technology")
                .industry("Semiconductor Equipment").chipType("EQUIPMENT")
                .currentPrice(820.00).marketCapBillions(109).peRatio(22.0).dividendYieldPercent(1.10)
                .annualDividendPerShare(8.00).fiftyTwoWeekHigh(1134.00).fiftyTwoWeekLow(718.70)
                .oneYearReturnPercent(-12.0).beta(1.60)
                .description("Specialises in etch and deposition equipment — critical steps in chip manufacturing. Leader in memory chip manufacturing equipment (DRAM, NAND). Partners with all major chipmakers globally.")
                .investmentNote("Memory chip capex cycles drive revenue. Current downturn in NAND creates a buying opportunity if you believe in long-term AI storage demand.")
                .build());

        add(Stock.builder().ticker("KLAC").companyName("KLA Corporation").sector("Technology")
                .industry("Semiconductor Equipment").chipType("EQUIPMENT")
                .currentPrice(740.00).marketCapBillions(101).peRatio(24.0).dividendYieldPercent(0.95)
                .annualDividendPerShare(6.24).fiftyTwoWeekHigh(888.13).fiftyTwoWeekLow(596.01)
                .oneYearReturnPercent(-5.5).beta(1.42)
                .description("Process control and inspection equipment for semiconductor manufacturing. Chips have billions of transistors — KLA's machines check every layer for defects. As chips get smaller, inspection becomes more critical.")
                .investmentNote("Less cyclical than other equipment makers because chipmakers always need to inspect regardless of capex cycle. Strong dividend growth track record.")
                .build());

        // ── LARGE CAP  Fabless ───────────────────────────────────────────────────────
        add(Stock.builder().ticker("AMD").companyName("Advanced Micro Devices Inc.").sector("Technology")
                .industry("Semiconductors").chipType("FABLESS")
                .currentPrice(168.00).marketCapBillions(272).peRatio(110.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(227.30).fiftyTwoWeekLow(117.80)
                .oneYearReturnPercent(-5.5).beta(1.72)
                .description("Makes Ryzen CPUs (competing with Intel) and RDNA/CDNA GPUs (competing with NVIDIA). AMD's EPYC server CPUs have taken significant data-centre market share from Intel. MI300X AI accelerators challenge NVIDIA's H100/H200.")
                .investmentNote("Gaining market share in CPUs. MI300X is a credible NVIDIA H100 alternative at lower cost. High P/E reflects growth expectations — priced for perfection. Volatile but exciting.")
                .build());

        add(Stock.builder().ticker("QCOM").companyName("Qualcomm Incorporated").sector("Technology")
                .industry("Semiconductors").chipType("FABLESS")
                .currentPrice(168.00).marketCapBillions(187).peRatio(16.0).dividendYieldPercent(2.10)
                .annualDividendPerShare(3.40).fiftyTwoWeekHigh(230.63).fiftyTwoWeekLow(128.00)
                .oneYearReturnPercent(-2.0).beta(1.35)
                .description("Designs Snapdragon processors (used in Samsung, Android flagship phones) and 5G modem chips. Also growing in automotive (Snapdragon Digital Chassis) and AI PCs (Snapdragon X Elite).")
                .investmentNote("Attractive P/E of 16 for a dominant mobile chip designer. Apple dependency risk (was ~25% of revenue) is fading as Apple builds its own modems. Automotive expansion is a multi-year tailwind.")
                .build());

        add(Stock.builder().ticker("AVGO").companyName("Broadcom Inc.").sector("Technology")
                .industry("Semiconductors").chipType("FABLESS")
                .currentPrice(1650.00).marketCapBillions(774).peRatio(55.0).dividendYieldPercent(1.18)
                .annualDividendPerShare(21.00).fiftyTwoWeekHigh(1848.99).fiftyTwoWeekLow(1084.61)
                .oneYearReturnPercent(32.5).beta(1.18)
                .description("Makes networking chips (Ethernet switches used in hyperscale data centres), Wi-Fi chips, storage controllers, and custom AI accelerators for Google (TPUs) and Meta. Acquired VMware in 2023 for $61B adding enterprise software.")
                .investmentNote("Custom AI chip (XPU) business for hyperscalers is a key growth driver. VMware acquisition diversifies into high-margin software. Strong dividend growth history — 20+ years of consecutive increases.")
                .build());

        add(Stock.builder().ticker("MRVL").companyName("Marvell Technology Inc.").sector("Technology")
                .industry("Semiconductors").chipType("FABLESS")
                .currentPrice(68.00).marketCapBillions(58).peRatio(65.0).dividendYieldPercent(0.28)
                .annualDividendPerShare(0.24).fiftyTwoWeekHigh(119.01).fiftyTwoWeekLow(50.04)
                .oneYearReturnPercent(-22.5).beta(1.65)
                .description("Makes chips for data-centre networking, storage, and custom AI accelerators. Key customer is Amazon (AWS Trainium/Inferentia). Also makes optical DSPs for high-speed data-centre interconnects.")
                .investmentNote("Significant pullback from highs despite strong AI tailwinds. AWS and Microsoft are key custom silicon customers. High-risk, high-reward AI infrastructure play in the mid-cap space.")
                .build());

        // ── LARGE CAP  IDM ───────────────────────────────────────────────────────────
        add(Stock.builder().ticker("INTC").companyName("Intel Corporation").sector("Technology")
                .industry("Semiconductors").chipType("IDM")
                .currentPrice(22.50).marketCapBillions(95).peRatio(0.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(37.99).fiftyTwoWeekLow(17.67)
                .oneYearReturnPercent(-30.5).beta(1.05)
                .description("Once the world's dominant chip company. Lost manufacturing leadership to TSMC. Now investing $100B+ in new fabs (Intel Foundry Services) to reclaim leadership by 2026-2027 with Intel 18A process. Cut dividend in 2024.")
                .investmentNote("Deep value turnaround play — or a value trap. Intel 18A process performance vs. TSMC N2 will be the defining moment. IDM 2.0 strategy is either a brilliant pivot or too-little-too-late. High risk, high potential reward. NOT for beginners.")
                .build());

        add(Stock.builder().ticker("TXN").companyName("Texas Instruments Incorporated").sector("Technology")
                .industry("Semiconductors").chipType("ANALOG")
                .currentPrice(192.00).marketCapBillions(175).peRatio(32.0).dividendYieldPercent(2.86)
                .annualDividendPerShare(5.16).fiftyTwoWeekHigh(220.38).fiftyTwoWeekLow(162.46)
                .oneYearReturnPercent(1.5).beta(0.98)
                .description("World's largest maker of analog chips — the 'boring' chips that control power, temperature, and signals in every car, appliance, and industrial machine. Sells 80,000+ chip products to 100,000+ customers. Near-monopoly in industrial/automotive analog.")
                .investmentNote("Dividend Aristocrat with 21+ years of consecutive increases. Analog chips are far less cyclical than logic chips — still needed in older manufacturing processes. The 'sleep well at night' semiconductor stock.")
                .build());

        add(Stock.builder().ticker("MU").companyName("Micron Technology Inc.").sector("Technology")
                .industry("Semiconductors").chipType("MEMORY")
                .currentPrice(108.00).marketCapBillions(120).peRatio(18.0).dividendYieldPercent(0.37)
                .annualDividendPerShare(0.46).fiftyTwoWeekHigh(157.54).fiftyTwoWeekLow(79.15)
                .oneYearReturnPercent(-5.0).beta(1.55)
                .description("One of only 3 DRAM manufacturers globally (alongside Samsung and SK Hynix). Makes HBM3E (High Bandwidth Memory) used in NVIDIA's AI GPUs — a massive tailwind. Also makes NAND flash storage.")
                .investmentNote("HBM (High Bandwidth Memory) for AI GPUs is transformational — NVIDIA needs HBM for every H100/H200/B200. Memory chips are cyclical but AI is creating a new sustained demand floor. Solid value at current P/E.")
                .build());

        // ── MID CAP  IDM / Analog ────────────────────────────────────────────────────
        add(Stock.builder().ticker("ADI").companyName("Analog Devices Inc.").sector("Technology")
                .industry("Semiconductors").chipType("ANALOG")
                .currentPrice(215.00).marketCapBillions(111).peRatio(28.0).dividendYieldPercent(1.75)
                .annualDividendPerShare(3.68).fiftyTwoWeekHigh(244.93).fiftyTwoWeekLow(186.27)
                .oneYearReturnPercent(-3.0).beta(1.12)
                .description("Precision analog and mixed-signal chips for industrial automation, healthcare instruments, communications, and automotive. Used in medical imaging, factory robots, 5G base stations, and EV battery management.")
                .investmentNote("Less cyclical than logic/memory semiconductors. Analog chips stay in production 10-20 years vs. 2-3 years for logic. Strong dividend growth. ADI + Maxim acquisition in 2021 created a diversified analog powerhouse.")
                .build());

        add(Stock.builder().ticker("MCHP").companyName("Microchip Technology Inc.").sector("Technology")
                .industry("Semiconductors").chipType("IDM")
                .currentPrice(58.00).marketCapBillions(31).peRatio(20.0).dividendYieldPercent(3.10)
                .annualDividendPerShare(1.752).fiftyTwoWeekHigh(98.00).fiftyTwoWeekLow(47.35)
                .oneYearReturnPercent(-32.0).beta(1.42)
                .description("World's #1 maker of microcontrollers (MCUs) — tiny programmable chips embedded in everything from dishwashers to cars to factory equipment. Every electronic product with a button probably has a Microchip MCU inside.")
                .investmentNote("Significant pullback creates value opportunity. MCUs are in every embedded system globally. Dividend yield of 3.1% with a long growth history. Key question: when does inventory digestion end?")
                .build());

        add(Stock.builder().ticker("ON").companyName("onsemi (ON Semiconductor)").sector("Technology")
                .industry("Semiconductors").chipType("ANALOG")
                .currentPrice(55.00).marketCapBillions(24).peRatio(13.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(80.81).fiftyTwoWeekLow(43.52)
                .oneYearReturnPercent(-25.5).beta(1.68)
                .description("Power semiconductors and image sensors. Dominant in Silicon Carbide (SiC) power chips for EV onboard chargers and traction inverters. Also makes CMOS image sensors and intelligent power modules.")
                .investmentNote("EV transition makes SiC power chips a structural growth market. Trading at P/E 13 — cheap for a company with EV exposure. Risk: EV adoption slowdown. Reward: every EV needs SiC chips.")
                .build());

        // ── MID CAP  Fabless ─────────────────────────────────────────────────────────
        add(Stock.builder().ticker("MPWR").companyName("Monolithic Power Systems Inc.").sector("Technology")
                .industry("Semiconductors").chipType("FABLESS")
                .currentPrice(545.00).marketCapBillions(26).peRatio(62.0).dividendYieldPercent(0.82)
                .annualDividendPerShare(4.20).fiftyTwoWeekHigh(1006.00).fiftyTwoWeekLow(424.00)
                .oneYearReturnPercent(-28.0).beta(1.48)
                .description("Makes highly efficient power management ICs for AI servers, storage, automotive, and consumer electronics. Its VR14 voltage regulators are used in Intel and AMD server CPUs. Extremely high gross margins (~56%).")
                .investmentNote("Massive pullback from $1006 high. Power management is critical for AI data centres — more AI compute = more power chips needed. High-quality business trading at a discount. Growth at a reasonable price for patient investors.")
                .build());

        // ── SMALL CAP ────────────────────────────────────────────────────────────────
        add(Stock.builder().ticker("SWKS").companyName("Skyworks Solutions Inc.").sector("Technology")
                .industry("Semiconductors").chipType("FABLESS")
                .currentPrice(78.00).marketCapBillions(12).peRatio(11.0).dividendYieldPercent(2.89)
                .annualDividendPerShare(2.80).fiftyTwoWeekHigh(103.96).fiftyTwoWeekLow(64.32)
                .oneYearReturnPercent(-15.5).beta(1.22)
                .description("Makes radio frequency (RF) chips and front-end modules for 5G smartphones. Key supplier to Apple for iPhone RF chips. Also expanding into automotive (V2X connectivity) and IoT.")
                .investmentNote("Cheap at P/E 11. Apple concentration risk (~65% of revenue) is well-known and priced in. 5G upgrade cycle and automotive expansion provide diversification. Dividend yield approaching 3% adds income angle.")
                .build());

        add(Stock.builder().ticker("CRUS").companyName("Cirrus Logic Inc.").sector("Technology")
                .industry("Semiconductors").chipType("FABLESS")
                .currentPrice(115.00).marketCapBillions(7).peRatio(18.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(144.23).fiftyTwoWeekLow(87.47)
                .oneYearReturnPercent(-5.0).beta(1.38)
                .description("Supplies audio chips and haptics controllers to Apple — found in every iPhone, iPad, and MacBook for audio codec and amplifier functions. High Apple concentration (~85% revenue) but entrenched position in Apple's supply chain.")
                .investmentNote("Single-customer risk is extreme but the relationship is deep and sticky. Cirrus chips are in every iPhone ever made. No dividend — uses cash for buybacks. Niche small-cap with reliable Apple revenue stream.")
                .build());

        add(Stock.builder().ticker("SLAB").companyName("Silicon Laboratories Inc.").sector("Technology")
                .industry("Semiconductors").chipType("FABLESS")
                .currentPrice(95.00).marketCapBillions(2.3).peRatio(0.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(143.36).fiftyTwoWeekLow(72.34)
                .oneYearReturnPercent(-18.5).beta(1.35)
                .description("Pure-play IoT wireless chip company. Makes chips for smart home devices, industrial IoT, and smart meters. Products include Bluetooth, Zigbee, Z-Wave, and proprietary Sub-GHz wireless chips. Refocused after selling infrastructure business to Skyworks in 2021.")
                .investmentNote("Small-cap pure IoT play. Revenue is recovering from inventory correction. IoT device proliferation is a long-term tailwind. Higher risk due to small size and current losses — suited for speculative allocation only.")
                .build());

        add(Stock.builder().ticker("FORM").companyName("FormFactor Inc.").sector("Technology")
                .industry("Semiconductor Equipment").chipType("EQUIPMENT")
                .currentPrice(42.00).marketCapBillions(1.9).peRatio(28.0).dividendYieldPercent(0.0)
                .annualDividendPerShare(0.0).fiftyTwoWeekHigh(53.95).fiftyTwoWeekLow(32.01)
                .oneYearReturnPercent(-10.5).beta(1.48)
                .description("Makes probe cards — the test equipment used to verify semiconductor wafers before they're cut into chips. Every chip must be tested; FormFactor's probes are used in testing leading-edge chips at TSMC, Samsung, and Intel.")
                .investmentNote("Smallest chip equipment stock in our list. Niche but essential — every wafer needs testing. Proxy for overall semiconductor production volume. High-risk small-cap with torque in chip upcycles.")
                .build());
    }

    private void add(Stock s) {
        stocks.put(s.getTicker(), s);
    }

    public List<Stock> getAllStocks() {
        return new ArrayList<>(stocks.values());
    }

    public Optional<Stock> getByTicker(String ticker) {
        return Optional.ofNullable(stocks.get(ticker.toUpperCase()));
    }

    public List<Stock> getBySector(String sector) {
        return stocks.values().stream()
                .filter(s -> s.getSector().equalsIgnoreCase(sector))
                .collect(Collectors.toList());
    }

    public List<Stock> search(String query) {
        String q = query.toLowerCase();
        return stocks.values().stream()
                .filter(s -> s.getTicker().toLowerCase().contains(q)
                        || s.getCompanyName().toLowerCase().contains(q)
                        || s.getSector().toLowerCase().contains(q)
                        || s.getIndustry().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public List<String> getAllSectors() {
        return stocks.values().stream()
                .map(Stock::getSector)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** All semiconductor/chip stocks only */
    public List<Stock> getChipStocks() {
        return stocks.values().stream()
                .filter(Stock::isChipStock)
                .collect(Collectors.toList());
    }

    /** Chip stocks filtered by chip type (FABLESS, IDM, FOUNDRY, EQUIPMENT, MEMORY, ANALOG) */
    public List<Stock> getChipStocksByType(String chipType) {
        return stocks.values().stream()
                .filter(s -> s.isChipStock() && chipType.equalsIgnoreCase(s.getChipType()))
                .collect(Collectors.toList());
    }

    /** Group chip stocks by their size tier */
    public Map<String, List<Stock>> getChipStocksBySize() {
        return getChipStocks().stream()
                .collect(Collectors.groupingBy(Stock::getSizeCategory,
                        LinkedHashMap::new, Collectors.toList()));
    }
}
