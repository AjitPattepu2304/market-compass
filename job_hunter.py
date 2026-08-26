#!/usr/bin/env python3
"""Hyderabad + Bangalore full-time Job Hunter for Ajit Pattepu.

Search scope:
- India only
- Hyderabad and Bangalore only
- Full-time / permanent only
- Recent jobs (last 7 days)
- Senior Java / backend / software engineering roles
"""

import csv
import hashlib
import json
import logging
import os
import smtplib
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from pathlib import Path

ADZUNA_APP_ID = os.getenv("ADZUNA_APP_ID", "")
ADZUNA_APP_KEY = os.getenv("ADZUNA_APP_KEY", "")

LOCATIONS = ["Hyderabad", "Bangalore"]
MAX_JOB_AGE_DAYS = 7
RESULTS_PER_PAGE = 50
PAGES_PER_KEYWORD = 2

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

TARGET_COMPANIES = [
    "jp morgan", "jpmorgan", "j.p. morgan", "wells fargo", "goldman sachs",
    "morgan stanley", "bank of america", "state street", "vanguard", "apple",
    "microsoft", "amazon", "oracle", "salesforce", "servicenow", "adp",
    "highradius", "epam", "globallogic", "global logic", "cognizant", "ey",
    "deloitte", "accenture", "wipro", "tcs", "infosys", "hcltech", "capgemini",
    "persistent systems", "tech mahindra", "ltimindtree", "hexaware"
]

EXCLUDE_TITLES = [
    "frontend", "front-end", "angular developer", "react developer", "ios",
    "android", "qa engineer", "test engineer", "data engineer", "data scientist",
    "ml engineer", "machine learning", "devops engineer", "ui developer", "php",
    "ruby", ".net developer", "c# developer", "salesforce", "mainframe",
    "sap functional", "business analyst"
]

MUST_HAVE = ["java"]
CORE_SKILLS = [
    "spring boot", "spring", "microservice", "microservices", "backend",
    "distributed", "rest api", "restful", "api"
]
BONUS_SKILLS = [
    "kafka", "kubernetes", "k8s", "aws", "gcp", "cassandra", "postgresql",
    "mysql", "mongodb", "docker", "hibernate", "jpa", "oauth", "openapi",
    "ci/cd", "jenkins", "helm", "prometheus", "grafana", "react", "python"
]
SENIOR_TERMS = [
    "senior", "sr ", "sr.", "lead", "principal", "staff", "sde iii",
    "engineer iii", "application engineer iii", "technical lead"
]
BACKEND_TITLE_TERMS = [
    "java developer", "java engineer", "software engineer", "software developer",
    "backend", "application developer", "application engineer", "sde"
]

OUTPUT_DIR = Path(__file__).parent
OUTPUT_CSV = OUTPUT_DIR / "india_fulltime_jobs.csv"
OUTPUT_JSON = OUTPUT_DIR / "india_fulltime_jobs.json"
SEEN_FILE = OUTPUT_DIR / "seen_jobs.json"
LOG_FILE = OUTPUT_DIR / "job_hunter.log"

EMAIL_FROM = os.getenv("EMAIL_FROM", "")
EMAIL_TO = os.getenv("EMAIL_TO", "")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD", "")

CSV_FIELDS = [
    "title", "company", "location", "salary", "type", "posted", "url",
    "source", "found_at", "score"
]

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.FileHandler(LOG_FILE), logging.StreamHandler()],
)
log = logging.getLogger(__name__)


def load_seen():
    if not SEEN_FILE.exists():
        return set()
    try:
        with open(SEEN_FILE, encoding="utf-8") as f:
            return set(json.load(f))
    except Exception:
        return set()


def save_seen(seen):
    with open(SEEN_FILE, "w", encoding="utf-8") as f:
        json.dump(sorted(seen), f)


def job_id(url):
    return hashlib.md5(url.encode("utf-8")).hexdigest()


