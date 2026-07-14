#!/usr/bin/env python3
"""
Contract Job Hunter — Ajit Pattepu
Uses Adzuna free developer API (works from GitHub Actions, no bot-blocking).

Setup (2 min):
  1. https://developer.adzuna.com → Sign up free → Create App
  2. Copy App ID and App Key
  3. GitHub repo → Settings → Secrets → Actions → add:
       ADZUNA_APP_ID   = your_app_id
       ADZUNA_APP_KEY  = your_app_key
       EMAIL_FROM      = pattepuajit23@gmail.com   (optional)
       EMAIL_TO        = pattepuajit23@gmail.com   (optional)
       EMAIL_PASSWORD  = gmail_app_password         (optional)

Free tier: 1000 req/month. This uses ~3 req/run x 6 runs/day = ~540/month.
"""

import csv
import json
import os
import time
import logging
import hashlib
import smtplib
from datetime import datetime
from email.mime.text import MIMEText
from pathlib import Path
import urllib.request
import urllib.parse

# ── Config ────────────────────────────────────────────────────────────────────

ADZUNA_APP_ID  = os.getenv("ADZUNA_APP_ID", "")
ADZUNA_APP_KEY = os.getenv("ADZUNA_APP_KEY", "")

KEYWORDS = [
    "Java Spring Boot contract",
    "Java microservices Kafka contract",
    "Java backend engineer contract",
]

# No location filter — search all USA, results include location column
RESULTS_PER_PAGE  = 50
PAGES_PER_KEYWORD = 4   # 50 x 4 = 200 results per keyword

OUTPUT_DIR  = Path(__file__).parent
OUTPUT_CSV  = OUTPUT_DIR / "contract_jobs.csv"
OUTPUT_JSON = OUTPUT_DIR / "contract_jobs.json"
SEEN_FILE   = OUTPUT_DIR / "seen_jobs.json"
LOG_FILE    = OUTPUT_DIR / "job_hunter.log"

EMAIL_FROM     = os.getenv("EMAIL_FROM", "")
EMAIL_TO       = os.getenv("EMAIL_TO", "")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD", "")

CSV_FIELDS = ["title", "company", "location", "salary", "type", "posted", "url", "source", "found_at"]

# ── Logging ───────────────────────────────────────────────────────────────────

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.FileHandler(LOG_FILE), logging.StreamHandler()],
)
log = logging.getLogger(__name__)

# ── State helpers ─────────────────────────────────────────────────────────────

def load_seen() -> set:
    if SEEN_FILE.exists():
        with open(SEEN_FILE) as f:
            return set(json.load(f))
    return set()

def save_seen(seen: set):
    with open(SEEN_FILE, "w") as f:
        json.dump(sorted(seen), f)

def job_id(url: str) -> str:
    return hashlib.md5(url.encode()).hexdigest()

