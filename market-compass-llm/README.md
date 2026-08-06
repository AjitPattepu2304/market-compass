# MarketCompass LLM Module

LLM-powered Q&A service for investment advice using **Ollama** and **LLaMA2**.

## Prerequisites

### 1. Install Ollama

Download and install from: https://ollama.ai

**macOS:**
```bash
brew install ollama
```

**Linux:**
```bash
curl https://ollama.ai/install.sh | sh
```

**Windows:**
Download from https://ollama.ai/download

### 2. Pull LLaMA2 Model

```bash
ollama pull llama2
```

This downloads the LLaMA2 model (~4GB). You only need to do this once.

### 3. Start Ollama Server

```bash
ollama serve
```

The server will run on `http://localhost:11434` by default.

## Running the LLM Service

### From Maven

```bash
# From project root
cd market-compass-llm
mvn spring-boot:run
```

The service will start on `http://localhost:8081`

### From IntelliJ

1. Right-click `MarketCompassLLMApplication.java`
2. Select **Run 'MarketCompassLLMApplication'**

## API Endpoints

### 1. Ask a Question

```bash
POST /api/llm/qa
Content-Type: application/json

{
  "question": "What is a good dividend stock for beginners?"
}
```

**Response:**
```json
{
  "question": "What is a good dividend stock for beginners?",
  "answer": "For beginners looking to invest in dividend stocks, consider blue-chip companies with...",
  "model": "ollama/llama2",
  "timestamp": "2026-01-15T10:30:00"
}
```

### 2. Ask with Context

Include portfolio or market context for more tailored responses:

```bash
POST /api/llm/qa/contextual
Content-Type: application/json

{
  "question": "Should I buy more AAPL?",
  "context": "Current portfolio: 100 shares of AAPL at $180, 50 shares of MSFT at $420. Current market: Tech sector up 5% this quarter."
}
```

### 3. Health Check

```bash
GET /api/llm/health
```

**Response:**
```
LLM service is running
```

## Configuration

Edit `src/main/resources/application.yml` to customize:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434  # Ollama server URL
      model: llama2                      # Model to use
```

## Troubleshooting

### "Connection refused" error

1. Make sure Ollama is running: `ollama serve`
2. Check it's on the correct port: `http://localhost:11434`
3. Verify in application.yml that `base-url` matches

### Model not found

```bash
# List available models
ollama list

# Pull a model
ollama pull llama2
```

### Slow responses

- LLaMA2 can be slow on CPU-only machines (10-30 seconds per response)
- GPU acceleration will improve performance significantly
- Consider using a smaller model: `ollama pull neural-chat` (faster, less accurate)

## Architecture

```
QAController
    ↓
LLMService
    ↓
Spring AI ChatClient
    ↓
Ollama API
    ↓
LLaMA2 Model
```

## Next Steps

1. **Integrate with Market-Compass-Core**
   - Call LLM service from investment endpoints
   - Add context from portfolio/market data

2. **Enhancements**
   - Add RAG (Retrieval-Augmented Generation) for financial documents
   - Implement caching for common questions
   - Add conversation history/multi-turn support

3. **Models to Try**
   - `mistral` — faster, good performance
   - `neural-chat` — optimized for chat
   - `llama2-uncensored` — less filtered responses

## Resources

- [Ollama Documentation](https://github.com/ollama/ollama)
- [Spring AI Guide](https://spring.io/projects/spring-ai)
- [LLaMA2 Paper](https://arxiv.org/abs/2307.09288)
