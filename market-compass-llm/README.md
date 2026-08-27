# MarketCompass LLM Module

LLM-powered Q&A and interview-assistant service using Ollama/Groq, speech-to-text, and LLaMA-family models.

## Interview Assistant

The interview assistant is designed as a real-time candidate copilot rather than a generic answer generator.

### Reusable candidate profile

A resume is now a persistent candidate profile. Upload it once:

```bash
curl -X PUT http://localhost:8081/api/interview/profile/resume \
  -H 'Content-Type: application/json' \
  -d '{"resume":"YOUR RESUME TEXT"}'
```

Check whether a saved profile exists:

```bash
curl http://localhost:8081/api/interview/profile
```

Start every new interview with only the new job description:

```bash
curl -X POST http://localhost:8081/api/stt/setup \
  -H 'Content-Type: application/json' \
  -d '{"jobDescription":"NEW JOB DESCRIPTION"}'
```

You can still supply `resume` in `/api/stt/setup` when you intentionally want to replace the saved candidate profile.

### Live interview guidance

`/api/stt/answer-live` and `/api/stt/ask` now return:

- `questionType` — technical, experience, behavioral, follow-up, coding, or system design
- `topic` — current interview topic
- `likelyFollowUps` — likely next questions to prepare for
- `answer` — the candidate-facing answer generated using the saved candidate context

The session keeps the current JD and conversation history separate from the reusable resume.

### Persistence

By default the candidate profile is stored at:

```text
~/.marketcompass/candidate-profile.json
```

For Docker, set `INTERVIEW_CANDIDATE_PROFILE_PATH` to a path backed by a persistent volume.

## Existing LLM/STT APIs

### Ask a Question

```bash
POST /api/llm/qa
Content-Type: application/json

{"question":"What is a good dividend stock for beginners?"}
```

### Ask with Context

```bash
POST /api/llm/qa/contextual
Content-Type: application/json

{"question":"Should I buy more AAPL?","context":"Current portfolio: 100 shares of AAPL at $180."}
```

### Health Check

```bash
GET /api/llm/health
```

## Running

```bash
mvn spring-boot:run
```

The service runs on `http://localhost:8081` by default.
