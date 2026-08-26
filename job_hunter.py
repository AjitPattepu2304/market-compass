#!/usr/bin/env python3
"""
Hyderabad Full-time Job Hunter - Ajit Pattepu
Uses Adzuna free developer API from GitHub Actions.

Scope:
  - Hyderabad, Telangana, India only
  - Full-time / permanent roles only
  - Senior Java / Backend / Software Engineering roles
  - Tailored to Ajit's profile: Java, Spring Boot, Microservices, Kafka,
    distributed systems, AWS/GCP, Kubernetes, Docker, Cassandra/PostgreSQL,
    REST APIs and related backend technologies.

Setup:
  GitHub repo -> Settings -> Secrets -> Actions:
    ADZUNA_APP_ID
    ADZUNA_APP_KEY
    EMAIL_FROM       (optional)
    EMAIL_TO         (optional)
    EMAIL_PASSWORD   (optional Gmail app password)
"""

import csv
import json
import os
import re
import time
import logging
import hashlib
import smtplib
from datetime import datetime, timezone, timedelta
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from pathlib import Path
import urllib.request
import urllib.parse

# -----------------------------------------------------------------------------
# Config
# -----------------------------------------------------------------------------

ADZUNA_APP_ID = os.getenv("ADZUNA_APP_ID", "")
ADZUNA_APP_KEY = os.getenv("ADZUNA_APP_KEY", "")

# Job titles / technology combinations relevant to an 8-year Java backend profile.
KEYWORDS = [
    "senior java developer",
    "senior software engineer java",
    "senior java backend developer",
    "senior backend engineer java",
    "java spring boot microservices",
    "java backend engineer",
    "java microservices",
    "java kafka microservices",
    "java spring boot aws",
    "java spring boot kubernetes",
    "java software engineer",
    "software engineer java spring boot",
    "software developer java spring",
    "application engineer java",
    "application developer java",
    "java technical lead",
    "sde 3 java backend",
    "senior application developer java",
]

# Hyderabad-specific searches. Adzuna's India endpoint receives the location
# filter separately; keeping these broad improves recall while the location and
# full-time filters below enforce the actual scope.
LOCATION_QUERY = "Hyderabad"

# Keep the vendor list focused on companies that commonly recruit this profile.
TARGET_COMPANIES = [
    "jp morgan", "jpmorgan", "j.p. morgan",
    "wells fargo",
    "goldman sachs",
    "morgan stanley",
    "bank of america",
    "state street",
    "vanguard",
    "apple",
    "microsoft",
    "amazon",
    "oracle",
    "salesforce",
    "servicenow",
    "adp",
    "highradius",
    "epam",
    "globallogic",
    "global logic",
    "cognizant",
    "ey",
    "deloitte",
    "accenture",
    "wipro",
    "tcs",
    "infosys",
    "hcltech",
    "capgemini",
    "persistent systems",
    "tech mahindra",
    "ltimindtree",
    "hexaware",
]

RESULTS_PER_PAGE = 50
PAGES_PER_KEYWORD = 2
MAX_JOB_AGE_DAYS = 7

OUTPUT_DIR = Path(__file__).parent
OUTPUT_CSV = OUTPUT_DIR / "hyderabad_fulltime_jobs.csv"
OUTPUT_JSON = OUTPUT_DIR / "hyderabad_fulltime_jobs.json"
SEEN_FILE = OUTPUT_DIR / "seen_jobs.json"
LOG_FILE = OUTPUT_DIR / "job_hunter.log"

EMAIL_FROM = os.getenv("EMAIL_FROM", "")
EMAIL_TO = os.getenv("EMAIL_TO", "")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD", "")

CSV_FIELDS = [
    "title", "company", "location", "salary", "type", "posted",
    "url", "adzuna_url", "source", "found_at"
]

# -----------------------------------------------------------------------------
# Logging
# -----------------------------------------------------------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.FileHandler(LOG_FILE), logging.StreamHandler()],
)
log = logging.getLogger(__name__)

# -----------------------------------------------------------------------------
# State helpers
# -----------------------------------------------------------------------------


def load_seen() -> set:
    if SEEN_FILE.exists():
        try:
            with open(SEEN_FILE, encoding="utf-8") as f:
                return set(json.load(f))
        except Exception:
            log.warning("Could not read seen_jobs.json; starting with empty state")
    return set()


def save_seen(seen: set):
    with open(SEEN_FILE, "w", encoding="utf-8") as f:
        json.dump(sorted(seen), f)