def search_adzuna(keyword, location, page=1):
    if not ADZUNA_APP_ID or not ADZUNA_APP_KEY:
        log.error("ADZUNA_APP_ID / ADZUNA_APP_KEY not set")
        return []

    params = {
        "app_id": ADZUNA_APP_ID,
        "app_key": ADZUNA_APP_KEY,
        "results_per_page": RESULTS_PER_PAGE,
        "what": keyword,
        "where": location,
        "sort_by": "date",
        "content-type": "application/json",
    }
    url = "https://api.adzuna.com/v1/api/jobs/in/search/{}?{}".format(
        page, urllib.parse.urlencode(params)
    )

    try:
        req = urllib.request.Request(url, headers={"User-Agent": "MarketCompass-JobHunter/3.0"})
        with urllib.request.urlopen(req, timeout=20) as response:
            data = json.loads(response.read().decode("utf-8"))
    except Exception as exc:
        log.warning("Adzuna error [%s / %s / page %s]: %s", location, keyword, page, exc)
        return []

    cutoff = datetime.now(timezone.utc) - timedelta(days=MAX_JOB_AGE_DAYS)
    jobs = []

    for item in data.get("results", []):
        created = item.get("created", "")
        if created:
            try:
                created_dt = datetime.fromisoformat(created.replace("Z", "+00:00"))
                if created_dt < cutoff:
                    continue
            except ValueError:
                pass

        title = item.get("title", "")
        description = item.get("description") or ""
        company = item.get("company", {}).get("display_name", "")
        location_name = item.get("location", {}).get("display_name", "")
        redirect_url = item.get("redirect_url", "")
        text = f"{title} {description}".lower()

        # Location must actually be Hyderabad or Bangalore/Bengaluru.
        location_text = f"{location_name} {title} {description}".lower()
        if location == "Hyderabad":
            valid_location = any(x in location_text for x in ["hyderabad", "secunderabad", "telangana"])
        else:
            valid_location = any(x in location_text for x in ["bangalore", "bengaluru", "karnataka"])
        if not valid_location:
            continue

        # HARD full-time-only filter. Reject every explicit non-permanent signal.
        raw_type = (item.get("contract_type") or "").lower()
        non_full_time = [
            "contract", "contractor", "c2c", "corp to corp", "temporary",
            "temp to hire", "freelance", "part time", "part-time", "w2",
            "internship", "intern", "consultant"
        ]
        if any(term in text or term in raw_type for term in non_full_time):
            continue

        # If Adzuna explicitly classifies the role, accept only permanent/full-time.
        if raw_type and raw_type not in {"permanent", "full_time", "full-time"}:
            continue

        if not redirect_url:
            continue

        salary = ""
        salary_min = item.get("salary_min")
        salary_max = item.get("salary_max")
        if salary_min and salary_max:
            salary = f"INR {int(salary_min):,}-{int(salary_max):,}"
        elif salary_min:
            salary = f"INR {int(salary_min):,}+"

        jobs.append({
            "title": title,
            "company": company,
            "location": location_name or location,
            "salary": salary,
            "type": "Full-time",
            "posted": created[:10],
            "url": redirect_url,
            "source": "Adzuna India",
            "description": description,
        })

    return jobs


def relevance_score(job):
    title = job.get("title", "").lower()
    text = f"{title} {job.get('description', '')}".lower()

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
    if any(x in title for x in SENIOR_TERMS):
        score += 10
    if "spring boot" in text:
        score += 8
    if "kafka" in text:
        score += 5
    if "microservice" in text:
        score += 5
    if "aws" in text or "gcp" in text:
        score += 4
    if "kubernetes" in text or "k8s" in text:
        score += 4
    return score


def is_target_company(job):
    company = job.get("company", "").lower()
    return any(name in company for name in TARGET_COMPANIES)


