# 📈 MarketCompass

**MarketCompass** is a Java 21 / Spring Boot investment research and paper-trading platform designed as a production-oriented portfolio project.

It combines market-data workflows, portfolio tracking, a simulated trading wallet, watchlists and an optional local LLM assistant. The project is intentionally built around real backend engineering concerns: modular design, validation, persistence-ready domain boundaries, automated testing and CI/CD.

> ⚠️ **Educational project only.** MarketCompass does not execute real trades and its AI features are not financial advice.

## Architecture

```text
                    +----------------------+
                    |   Client / REST API  |
                    +----------+-----------+
                               |
                    +----------v-----------+
                    | MarketCompass Core   |
                    | Spring Boot / Java21 |
                    +----+----+----+-------+
                         |    |    |
              +----------+    |    +----------+
              |               |               |
        Portfolio        Paper Wallet     Watchlist
              |               |               |
              +---------------+---------------+
                              |
                       Market Data Layer
                              |
                 +------------+------------+
                 |                         |
          External Provider          Local/Test Data
                 |
          +------v------+
          | LLM Module  |
          | Spring AI   |
          +------+------+
                 |
              Ollama
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Current features

- Virtual paper-trading wallet with a $25,000 starting balance
- Buy and sell by share count or dollar amount
- Trade history
- Portfolio and holdings APIs
- Stock and ETF APIs
- Dividend-focused stock data
- Watchlist and price-alert concepts
- Brokerage-agent scanning/recommendation endpoints
- Optional Spring AI + Ollama LLM service
- Java 21 and Spring Boot 3.5
- Multi-module Maven structure
- GitHub Actions CI

## API overview

| Area | Method | Endpoint |
|---|---|---|
| Wallet | GET | `/api/wallet` |
| Wallet | GET | `/api/wallet/history` |
| Wallet | POST | `/api/wallet/buy` |
| Wallet | POST | `/api/wallet/buy/amount` |
| Wallet | POST | `/api/wallet/sell` |
| Portfolio | GET | `/api/portfolio` |
| Portfolio | POST | `/api/portfolio/holdings` |
| Portfolio | DELETE | `/api/portfolio/holdings/{ticker}` |
| Stocks | GET | `/api/stocks` |
| Stocks | GET | `/api/stocks/{ticker}` |
| ETFs | GET | `/api/etfs` |
| ETFs | GET | `/api/etfs/{ticker}` |
| Dividends | GET | `/api/dividends` |
| Watchlist | GET | `/api/watchlist` |
| Watchlist | GET | `/api/watchlist/alerts` |
| Watchlist | POST | `/api/watchlist` |
| Watchlist | DELETE | `/api/watchlist/{ticker}` |
| Agent | GET | `/api/agent/scan` |
| Agent | GET | `/api/agent/recommendations` |

## Example

```bash
curl http://localhost:8080/api/wallet
```

```bash
curl -X POST http://localhost:8080/api/wallet/buy \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"AAPL","shares":5}'
```

## Run locally

Requirements:

- Java 21
- Maven Wrapper
- Optional: Ollama for the LLM module

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

The core application is available on `http://localhost:8080`.

For the LLM module, see [`market-compass-llm/README.md`](market-compass-llm/README.md).

## Quality and engineering practices

- Java 21
- Spring Boot 3.5
- Maven multi-module build
- GitHub Actions CI on pushes and pull requests
- Separation of core application and LLM integration
- Production roadmap covering PostgreSQL, migrations, validation, observability and cloud deployment

## Roadmap

### Near term

- PostgreSQL persistence + Flyway migrations
- Bean Validation and consistent API error responses
- OpenAPI documentation
- Market-data provider abstraction
- More unit/integration tests

### Medium term

- Historical prices and fundamentals
- Caching and rate-limit handling
- Scheduled watchlist alerts
- LLM source attribution and retrieval

### Cloud

- Container deployment
- Managed PostgreSQL
- Health/readiness checks
- Metrics and structured logging
- Secrets management
- Automated deployment

## Project structure

```text
market-compass/
├── market-compass-core/       # Portfolio, wallet, market and watchlist domain
├── market-compass-llm/        # Optional Spring AI / Ollama integration
├── docs/                      # Architecture and engineering roadmap
├── .github/workflows/         # CI/CD automation
├── pom.xml
└── mvnw
```

## License

This project is intended as a learning and portfolio project. Add an explicit open-source license before accepting external contributions.