def job_id(url: str) -> str:
    return hashlib.md5(url.encode("utf-8")).hexdigest()


def append_csv(jobs: list):
    if not jobs:
        return
    write_header = not OUTPUT_CSV.exists()
    with open(OUTPUT_CSV, "a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        if write_header:
            writer.writeheader()
        for job in jobs:
            writer.writerow({k: job.get(k, "") for k in CSV_FIELDS})


def append_json(jobs: list):
    if not jobs:
        return
    existing = []
    if OUTPUT_JSON.exists():
        try:
            with open(OUTPUT_JSON, encoding="utf-8") as f:
                existing = json.load(f)
        except Exception:
            existing = []
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(jobs + existing, f, indent=2)

# -----------------------------------------------------------------------------
# Adzuna API
# -----------------------------------------------------------------------------


def search_adzuna(keyword: str, page: int = 1) -> list:
    if not ADZUNA_APP_ID or not ADZUNA_APP_KEY:
        log.error("ADZUNA_APP_ID / ADZUNA_APP_KEY not set")
        return []

    params = {
        "app_id": ADZUNA_APP_ID,
        "app_key": ADZUNA_APP_KEY,
        "results_per_page": RESULTS_PER_PAGE,
        "what": keyword,
        "where": LOCATION_QUERY,
        "sort_by": "date",
        "content-type": "application/json",
    }
    # India endpoint: https://api.adzuna.com/v1/api/jobs/in/search/{page}
    url = f"https://api.adzuna.com/v1/api/jobs/in/search/{page}?{urllib.parse.urlencode(params)}"

    jobs = []
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "JobHunter/2.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode("utf-8"))

        cutoff = datetime.now(timezone.utc) - timedelta(days=MAX_JOB_AGE_DAYS)

        for item in data.get("results", []):
            created_str = item.get("created", "")
            if created_str:
                try:
                    created_dt = datetime.fromisoformat(created_str.replace("Z", "+00:00"))
                    if created_dt < cutoff:
                        continue
                except Exception:
                    pass

            location_str = item.get("location", {}).get("display_name", "")
            title = item.get("title", "")
            description = item.get("description") or ""
            adzuna_link = item.get("redirect_url", "")

            # Enforce Hyderabad. Adzuna may return nearby locations for a broad
            # city query, so require Hyderabad / Secunderabad in the final text.
            location_text = f"{location_str} {title} {description}".lower()
            if not any(x in location_text for x in [
                "hyderabad", "secunderabad", "telangana"
            ]):
                continue

            # Exclude obvious aggregators / low quality reposts.
            skip_signals = [
                "dice.com", "appcast", "jobs2careers", "ziprecruiter.com",
                "apply on dice", "view on dice"
            ]
            if any(s in description.lower() or s in adzuna_link.lower() for s in skip_signals):
                continue

            raw_type = (item.get("contract_type") or "").lower()
            raw_category = (item.get("category", {}).get("label") or "").lower()
            title_lower = title.lower()
            text_lower = f"{title} {description}".lower()

            # FULL-TIME ONLY. Adzuna sometimes leaves contract_type blank, so
            # accept blank/unknown only when there are clear permanent signals.
            is_contract = any(x in f"{raw_type} {title_lower} {text_lower}" for x in [
                "contract", "contractor", "c2c", "corp to corp", "temporary", "temp to hire",
                "freelance", "part time", "part-time", "w2"
            ])
            is_full_time = (
                "full time" in text_lower
                or "full-time" in text_lower
                or "permanent" in text_lower
                or raw_type in {"permanent", "full_time", "full-time"}
            )

            if is_contract:
                continue
            if raw_type and raw_type not in {"permanent", "full_time", "full-time"} and not is_full_time:
                continue

            # Exclude obviously non-job / irrelevant titles before scoring.
            company_name = item.get("company", {}).get("display_name", "")
            salary = ""
            s_min = item.get("salary_min")
            s_max = item.get("salary_max")
            if s_min and s_max:
                salary = f"INR {int(s_min):,}-{int(s_max):,}"
            elif s_min:
                salary = f"INR {int(s_min):,}+"

            jobs.append({
                "title": title,
                "company": company_name,
                "location": location_str,
                "salary": salary,
                "type": "Full-time",
                "posted": (item.get("created") or "")[:10],
                "url": adzuna_link,
                "adzuna_url": adzuna_link,
                "source": "Adzuna",
                "description": description,
                "category": raw_category,
            })
    except Exception as e:
        log.warning(f"Adzuna error ('{keyword}' page {page}): {e}")

    return jobs

# -----------------------------------------------------------------------------
# Relevance filter
# -----------------------------------------------------------------------------

MUST_HAVE = ["java"]
CORE_SKILLS = [
    "spring boot", "spring", "microservice", "microservices",
    "backend", "distributed", "rest api", "restful", "api",
]
BONUS_SKILLS = [
    "kafka", "kubernetes", "k8s", "aws", "gcp", "cassandra", "postgresql",
    "mysql", "mongodb", "docker", "hibernate", "jpa", "oauth", "openapi",
    "ci/cd", "jenkins", "helm", "prometheus", "grafana", "react", "python",
]
EXCLUDE_TITLES = [
    "frontend", "front-end", "angular developer", "react developer",
    "ios", "android", "qa engineer", "test engineer", "qa tester",
    "data engineer", "data scientist", "ml engineer", "machine learning",
    "devops engineer", "ui developer", "php", "ruby", ".net developer",
    "c# developer", "salesforce", "mainframe", "sap functional", "business analyst",
]


# Job-title patterns that are a strong fit for your 8-year profile.
SENIOR_TITLE_TERMS = [
    "senior", "sr ", "sr.", "lead", "principal", "staff", "sde iii",
    "engineer iii", "application engineer iii", "technical lead"
]

BACKEND_TITLE_TERMS = [
    "java developer", "java engineer", "software engineer", "software developer",
    "backend", "application developer", "application engineer", "sde"
]


def relevance_score(job: dict) -> int:
    title = (job.get("title", "") or "").lower()
    description = (job.get("description", "") or "").lower()
    text = f"{title} {description}"

    if any(x in title for x in EXCLUDE_TITLES):
        return 0
    if not any(x in text for x in MUST_HAVE):
        return 0
    if not any(x in title for x in BACKEND_TITLE_TERMS):
        return 0

    score = 15

    for skill in CORE_SKILLS:
        if skill in text:
            score += 5

    for skill in BONUS_SKILLS:
        if skill in text:
            score += 3

    if any(x in title for x in SENIOR_TITLE_TERMS):
        score += 10

    if "spring boot" in text:
        score += 8
    if "kafka" in text:
        score += 5
    if "microservices" in text or "microservice" in text:
        score += 5
    if "aws" in text or "gcp" in text:
        score += 4
    if "kubernetes" in text or "k8s" in text:
        score += 4

    if any(x in text for x in ["8 years", "7 years", "6 years", "8+ years", "7+ years"]):
        score += 5

    if job.get("salary"):
        score += 2

    return score


def is_target_company(job: dict) -> bool:
    company = (job.get("company", "") or "").lower()
    return any(x in company for x in TARGET_COMPANIES)


def filter_relevant(jobs: list, min_score: int = 30) -> list:
    scored = [(relevance_score(j), j) for j in jobs]
    relevant = [(score, job) for score, job in scored if score >= min_score]
    relevant.sort(key=lambda x: (-x[0], x[1].get("posted", "")), reverse=False)
    return [job for _, job in relevant]

# -----------------------------------------------------------------------------
# Email
# -----------------------------------------------------------------------------


def build_html_email(new_jobs: list) -> str:
    target_jobs = [j for j in new_jobs if is_target_company(j)]

    def job_rows(jobs):
        rows = ""
        for j in jobs:
            rows += f"""
            <tr style="border-bottom:1px solid #334155">
              <td style="padding:10px 8px">
                <div style="font-weight:600;color:#e2e8f0">{j['title']}</div>
                <div style="color:#94a3b8;font-size:12px">{j['company']}</div>
              </td>
              <td style="padding:10px 8px;color:#94a3b8;font-size:13px">{j['location']}</td>
              <td style="padding:10px 8px;color:#34d399;font-size:13px">{j['salary'] or 'Not listed'}</td>
              <td style="padding:10px 8px;color:#64748b;font-size:12px">{j['posted']}</td>
              <td style="padding:10px 8px">
                <a href="{j['url']}" style="background:#2563eb;color:#fff;padding:6px 14px;border-radius:6px;text-decoration:none;font-size:12px;font-weight:bold">Apply</a>
              </td>
            </tr>"""
        return rows

    def section(title, jobs):
        if not jobs:
            return ""
        return f"""
        <h3 style="color:#60a5fa;margin:24px 0 8px">{title} ({len(jobs)})</h3>
        <table style="width:100%;border-collapse:collapse;background:#1e293b;border-radius:8px;overflow:hidden">
          <thead>
            <tr style="background:#0f172a">
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">ROLE / COMPANY</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">LOCATION</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">SALARY</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">POSTED</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">APPLY</th>
            </tr>
          </thead>
          <tbody>{job_rows(jobs)}</tbody>
        </table>"""

    return f"""
    <html><body style="background:#0f172a;color:#e2e8f0;font-family:system-ui,sans-serif;padding:24px;margin:0">
      <div style="max-width:1000px;margin:0 auto">
        <h2 style="color:#34d399;margin-bottom:4px">Hyderabad Full-time Java Jobs</h2>
        <p style="color:#64748b;margin:0 0 20px">{len(new_jobs)} relevant new roles — last {MAX_JOB_AGE_DAYS} days</p>
        {section("Priority companies", target_jobs)}
        {section("Other strong matches", [j for j in new_jobs if j not in target_jobs])}
        <p style="color:#475569;font-size:11px;margin-top:24px">Powered by MarketCompass Job Hunter · Adzuna India API</p>
      </div>
    </body></html>"""


def send_email(new_jobs: list):
    if not all([EMAIL_FROM, EMAIL_TO, EMAIL_PASSWORD]):
        return

    msg = MIMEMultipart("alternative")
    msg["Subject"] = f"[Job Hunter] {len(new_jobs)} Hyderabad full-time Java jobs"
    msg["From"] = EMAIL_FROM
    msg["To"] = EMAIL_TO

    plain = f"Found {len(new_jobs)} relevant Hyderabad full-time Java jobs.\n\n"
    for j in new_jobs:
        plain += (
            f"{j['title']} @ {j['company']} | {j['location']} | "
            f"{j['posted']} | {j['salary'] or 'N/A'}\n{j['url']}\n\n"
        )

    msg.attach(MIMEText(plain, "plain"))
    msg.attach(MIMEText(build_html_email(new_jobs), "html"))

    try:
        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
            server.login(EMAIL_FROM, EMAIL_PASSWORD)
            server.send_message(msg)
        log.info(f"Email sent to {EMAIL_TO}")
    except Exception as e:
        log.warning(f"Email failed: {e}")

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------


def run_search():
    log.info("=" * 70)
    log.info(f"Search started - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    log.info(
        f"Scope: Hyderabad, Telangana | Full-time only | Last {MAX_JOB_AGE_DAYS} days | "
        "Senior Java/backend/software engineering"
    )

    if not ADZUNA_APP_ID or not ADZUNA_APP_KEY:
        log.error("ADZUNA_APP_ID / ADZUNA_APP_KEY not set")
        return

    seen = load_seen()
    new_jobs = []

    for keyword in KEYWORDS:
        log.info(f"Keyword: '{keyword}'")
        for page in range(1, PAGES_PER_KEYWORD + 1):
            batch = search_adzuna(keyword, page)
            log.info(f"  Page {page}: {len(batch)} Hyderabad full-time candidates")
            for job in batch:
                url = job.get("url")
                if not url:
                    continue
                jid = job_id(url)
                if jid in seen:
                    continue
                seen.add(jid)
                job["found_at"] = datetime.now().strftime("%Y-%m-%d %H:%M")
                new_jobs.append(job)
            time.sleep(0.5)

    save_seen(seen)

    relevant_jobs = filter_relevant(new_jobs)
    log.info(f"Relevance filter: {len(new_jobs)} new -> {len(relevant_jobs)} relevant")

    append_csv(relevant_jobs)
    append_json(relevant_jobs)

    if not relevant_jobs:
        log.info("No relevant new Hyderabad full-time jobs this cycle.")
        return

    print("\n" + "=" * 80)
    print(
        f" {len(relevant_jobs)} RELEVANT HYDERABAD FULL-TIME JOBS "
        f"- last {MAX_JOB_AGE_DAYS} days"
    )
    print("=" * 80)

    for job in relevant_jobs:
        print(
            f" [{job['posted']}] {job['title'][:55]:<55} "
            f"{job['company'][:28]:<28}"
        )
        print(f"   {job['url']}")

    print("=" * 80)
    print(f"Saved -> {OUTPUT_CSV}")
    send_email(relevant_jobs)


if __name__ == "__main__":
    log.info("Hyderabad Full-time Job Hunter started")
    run_search()
