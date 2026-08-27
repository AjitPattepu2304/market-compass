#!/usr/bin/env python3
"""Hyderabad + Bangalore full-time Job Hunter.

India only. Searches broadly for relevant Java/backend/software roles and
uses the preferred company list only to prioritize results, not to restrict
which companies can be returned.
"""
import csv
import hashlib
import json
import logging
import os
import smtplib
import time
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

# Broad discovery queries. Company names are deliberately NOT included here.
KEYWORDS = [
    "senior java developer",
    "senior java engineer",
    "senior software engineer java",
    "senior software developer java",
    "senior backend engineer java",
    "senior java backend developer",
    "java spring boot",
    "java spring boot microservices",
    "java microservices",
    "java kafka",
    "java spring kafka",
    "java aws microservices",
    "java kubernetes microservices",
    "java cloud backend",
    "java distributed systems",
    "software engineer backend java",
    "application engineer java",
    "application developer java",
    "technical lead java",
    "sde iii java",
    "software engineer iii java",
    "java full stack spring boot",
]

# These companies receive a ranking boost only. Any other company can be returned.
PREFERRED_COMPANIES = [
    "jp morgan", "jpmorgan", "j.p. morgan", "wells fargo", "goldman sachs",
    "morgan stanley", "bank of america", "state street", "vanguard", "apple",
    "microsoft", "amazon", "oracle", "salesforce", "servicenow", "adp",
    "highradius", "epam", "globallogic", "global logic", "cognizant", "ey",
    "deloitte", "accenture", "wipro", "tcs", "infosys", "hcltech", "capgemini",
    "persistent systems", "tech mahindra", "ltimindtree", "hexaware", "paypal",
    "uber", "expedia", "qualcomm", "amd", "broadcom", "sap", "siemens",
    "bosch", "hsbc", "citi", "american express", "s&p global", "factset",
    "thoughtworks", "zensar", "darwinbox", "infor", "servicenow", "walmart"
]

EXCLUDE_TITLES = [
    "frontend", "front-end", "angular developer", "react developer", "ios",
    "android", "qa engineer", "test engineer", "tester", "data engineer",
    "data scientist", "ml engineer", "machine learning", "devops engineer",
    "ui developer", "php", "ruby", ".net developer", "c# developer",
    "salesforce", "mainframe", "sap functional", "business analyst"
]

NON_FULL_TIME_TERMS = [
    "contract", "contractor", "c2c", "corp to corp", "corp-to-corp",
    "temporary", "temp to hire", "freelance", "part time", "part-time",
    "w2", "internship", "intern", "walk-in", "walk in"
]