def append_csv(jobs: list):
    write_header = not OUTPUT_CSV.exists()
    with open(OUTPUT_CSV, "a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        if write_header:
            writer.writeheader()  # always write header so file exists even if empty
        for job in jobs:
            writer.writerow({k: job.get(k, "") for k in CSV_FIELDS})

def append_json(jobs: list):
    existing = []
    if OUTPUT_JSON.exists():
        try:
            with open(OUTPUT_JSON) as f:
                existing = json.load(f)
        except Exception:
            existing = []
    with open(OUTPUT_JSON, "w") as f:
        json.dump(jobs + existing, f, indent=2)

# ── Adzuna API ────────────────────────────────────────────────────────────────

def search_adzuna(keyword: str, page: int = 1) -> list:
    if not ADZUNA_APP_ID or not ADZUNA_APP_KEY:
        log.error("ADZUNA_APP_ID / ADZUNA_APP_KEY not set. See script header.")
        return []

    params = {
        "app_id":           ADZUNA_APP_ID,
        "app_key":          ADZUNA_APP_KEY,
        "results_per_page": RESULTS_PER_PAGE,
        "what":             keyword,
        "contract":         1,
        "content-type":     "application/json",
    }
    url = f"https://api.adzuna.com/v1/api/jobs/us/search/{page}?{urllib.parse.urlencode(params)}"

    jobs = []
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "JobHunter/1.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode())

        for item in data.get("results", []):
            location_str = item.get("location", {}).get("display_name", "")

            s_min = item.get("salary_min")
            s_max = item.get("salary_max")
            salary = ""
            if s_min and s_max:
                salary = f"${int(s_min/1000)}k-${int(s_max/1000)}k"
            elif s_min:
                salary = f"${int(s_min/1000)}k+"

            jobs.append({
                "title":   item.get("title", ""),
                "company": item.get("company", {}).get("display_name", ""),
                "location": location_str,
                "salary":  salary,
                "type":    "Contract",
                "posted":  (item.get("created") or "")[:10],
                "url":     item.get("redirect_url", ""),
                "source":  "Adzuna",
            })
    except Exception as e:
        log.warning(f"  Adzuna error ('{keyword}' page {page}): {e}")
    return jobs

# ── Email ─────────────────────────────────────────────────────────────────────

def send_email(new_jobs: list):
    if not all([EMAIL_FROM, EMAIL_TO, EMAIL_PASSWORD]):
        return
    lines = [f"Found {len(new_jobs)} new Java contract jobs:\n"]
    for j in new_jobs:
        lines += [f"[{j['location']}] {j['title']} @ {j['company']}",
                  f"  Salary: {j['salary'] or 'Not listed'}",
                  f"  URL: {j['url']}\n"]
    msg = MIMEText("\n".join(lines))
    msg["Subject"] = f"[Job Hunter] {len(new_jobs)} new Java contract jobs"
    msg["From"] = EMAIL_FROM
    msg["To"]   = EMAIL_TO
    try:
        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as s:
            s.login(EMAIL_FROM, EMAIL_PASSWORD)
            s.send_message(msg)
        log.info(f"  Email sent to {EMAIL_TO}")
    except Exception as e:
        log.warning(f"  Email failed: {e}")

# ── Main ──────────────────────────────────────────────────────────────────────

def run_search():
    log.info("=" * 70)
    log.info(f"Search started — {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    log.info("Scope: All USA contract roles")

    if not ADZUNA_APP_ID:
        log.error("ADZUNA_APP_ID not set. Get free key at https://developer.adzuna.com")
        return

    seen = load_seen()
    new_jobs = []

    for keyword in KEYWORDS:
        log.info(f"  Searching: '{keyword}'")
        for page in range(1, PAGES_PER_KEYWORD + 1):
            batch = search_adzuna(keyword, page)
            log.info(f"    Page {page}: {len(batch)} jobs found")
            for job in batch:
                if not job.get("url"):
                    continue
                jid = job_id(job["url"])
                if jid not in seen:
                    seen.add(jid)
                    job["found_at"] = datetime.now().strftime("%Y-%m-%d %H:%M")
                    new_jobs.append(job)
            time.sleep(0.5)

    save_seen(seen)

    # Always write files so GitHub Actions artifact upload never fails
    append_csv(new_jobs)
    append_json(new_jobs)

    if not new_jobs:
        log.info("No new jobs this cycle.")
        return

    print(f"\n{'='*70}")
    print(f"  {len(new_jobs)} NEW CONTRACT JOBS  —  {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    print(f"{'='*70}")
    for j in sorted(new_jobs, key=lambda x: x["location"]):
        print(f"  {j['title'][:46]:<46}  {j['company'][:24]:<24}  {j['location']:<20}  {j['salary'] or '—'}")
    print(f"{'='*70}")
    print(f"  Saved -> {OUTPUT_CSV}\n")

    send_email(new_jobs)

if __name__ == "__main__":
    log.info("Contract Job Hunter (Adzuna API) started")
    run_search()
