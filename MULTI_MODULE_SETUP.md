# Multi-Module MarketCompass Setup

## Architecture Overview

```
market-compass/ (root)
├── pom.xml (parent - declares modules)
├── market-compass-core/
│   ├── pom.xml
│   └── src/ (existing investment app)
└── market-compass-llm/
    ├── pom.xml
    └── src/ (new LLM service)
```

## Module Descriptions

### market-compass-core
- **Purpose**: Investment portfolio tracker, brokerage agent, wallet
- **Port**: 8080
- **Key Endpoints**: `/api/wallet`, `/api/portfolio`, `/api/stocks`, `/api/agent`
- **Tech**: Spring Boot, in-memory data

### market-compass-llm
- **Purpose**: LLM-powered Q&A service for investment advice
- **Port**: 8081
- **Key Endpoints**: `/api/llm/qa`, `/api/llm/qa/contextual`
- **Tech**: Spring AI, Ollama, LLaMA2

## Running Both Services

### Option 1: Run from IDE (Recommended for development)

1. **Terminal 1 - Start Ollama:**
   ```bash
   ollama serve
   ```

2. **Terminal 2 - Start LLM Module:**
   - IntelliJ: Right-click `MarketCompassLLMApplication` → Run
   - Or: `cd market-compass-llm && mvn spring-boot:run`

3. **Terminal 3 - Start Core Module:**
   - IntelliJ: Right-click `MarketCompassApplication` → Run
   - Or: `cd market-compass-core && mvn spring-boot:run`

### Option 2: Run from Maven (Production-like)

```bash
# Build all modules
mvn clean package

# Run each JAR separately
java -jar market-compass-core/target/market-compass-core-1.0.0.jar
java -jar market-compass-llm/target/market-compass-llm-1.0.0.jar
```

### Option 3: Build and Run with Docker

See `Dockerfile` for containerization.

## Next Steps

### 1. Test LLM Module

```bash
# Check if LLM service is running
curl http://localhost:8081/api/llm/health

# Ask a question
curl -X POST http://localhost:8081/api/llm/qa \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the benefits of index funds?"}'
```

### 2. Integrate LLM with Core Module

Add dependency in `market-compass-core/pom.xml`:

```xml
<dependency>
    <groupId>com.marketcompass</groupId>
    <artifactId>market-compass-llm</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then create a client to call the LLM service:

```java
@Service
public class LLMClient {
    private final RestTemplate restTemplate = new RestTemplate();
    
    public String askQuestion(String question) {
        String url = "http://localhost:8081/api/llm/qa";
        QuestionRequest request = new QuestionRequest(question, "");
        ResponseEntity<AnswerResponse> response = restTemplate.postForEntity(url, request, AnswerResponse.class);
        return response.getBody().getAnswer();
    }
}
```

### 3. Create API Gateway (Optional)

Consider adding `market-compass-api-gateway` module to route requests:

```
8080 → API Gateway
       ├─ /api/wallet → core:8080
       ├─ /api/portfolio → core:8080
       └─ /api/llm → llm:8081
```

## Troubleshooting

### Port Conflicts

If ports are already in use:

```bash
# Change LLM port in market-compass-llm/src/main/resources/application.yml
server:
  port: 8082  # Change this
```

### Maven Build Issues

```bash
# Clean and rebuild
mvn clean install

# Skip tests
mvn clean package -DskipTests
```

### Ollama Connection Issues

Ensure Ollama is running:

```bash
# Start Ollama
ollama serve

# Test connection
curl http://localhost:11434/api/tags
```

## IDE Configuration

### IntelliJ

1. **File** → **Project Structure** → **Project**
2. Set **Project name**: `market-compass`
3. **Modules**: Should auto-detect both modules
4. **Build** → **Maven**: Set to `/mvnw` (if using Maven wrapper)

### VS Code

Install extensions:
- Extension Pack for Java
- Maven for Java
- Spring Boot Dashboard

## References

- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Spring AI Documentation](https://spring.io/projects/spring-ai)
- [Ollama GitHub](https://github.com/ollama/ollama)
