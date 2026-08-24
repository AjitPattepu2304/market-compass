# MarketCompass Architecture

MarketCompass is a modular Spring Boot 3 application built with Java 21. The project is split into a core application and an LLM integration module.

## Current architecture

```text
Client
  |
  v
MarketCompass Core (Spring Boot)
  |-- Stocks / ETFs
  |-- Portfolio
  |-- Paper Wallet
  |-- Watchlist / Alerts
  |-- Brokerage Agent
  |
  +--> Market data services

MarketCompass LLM (Spring AI)
  |
  +--> Ollama
      |
      +--> Local LLM
```

## Production direction

The next evolution is to replace in-memory state with a persistent data layer, isolate external market-data integrations behind interfaces, add validation and centralized exception handling, and expose operational health metrics. The LLM module should remain an optional integration so the core application remains usable when Ollama is unavailable.

## Design principles

- Keep domain logic out of controllers.
- Isolate external providers behind service interfaces.
- Validate API input at the boundary.
- Never commit secrets or provider credentials.
- Prefer deterministic calculations for portfolio and paper-trading operations.
- Treat LLM output as advisory text, never as the source of truth for financial calculations.
- Keep infrastructure concerns separate from business logic.
