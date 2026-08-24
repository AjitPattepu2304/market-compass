# MarketCompass Engineering Roadmap

## Phase 1 — Production foundation

- [ ] Persistent PostgreSQL storage for users, portfolios, holdings, trades and watchlists
- [ ] Flyway database migrations
- [ ] Bean Validation on API request models
- [ ] Global REST exception handling with stable error responses
- [ ] OpenAPI/Swagger API documentation
- [ ] Provider abstraction for market-data APIs
- [ ] Configuration through environment variables
- [ ] Unit and integration test coverage for trading and portfolio calculations
- [ ] CI pipeline for build, test and static checks

## Phase 2 — Market intelligence

- [ ] Real-time/delayed market-price provider
- [ ] Historical price endpoints
- [ ] Fundamental metrics and dividend history
- [ ] Caching and rate-limit handling
- [ ] Scheduled watchlist price alerts
- [ ] Market-hours awareness based on exchange calendar

## Phase 3 — AI assistant

- [ ] LLM service with provider-neutral interface
- [ ] Retrieval of trusted market/company documents
- [ ] Source attribution in AI responses
- [ ] Conversation history
- [ ] Guardrails that prevent the assistant from presenting financial advice as guaranteed outcomes
- [ ] Graceful degradation when the LLM provider is unavailable

## Phase 4 — Cloud and observability

- [ ] Containerized deployment
- [ ] PostgreSQL managed database
- [ ] Actuator health/readiness endpoints
- [ ] Structured logging
- [ ] Metrics and dashboards
- [ ] Secrets management
- [ ] Automated deployment from main after CI passes