MUST_HAVE = ["java"]
CORE_SKILLS = ["spring boot", "spring", "microservice", "backend", "rest api", "restful", "distributed"]
BONUS_SKILLS = [
    "kafka", "kubernetes", "k8s", "aws", "gcp", "cassandra", "postgresql",
    "mysql", "mongodb", "docker", "hibernate", "jpa", "oauth", "openapi",
    "ci/cd", "jenkins", "helm", "prometheus", "grafana", "react", "python"
]
SENIOR_TERMS = ["senior", "sr ", "sr.", "lead", "principal", "staff", "sde iii", "engineer iii"]
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
CSV_FIELDS = ["title", "company", "location", "salary", "type", "posted", "url", "source", "found_at", "score"]

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s",
                    handlers=[logging.FileHandler(LOG_FILE), logging.StreamHandler()])
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
        "app_id": ADZUNA_APP_ID, "app_key": ADZUNA_APP_KEY,
        "results_per_page": RESULTS_PER_PAGE, "what": keyword,
        "where": location, "sort_by": "date", "content-type": "application/json"
    }
    url = "https://api.adzuna.com/v1/api/jobs/in/search/{}?{}".format(page, urllib.parse.urlencode(params))
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "MarketCompass-JobHunter/4.0"})
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
                if datetime.fromisoformat(created.replace("Z", "+00:00")) < cutoff:
                    continue
            except ValueError:
                pass

        title = item.get("title", "")
        description = item.get("description") or ""
        company = item.get("company", {}).get("display_name", "")
        location_name = item.get("location", {}).get("display_name", "")
        link = item.get("redirect_url", "")
        text = f"{title} {description}".lower()
        loc = f"{location_name} {title} {description}".lower()

        if location == "Hyderabad":
            valid_location = any(x in loc for x in ["hyderabad", "secunderabad", "telangana"])
        else:
            valid_location = any(x in loc for x in ["bangalore", "bengaluru", "karnataka"])
        if not valid_location:
            continue

        # Hard exclusion: no contract/C2C/W2/temp/freelance/part-time/internship.
        raw_type = (item.get("contract_type") or "").lower()
        if any(term in text or term in raw_type for term in NON_FULL_TIME_TERMS):
            continue
        if raw_type and raw_type not in {"permanent", "full_time", "full-time"}:
            continue
        if not link:
            continue

        salary = ""
        smin, smax = item.get("salary_min"), item.get("salary_max")
        if smin and smax:
            salary = f"INR {int(smin):,}-{int(smax):,}"
        elif smin:
            salary = f"INR {int(smin):,}+"

        jobs.append({
            "title": title, "company": company, "location": location_name or location,
            "salary": salary, "type": "Full-time", "posted": created[:10],
            "url": link, "source": "Adzuna India", "description": description
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

    score = 20
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

    company = job.get("company", "").lower()
    if any(x in company for x in PREFERRED_COMPANIES):
        score += 10
    return score


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


def send_email(jobs):
    if not all([EMAIL_FROM, EMAIL_TO, EMAIL_PASSWORD]):
        return
    message = MIMEMultipart("alternative")
    message["Subject"] = f"[Job Hunter] {len(jobs)} new Hyderabad/Bangalore full-time Java jobs"
    message["From"], message["To"] = EMAIL_FROM, EMAIL_TO
    plain = f"Found {len(jobs)} relevant full-time jobs in Hyderabad/Bangalore.\n\n"
    for j in jobs:
        plain += f"[{j['score']}] {j['title']} | {j['company']} | {j['location']} | {j['posted']}\n{j['url']}\n\n"
    html = "<html><body><h2>Hyderabad + Bangalore Full-time Java Jobs</h2>"
    html += f"<p>{len(jobs)} new relevant jobs from the last {MAX_JOB_AGE_DAYS} days.</p><table border='1' cellpadding='6' cellspacing='0'>"
    html += "<tr><th>Score</th><th>Role</th><th>Company</th><th>Location</th><th>Posted</th><th>Apply</th></tr>"
    for j in jobs:
        html += f"<tr><td>{j['score']}</td><td>{j['title']}</td><td>{j['company']}</td><td>{j['location']}</td><td>{j['posted']}</td><td><a href='{j['url']}'>Apply</a></td></tr>"
    html += "</table></body></html>"
    message.attach(MIMEText(plain, "plain"))
    message.attach(MIMEText(html, "html"))
    try:
        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
            server.login(EMAIL_FROM, EMAIL_PASSWORD)
            server.send_message(message)
        log.info("Email sent to %s", EMAIL_TO)
    except Exception as exc:
        log.warning("Email failed: %s", exc)


def run_search():
    log.info("=" * 70)
    log.info("Search: Hyderabad + Bangalore | India | Full-time only | Last %s days", MAX_JOB_AGE_DAYS)
    seen = load_seen()
    new_jobs = []
    if not ADZUNA_APP_ID or not ADZUNA_APP_KEY:
        log.error("ADZUNA_APP_ID / ADZUNA_APP_KEY not set")
        return

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
                time.sleep(0.4)

    save_seen(seen)
    # Deduplicate the same job returned under multiple keyword searches.
    unique = {}
    for job in new_jobs:
        unique[job["url"]] = job
    new_jobs = list(unique.values())
    new_jobs.sort(key=lambda j: (-j["score"], j["posted"], j["company"].lower()))
    append_results(new_jobs)

    log.info("Found %s NEW relevant jobs", len(new_jobs))
    for job in new_jobs:
        log.info("[%s] %s | %s | %s", job["score"], job["title"], job["company"], job["location"])
    if new_jobs:
        send_email(new_jobs)


if __name__ == "__main__":
    run_search()
