#!/usr/bin/env python3
"""
Contract Job Hunter — Ajit Pattepu
Searches Dice, Indeed, LinkedIn, ZipRecruiter for Java/Spring Boot/Kafka
contract roles in NC, Atlanta, Dallas, Virginia, PA, NJ.

Zero extra installs needed — uses only:
  - requests        (already installed)
  - beautifulsoup4  (already installed)
  - Python stdlib (csv, json, time, logging, smtplib)

Run:
    python3 /Users/vn552f7/Dev/job_hunter.py

Output:
    ~/Dev/contract_jobs.csv   — all jobs found (opens in Excel)
    ~/Dev/contract_jobs.json  — same data as JSON
    ~/Dev/job_hunter.log      — run history

Optional email alerts (Gmail App Password):
    export EMAIL_FROM="pattepuajit23@gmail.com"
    export EMAIL_TO="pattepuajit23@gmail.com"
    export EMAIL_PASSWORD="xxxx xxxx xxxx xxxx"
    python3 job_hunter.py
"""

import csv
import json
import time
import logging
import hashlib
import os
import smtplib
from datetime import datetime
from email.mime.text import MIMEText
from pathlib import Path

import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)  # suppress SSL warnings
import requests
from bs4 import BeautifulSoup

# ── Config ────────────────────────────────────────────────────────────────────

KEYWORDS = [
    "Java Spring Boot contract",
    "Java microservices contract",
    "Java Kafka distributed systems contract",
    "Java backend engineer contract",
    "Spring Boot Kubernetes contract",
]

LOCATIONS = [
    "Charlotte, NC",
    "Raleigh, NC",
    "Atlanta, GA",
    "Dallas, TX",
    "Plano, TX",
    "Reston, VA",
    "McLean, VA",
    "Richmond, VA",
    "Philadelphia, PA",
    "Pittsburgh, PA",
    "Edison, NJ",
    "Jersey City, NJ",
    "Princeton, NJ",
]

INTERVAL_HOURS = 1    # how often to search

OUTPUT_DIR  = Path(__file__).parent
OUTPUT_CSV  = OUTPUT_DIR / "contract_jobs.csv"
OUTPUT_JSON = OUTPUT_DIR / "contract_jobs.json"
SEEN_FILE   = OUTPUT_DIR / "seen_jobs.json"
LOG_FILE    = OUTPUT_DIR / "job_hunter.log"

EMAIL_FROM     = os.getenv("EMAIL_FROM", "")
EMAIL_TO       = os.getenv("EMAIL_TO", "")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD", "")

CSV_FIELDS = ["title", "company", "location", "type", "posted", "url", "source", "found_at"]

