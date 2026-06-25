# 📈 MarketCompass

An investment study hub for learning stocks, ETFs, dividends, semiconductor sector analysis, and portfolio tracking.
Includes a virtual paper-trading wallet and an AI-style brokerage agent.

---

## 🔗 Links

| | URL |
|---|---|
| **GitHub Repo** | https://github.com/AjitPattepu2304/market-compass |
| **Local App** | http://localhost:8080 |
| **Render (Beta Deploy)** | https://market-compass.onrender.com _(live after first deploy)_ |

---

## 🚀 Run Locally

```bash
./mvnw spring-boot:run
```

> On Walmart network: offline mode is auto-applied via `.mvn/maven.config`.
> Off VPN / home network: full Maven Central access.

---

## 💰 Virtual Wallet

Starting balance: **$25,000** (paper trading — no real money)

| Method | Endpoint | Body |
|---|---|---|
| GET | `/api/wallet` | — |
| GET | `/api/wallet/history` | — |
| GET | `/api/wallet/history/{ticker}` | — |
| POST | `/api/wallet/buy` | `{"ticker":"AAPL","shares":5}` |
| POST | `/api/wallet/buy/amount` | `{"ticker":"NVDA","amount":500}` |
| POST | `/api/wallet/sell` | `{"ticker":"AAPL","shares":3}` |

---

## 📊 Portfolio

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/portfolio` | Summary + holdings |
| POST | `/api/portfolio/holdings` | Add holding manually |
| DELETE | `/api/portfolio/holdings/{ticker}` | Remove holding |

---

## 📈 Stocks & ETFs

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/stocks` | All stocks |
| GET | `/api/stocks/{ticker}` | Single stock |
| GET | `/api/etfs` | All ETFs |
| GET | `/api/etfs/{ticker}` | Single ETF |
| GET | `/api/dividends` | Dividend stocks |

---

## 👁️ Watchlist

| Method | Endpoint | Body |
|---|---|---|
| GET | `/api/watchlist` | — |
| GET | `/api/watchlist/alerts` | — |
| GET | `/api/watchlist/{ticker}` | — |
| POST | `/api/watchlist` | `{"ticker":"NVDA","targetPrice":850,"alertThreshold":5,"notes":"..."}` |
| DELETE | `/api/watchlist/{ticker}` | — |

---

## 🤖 Brokerage Agent

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/agent/scan` | Scan for opportunities |
| GET | `/api/agent/recommendations` | Buy/sell recommendations |

---

## 🗺️ Roadmap

- [x] Virtual wallet ($25k balance)
- [x] Buy / Sell by shares or dollar amount
- [x] Trade history
- [x] Watchlist (add/remove tickers to monitor)
- [x] Market open/close simulation with agent reactions
- [x] Docker polish (.dockerignore, JVM container flags, HEALTHCHECK)
- [ ] SIP scheduler (auto-invest on interval)
- [ ] Render.com auto-deploy from main

---

## 🧱 Tech Stack

- Java 21
- Spring Boot 3.5
- In-memory data (no DB — stateless for easy cloud deploy)
- Maven (Maven Central — no Walmart Artifactory)
