# 📈 MarketCompass

**MarketCompass** is a Java 21 / Spring Boot investment research and paper-trading platform designed as a production-oriented backend portfolio project.

It combines market-data workflows, portfolio tracking, a persistent paper-trading wallet, watchlists and an optional local LLM assistant. It is educational software only and does not execute real trades or provide financial advice.

## Architecture

```text
Client / REST API
        |
        v
Spring Boot Core
  |       |        |
Portfolio Wallet Watchlist
  |       |        |
  +-------+--------+
          |
   Market Data Layer
          |
    PostgreSQL (prod)
    H2 (local fallback)
          |
   Spring AI / Ollama
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Production foundation implemented

- Java 21 / Spring Boot 3.5
- Maven multi-module architecture
- PostgreSQL persistence with Flyway migrations
- Transactional persistent paper trading
- Portfolio and holding persistence
- Trade history persistence
- PostgreSQL profile with environment-based database configuration
- H2 local runtime fallback for development/CI
- Local PostgreSQL Docker Compose stack
- Spring Boot Actuator health/info endpoints
- GitHub Actions CI
- Optional Spring AI + Ollama integration

## Paper trading

The default paper account starts with **$25,000**. Trades are simulated only.

In the `postgres` profile, buys/sells are persisted and processed transactionally. Market prices are resolved from the existing live-price service with static stock/ETF data as a fallback.

### API

| Area | Method | Endpoint |
|---|---|---|
| Wallet | GET | `/api/wallet` |
| Wallet | GET | `/api/wallet/history` |
| Wallet | GET | `/api/wallet/history/{ticker}` |
| Wallet | POST | `/api/wallet/buy` |
| Wallet | POST | `/api/wallet/buy/amount` |
| Wallet | POST | `/api/wallet/sell` |
| Portfolio | GET | `/api/portfolio` |
| Portfolio | GET | `/api/portfolio/summary` |
| Portfolio | POST | `/api/portfolio/holdings` |
| Portfolio | DELETE | `/api/portfolio/holdings/{ticker}` |
| Stocks | GET | `/api/stocks` |
| ETFs | GET | `/api/etfs` |
| Dividends | GET | `/api/dividends` |
| Watchlist | GET | `/api/watchlist` |
| Watchlist | POST | `/api/watchlist` |
| Watchlist | DELETE | `/api/watchlist/{ticker}` |
| Agent | GET | `/api/agent/scan` |
| Agent | GET | `/api/agent/recommendations` |

## Run with PostgreSQL

Start PostgreSQL:

```bash
docker compose -f docker-compose.postgres.yml up -d
```

Run the application using the PostgreSQL profile:

```bash
./mvnw spring-boot:run -pl market-compass-core -Dspring-boot.run.profiles=postgres
```

Flyway automatically applies migrations from `market-compass-core/src/main/resources/db/migration`.

For a remote database, set:

```bash
export DB_URL='jdbc:postgresql://host:5432/marketcompass'
export DB_USERNAME='marketcompass'
export DB_PASSWORD='your-secret'
```

Do not commit database credentials.

## Run locally without PostgreSQL

The default profile uses an in-memory H2 datasource so the application and existing demo features remain easy to run:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

The persistent PostgreSQL implementation is enabled only with the `postgres` profile.

## Example

```bash
curl http://localhost:8080/api/wallet
```

```bash
curl -X POST http://localhost:8080/api/wallet/buy \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"AAPL","shares":5}'
```

## Engineering roadmap

### Next

- Bean Validation and consistent API error responses
- OpenAPI/Swagger documentation
- Market-data provider interface and real provider adapters
- Integration tests using PostgreSQL/Testcontainers
- Optimistic/concurrency controls for portfolio updates

### Platform

- Redis caching for market quotes
- Kafka events for `TradeExecuted` and portfolio updates
- Scheduled watchlist alerts
- Structured logging and metrics
- Cloud deployment with managed PostgreSQL

### AI

- Provider-neutral LLM service
- Retrieval of trusted market/company sources
- Source attribution
- Portfolio-aware explanations
- Guardrails preventing the LLM from becoming the source of financial calculations

## Project structure

```text
market-compass/
├── market-compass-core/
│   ├── src/main/java/.../investment/
│   │   ├── controller/       # REST boundary
│   │   ├── service/          # business logic
│   │   ├── model/            # API/domain models
│   │   └── persistence/      # PostgreSQL implementation
│   └── src/main/resources/
│       └── db/migration/     # Flyway schema history
├── market-compass-llm/       # Spring AI / Ollama integration
├── docs/
├── .github/workflows/
├── docker-compose.postgres.yml
└── mvnw
```

## License

This project is intended as a learning and portfolio project. Add an explicit open-source license before accepting external contributions.