# ── Logging ───────────────────────────────────────────────────────────────────

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.FileHandler(LOG_FILE),
        logging.StreamHandler(),
    ],
)
log = logging.getLogger(__name__)

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/json,*/*;q=0.9",
    "Accept-Language": "en-US,en;q=0.9",
}

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
            writer.writeheader()
        for job in jobs:
            writer.writerow({k: job.get(k, "") for k in CSV_FIELDS})


def append_json(jobs: list):
    existing = []
    if OUTPUT_JSON.exists():
        with open(OUTPUT_JSON) as f:
            existing = json.load(f)
    with open(OUTPUT_JSON, "w") as f:
        json.dump(jobs + existing, f, indent=2)


# ── Source: Dice.com (JSON API) ───────────────────────────────────────────────

def search_dice(keyword: str, location: str) -> list:
    """Dice has a public REST API — most reliable source for tech contract roles."""
    url = "https://job-search-api.svc.dhigroupinc.com/v1/dice/jobs/search"
    params = {
        "q":                      keyword,
        "location":               location,
        "radius":                 "30",
        "radiusUnit":             "mi",
        "page":                   1,
        "pageSize":               20,
        "filters.employmentType": "CONTRACTS",
        "filters.postedDate":     "ONE_DAY",
        "language":               "en",
    }
    jobs = []
    try:
        r = requests.get(url, params=params, headers=HEADERS, timeout=12, verify=False)
        r.raise_for_status()
        for item in r.json().get("data", []):
            jobs.append({
                "title":    item.get("title", ""),
                "company":  item.get("company", ""),
                "location": item.get("location", location),
                "type":     "Contract",
                "posted":   (item.get("postedDate") or "")[:10],
                "url":      f"https://www.dice.com/jobs/detail/{item.get('id', '')}",
                "source":   "Dice",
            })
    except Exception as e:
        log.warning(f"  Dice ({location}): {e}")
    return jobs


# ── Source: Indeed ────────────────────────────────────────────────────────────

def search_indeed(keyword: str, location: str) -> list:
    url = "https://www.indeed.com/jobs"
    params = {"q": keyword, "l": location, "jt": "contract", "fromage": 1, "limit": 25}
    jobs = []
    try:
        r = requests.get(url, params=params, headers=HEADERS, timeout=12, verify=False)
        r.raise_for_status()
        soup = BeautifulSoup(r.text, "html.parser")
        for card in soup.select('[class*="job_seen_beacon"]'):
            title_el   = card.select_one("h2.jobTitle span")
            company_el = card.select_one('[data-testid="company-name"]')
            loc_el     = card.select_one('[data-testid="text-location"]')
            link_el    = card.select_one("h2.jobTitle a")
            title    = title_el.get_text(strip=True)   if title_el   else ""
            company  = company_el.get_text(strip=True) if company_el else ""
            loc      = loc_el.get_text(strip=True)     if loc_el     else location
            href     = link_el.get("href", "")         if link_el    else ""
            full_url = f"https://www.indeed.com{href}" if href.startswith("/") else href
            if title and full_url:
                jobs.append({"title": title, "company": company, "location": loc,
                              "type": "Contract", "posted": today(), "url": full_url, "source": "Indeed"})
    except Exception as e:
        log.warning(f"  Indeed ({location}): {e}")
    return jobs


# ── Source: LinkedIn ──────────────────────────────────────────────────────────

def search_linkedin(keyword: str, location: str) -> list:
    url = "https://www.linkedin.com/jobs/search/"
    params = {"keywords": keyword, "location": location, "f_JT": "C", "f_TPR": "r86400", "start": 0}
    jobs = []
    try:
        r = requests.get(url, params=params, headers=HEADERS, timeout=12, verify=False)
        r.raise_for_status()
        soup = BeautifulSoup(r.text, "html.parser")
        for card in soup.select("ul.jobs-search__results-list > li"):
            title_el   = card.select_one(".base-search-card__title")
            company_el = card.select_one(".base-search-card__subtitle")
            loc_el     = card.select_one(".job-search-card__location")
            link_el    = card.select_one("a.base-card__full-link")
            title   = title_el.get_text(strip=True)   if title_el   else ""
            company = company_el.get_text(strip=True) if company_el else ""
            loc     = loc_el.get_text(strip=True)     if loc_el     else location
            href    = (link_el.get("href", "") if link_el else "").split("?")[0]
            if title and href:
                jobs.append({"title": title, "company": company, "location": loc,
                              "type": "Contract", "posted": today(), "url": href, "source": "LinkedIn"})
    except Exception as e:
        log.warning(f"  LinkedIn ({location}): {e}")
    return jobs


# ── Source: ZipRecruiter ──────────────────────────────────────────────────────

def search_ziprecruiter(keyword: str, location: str) -> list:
    url = "https://www.ziprecruiter.com/jobs-search"
    params = {"search": keyword, "location": location, "days": 1}
    jobs = []
    try:
        r = requests.get(url, params=params, headers=HEADERS, timeout=12, verify=False)
        r.raise_for_status()
        soup = BeautifulSoup(r.text, "html.parser")
        for card in soup.select("article.job_result"):
            title_el   = card.select_one("h2.title")
            company_el = card.select_one("a.t_org_link") or card.select_one(".company_name")
            loc_el     = card.select_one("[itemprop='addressLocality']") or card.select_one(".location")
            link_el    = card.select_one("a.job_link") or card.select_one("h2.title a")
            title   = title_el.get_text(strip=True)   if title_el   else ""
            company = company_el.get_text(strip=True) if company_el else ""
            loc     = loc_el.get_text(strip=True)     if loc_el     else location
            href    = link_el.get("href", "")         if link_el    else ""
            if title and href:
                jobs.append({"title": title, "company": company, "location": loc,
                              "type": "Contract", "posted": today(), "url": href, "source": "ZipRecruiter"})
    except Exception as e:
        log.warning(f"  ZipRecruiter ({location}): {e}")
    return jobs


# ── Helpers ───────────────────────────────────────────────────────────────────

def today():
    return datetime.now().strftime("%Y-%m-%d")


def send_email(new_jobs: list):
    if not all([EMAIL_FROM, EMAIL_TO, EMAIL_PASSWORD]):
        return
    lines = [f"Found {len(new_jobs)} new contract jobs:\n"]
    for j in new_jobs:
        lines += [f"[{j['source']}] {j['title']} @ {j['company']}", f"  {j['location']}", f"  {j['url']}\n"]
    msg = MIMEText("\n".join(lines))
    msg["Subject"] = f"[Job Hunter] {len(new_jobs)} new contract jobs"
    msg["From"]    = EMAIL_FROM
    msg["To"]      = EMAIL_TO
    try:
        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as s:
            s.login(EMAIL_FROM, EMAIL_PASSWORD)
            s.send_message(msg)
        log.info(f"  Email sent to {EMAIL_TO}")
    except Exception as e:
        log.warning(f"  Email failed: {e}")


# ── Main cycle ────────────────────────────────────────────────────────────────

def run_search():
    log.info("=" * 70)
    log.info(f"Search started — {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    seen     = load_seen()
    new_jobs = []
    total    = len(KEYWORDS) * len(LOCATIONS)
    n        = 0

    for keyword in KEYWORDS:
        for location in LOCATIONS:
            n += 1
            log.info(f"  [{n:02}/{total}] '{keyword[:35]}' | {location}")

            batch = []
            batch += search_dice(keyword, location);        time.sleep(1.5)
            batch += search_indeed(keyword, location);      time.sleep(1.5)
            batch += search_linkedin(keyword, location);    time.sleep(1.5)
            batch += search_ziprecruiter(keyword, location); time.sleep(2)

            for job in batch:
                if not job.get("url"):
                    continue
                jid = job_id(job["url"])
                if jid not in seen:
                    seen.add(jid)
                    job["found_at"] = datetime.now().strftime("%Y-%m-%d %H:%M")
                    new_jobs.append(job)

    save_seen(seen)

    if not new_jobs:
        log.info("No new jobs this cycle.")
        return

    append_csv(new_jobs)
    append_json(new_jobs)

    print(f"\n{'='*70}")
    print(f"  🎯  {len(new_jobs)} NEW CONTRACT JOBS  —  {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    print(f"{'='*70}")
    for j in sorted(new_jobs, key=lambda x: x["source"]):
        title   = j["title"][:48].ljust(48)
        company = j["company"][:26].ljust(26)
        print(f"  [{j['source']:<12}] {title}  {company}  {j['location']}")
    print(f"{'='*70}")
    print(f"  Saved → {OUTPUT_CSV}\n")

    send_email(new_jobs)


# ── Entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    log.info("Contract Job Hunter — GitHub Actions run started")
    run_search()   # single run; GitHub Actions cron handles scheduling