def append_results(jobs):
    if not jobs:
        return
    write_header = not OUTPUT_CSV.exists()
    with open(OUTPUT_CSV, "a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        if write_header:
            writer.writeheader()
        for job in jobs:
            writer.writerow({field: job.get(field, "") for field in CSV_FIELDS})

    existing = []
    if OUTPUT_JSON.exists():
        try:
            with open(OUTPUT_JSON, encoding="utf-8") as f:
                existing = json.load(f)
        except Exception:
            pass
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(jobs + existing, f, indent=2)


def build_email(jobs):
    priority = [j for j in jobs if is_target_company(j)]
    other = [j for j in jobs if j not in priority]

    def rows(items):
        html = ""
        for job in items:
            html += (
                "<tr style='border-bottom:1px solid #334155'>"
                f"<td style='padding:9px'>{job['title']}<br><small>{job['company']}</small></td>"
                f"<td style='padding:9px'>{job['location']}</td>"
                f"<td style='padding:9px'>{job['posted']}</td>"
                f"<td style='padding:9px'><a href='{job['url']}'>Apply</a></td>"
                "</tr>"
            )
        return html

    def section(title, items):
        if not items:
            return ""
        return (
            f"<h3>{title} ({len(items)})</h3>"
            "<table style='width:100%;border-collapse:collapse'>"
            "<tr><th align='left'>Role / Company</th><th align='left'>Location</th>"
            "<th align='left'>Posted</th><th align='left'>Apply</th></tr>"
            f"{rows(items)}</table>"
        )

    return (
        "<html><body style='font-family:Arial'>"
        f"<h2>Hyderabad + Bangalore Full-time Java Jobs</h2>"
        f"<p>{len(jobs)} new relevant jobs from the last {MAX_JOB_AGE_DAYS} days.</p>"
        f"{section('Priority companies', priority)}"
        f"{section('Other strong matches', other)}"
        "</body></html>"
    )


def send_email(jobs):
    if not all([EMAIL_FROM, EMAIL_TO, EMAIL_PASSWORD]):
        return

    message = MIMEMultipart("alternative")
    message["Subject"] = f"[Job Hunter] {len(jobs)} new Hyderabad/Bangalore full-time Java jobs"
    message["From"] = EMAIL_FROM
    message["To"] = EMAIL_TO

    plain = f"Found {len(jobs)} relevant full-time jobs in Hyderabad/Bangalore.\n\n"
    for job in jobs:
        plain += f"{job['title']} | {job['company']} | {job['location']} | {job['posted']}\n{job['url']}\n\n"

    message.attach(MIMEText(plain, "plain"))
    message.attach(MIMEText(build_email(jobs), "html"))

    try:
        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
            server.login(EMAIL_FROM, EMAIL_PASSWORD)
            server.send_message(message)
        log.info("Email sent to %s", EMAIL_TO)
    except Exception as exc:
        log.warning("Email failed: %s", exc)


def run_search():
    log.info("=" * 70)
    log.info(
        "Search scope: Hyderabad + Bangalore | India | Full-time only | Last %s days",
        MAX_JOB_AGE_DAYS,
    )

    if not ADZUNA_APP_ID or not ADZUNA_APP_KEY:
        log.error("ADZUNA_APP_ID / ADZUNA_APP_KEY not set")
        return

    seen = load_seen()
    new_jobs = []

    for location in LOCATIONS:
        for keyword in KEYWORDS:
            for page in range(1, PAGES_PER_KEYWORD + 1):
                batch = search_adzuna(keyword, location, page)
                log.info("%s | %s | page %s -> %s candidates", location, keyword, page, len(batch))
                for job in batch:
                    jid = job_id(job["url"])
                    if jid in seen:
                        continue
                    seen.add(jid)
                    job["found_at"] = datetime.now().strftime("%Y-%m-%d %H:%M")
                    score = relevance_score(job)
                    if score >= 30:
                        job["score"] = score
                        new_jobs.append(job)
                # Keep API usage reasonable.
                import time
                time.sleep(0.4)

    save_seen(seen)
    new_jobs.sort(key=lambda job: (-job["score"], job["posted"]), reverse=False)
    append_results(new_jobs)

    log.info("Found %s new relevant full-time jobs", len(new_jobs))
    for job in new_jobs:
        log.info("[%s] %s | %s | %s", job["score"], job["title"], job["company"], job["location"])

    if new_jobs:
        send_email(new_jobs)


if __name__ == "__main__":
    run_search()
