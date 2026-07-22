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

# ── Config ────────────────────────────────────────────────────────────────────

ADZUNA_APP_ID  = os.getenv("ADZUNA_APP_ID", "")
ADZUNA_APP_KEY = os.getenv("ADZUNA_APP_KEY", "")

# General keyword searches — contract + full-time + C2C
KEYWORDS = [
    "Java Spring Boot",
    "Java microservices",
    "Java backend engineer",
    "Java developer Spring Boot",
    "Java Kafka microservices",
    "Java Spring Boot Kubernetes",
    "Java software engineer",
    "Java developer",
    "Java backend developer",
    "software engineer Java",
    "software developer Java Spring",
    "backend software engineer Java",
    # C2C specific
    "Java developer C2C",
    "Java Spring Boot C2C",
    "Java engineer corp to corp",
    "Java developer W2 contract",
    # Full-time specific
    "Java Spring Boot full time",
    "Java developer full time",
    "senior Java developer",
]

# Vendor searches — kept minimal, company filtering done in filter_by_vendor()
VENDOR_SEARCHES = [
    "Java Spring Boot contract",
    "Java developer contract",
    "Java microservices contract",
    "senior Java engineer",
]

# Target staffing companies — matched against Adzuna's company field in results
TARGET_VENDORS = [
    # US Staffing Firms
    "judge group", "judge",
    "teksystems", "tek systems",
    "ust global", "ust",
    "randstad", "randstad technologies", "randstad digital",
    "insight global",
    "apex systems", "apex",
    "robert half", "robert half technology",
    "akkodis", "adecco",
    "experis", "manpowergroup", "manpower",
    "kforce",
    "tekrecruiter",
    "west monroe",
    "cyient", "mastech", "softpath", "mindlance",
    "collabera", "genesis10", "pearson frank",
    "russell tobin", "motion recruitment",
    "spherion", "staffmark", "staffworks",
    # Big IT / Consulting / Offshore
    "infosys", "infosys bpo",
    "cognizant", "cognizant technology",
    "tcs", "tata consultancy",
    "wipro",
    "hcl", "hcltech",
    "capgemini",
    "ibm", "ibm consulting",
    "accenture",
    "epam",
    "ltimindtree", "lti", "mindtree",
    "mphasis",
    "tech mahindra",
    "hexaware",
    "birlasoft",
    "niit technologies",
    "persistent systems",
]

# No location filter — search all USA, results include location column
RESULTS_PER_PAGE  = 50
PAGES_PER_KEYWORD = 2   # 50 x 2 = 100 results per keyword/vendor

OUTPUT_DIR  = Path(__file__).parent
OUTPUT_CSV  = OUTPUT_DIR / "contract_jobs.csv"
OUTPUT_JSON = OUTPUT_DIR / "contract_jobs.json"
SEEN_FILE   = OUTPUT_DIR / "seen_jobs.json"
LOG_FILE    = OUTPUT_DIR / "job_hunter.log"

EMAIL_FROM     = os.getenv("EMAIL_FROM", "")
EMAIL_TO       = os.getenv("EMAIL_TO", "")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD", "")

CSV_FIELDS = ["title", "company", "location", "salary", "type", "posted", "url", "adzuna_url", "source", "found_at"]

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

def resolve_url(adzuna_url: str) -> str:
    """
    Fetch the Adzuna job page and extract the real external job URL.
    Looks for 'Return to job advert' link or canonical source URL.
    Falls back to the Adzuna URL — click 'Return to job advert' to apply without login.
    """
    try:
        req = urllib.request.Request(
            adzuna_url,
            headers={
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Accept": "text/html,application/xhtml+xml,*/*",
            },
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode("utf-8", errors="ignore")

        # Patterns Adzuna uses for the original source/apply URL
        patterns = [
            r'"sourceUrl"\s*:\s*"([^"]+)"',                    # source URL in JSON
            r'"externalApplyUrl"\s*:\s*"([^"]+)"',             # explicit external apply
            r'data-href=["\']([^"\']+)["\'][^>]*return.to.job', # return to job advert
            r'"canonicalUrl"\s*:\s*"([^"]+)"',                 # canonical URL
            r'href=["\']([^"\']+)["\'][^>]*>Return to job',    # return to job advert link
            r'"applyUrl"\s*:\s*"([^"]+)"',                     # generic apply URL
        ]
        for pattern in patterns:
            m = re.search(pattern, html, re.IGNORECASE)
            if m:
                candidate = m.group(1).replace("\\u0026", "&").replace("\\/", "/").replace("\\u003d", "=")
                if candidate.startswith("http") and "adzuna.com" not in candidate:
                    return candidate

    except Exception:
        pass
    # Fallback: return Adzuna URL — on that page click "Return to job advert" to apply directly
    return adzuna_url


def search_adzuna(keyword: str, page: int = 1) -> list:
    if not ADZUNA_APP_ID or not ADZUNA_APP_KEY:
        log.error("ADZUNA_APP_ID / ADZUNA_APP_KEY not set. See script header.")
        return []

    params = {
        "app_id":           ADZUNA_APP_ID,
        "app_key":          ADZUNA_APP_KEY,
        "results_per_page": RESULTS_PER_PAGE,
        "what":             keyword,
        "sort_by":          "date",  # newest first
        "content-type":     "application/json",
    }
    url = f"https://api.adzuna.com/v1/api/jobs/us/search/{page}?{urllib.parse.urlencode(params)}"

    jobs = []
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "JobHunter/1.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode())

        cutoff = datetime.now(timezone.utc) - timedelta(days=3)

        for item in data.get("results", []):
            # Skip jobs older than 3 days
            created_str = item.get("created", "")
            if created_str:
                try:
                    created_dt = datetime.fromisoformat(created_str.replace("Z", "+00:00"))
                    if created_dt < cutoff:
                        continue
                except Exception:
                    pass

            location_str = item.get("location", {}).get("display_name", "")
            adzuna_link  = item.get("redirect_url", "")
            real_url     = resolve_url(adzuna_link) if adzuna_link else ""

            # Skip Dice.com postings — often expired/stale aggregator reposts
            if "dice.com" in real_url or "dice.com" in adzuna_link:
                continue

            s_min = item.get("salary_min")
            s_max = item.get("salary_max")
            salary = ""
            if s_min and s_max:
                salary = f"${int(s_min/1000)}k-${int(s_max/1000)}k"
            elif s_min:
                salary = f"${int(s_min/1000)}k+"

            # Detect job type from Adzuna field + title keywords
            raw_type = (item.get("contract_type") or "").lower()
            title_lower = item.get("title", "").lower()
            if "permanent" in raw_type or "full_time" in raw_type or "full-time" in title_lower or "permanent" in title_lower:
                job_type = "Full-time"
            elif "contract" in raw_type or "contract" in title_lower or "c2c" in title_lower or "w2" in title_lower:
                job_type = "Contract"
            else:
                job_type = "Contract/FT"   # unknown — show both

            jobs.append({
                "title":      item.get("title", ""),
                "company":    item.get("company", {}).get("display_name", ""),
                "location":   location_str,
                "salary":     salary,
                "type":       job_type,
                "posted":     (item.get("created") or "")[:10],
                "url":        real_url,
                "adzuna_url": adzuna_link,
                "source":     "Adzuna",
            })
    except Exception as e:
        log.warning(f"  Adzuna error ('{keyword}' page {page}): {e}")
    return jobs

# ── Y Combinator / HN Who's Hiring ───────────────────────────────────────────

YC_JAVA_KEYWORDS = ["java", "spring boot", "spring", "backend", "microservice",
                    "kafka", "kubernetes", "distributed", "jvm", "kotlin"]

def search_yc_hiring() -> list:
    """
    Fetches the latest HN 'Ask HN: Who is Hiring?' thread via Algolia API.
    Filters comments containing Java/backend keywords.
    No API key required. Works from GitHub Actions.
    """
    jobs = []
    try:
        # Step 1: Find latest Who's Hiring thread
        search_url = (
            "https://hn.algolia.com/api/v1/search"
            "?query=Ask+HN%3A+Who+is+hiring%3F"
            "&tags=story,ask_hn"
            "&hitsPerPage=3"
        )
        req = urllib.request.Request(search_url, headers={"User-Agent": "JobHunter/1.0"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode())

        hits = data.get("hits", [])
        if not hits:
            log.warning("YC: No 'Who is hiring' thread found")
            return []

        thread = hits[0]
        story_id = thread.get("objectID")
        thread_title = thread.get("title", "")
        log.info(f"  YC thread: {thread_title} (id={story_id})")

        # Step 2: Fetch comments (job posts) from the thread
        comments_url = (
            f"https://hn.algolia.com/api/v1/search"
            f"?tags=comment,story_{story_id}"
            f"&hitsPerPage=500"
        )
        req2 = urllib.request.Request(comments_url, headers={"User-Agent": "JobHunter/1.0"})
        with urllib.request.urlopen(req2, timeout=15) as resp2:
            comments_data = json.loads(resp2.read().decode())

        cutoff = datetime.now(timezone.utc) - timedelta(days=35)  # HN thread is monthly

        for comment in comments_data.get("hits", []):
            text = (comment.get("comment_text") or "").lower()
            if not text:
                continue

            # Must mention Java or backend keywords
            if not any(kw in text for kw in YC_JAVA_KEYWORDS):
                continue

            # Skip if not USA-based (rough check)
            if not any(loc in text for loc in ["usa", "us only", "united states",
                                                "remote", "new york", "san francisco",
                                                "seattle", "austin", "chicago", "boston",
                                                "anywhere"]):
                continue

            # Extract salary if mentioned
            salary = ""
            sal_match = re.search(r"\$(\d+)[k]?\s*[-–]\s*\$?(\d+)k?", text)
            if sal_match:
                a, b = sal_match.group(1), sal_match.group(2)
                salary = f"${a}k-${b}k" if len(a) <= 3 else f"${int(a)//1000}k-${int(b)//1000}k"

            # Detect remote
            is_remote = any(x in text for x in ["remote", "work from home", "wfh", "anywhere"])
            location  = "Remote" if is_remote else "USA"

            # Try to extract company name from first line
            raw_text   = comment.get("comment_text") or ""
            first_line = re.sub(r"<[^>]+>", "", raw_text).strip().split("\n")[0][:80]
            company    = first_line if first_line else "HN Company"

            comment_id = comment.get("objectID", "")
            hn_url     = f"https://news.ycombinator.com/item?id={comment_id}"

            jobs.append({
                "title":      "Java/Backend Engineer",
                "company":    company,
                "location":   location,
                "salary":     salary,
                "type":       "Full-time",
                "posted":     (comment.get("created_at") or "")[:10],
                "url":        hn_url,
                "adzuna_url": hn_url,
                "source":     "YCombinator",
            })

        log.info(f"  YC: {len(jobs)} Java/backend jobs found")

    except Exception as e:
        log.warning(f"  YC error: {e}")

    return jobs


# ── Relevance filter ──────────────────────────────────────────────────────────

# Must contain Java + at least one core skill to be considered relevant
MUST_HAVE      = ["java"]
CORE_SKILLS    = ["spring boot", "spring", "microservice", "backend", "distributed", "api"]
BONUS_SKILLS   = ["kafka", "kubernetes", "k8s", "aws", "gcp", "cassandra", "docker",
                  "hibernate", "jpa", "rest", "oauth", "ci/cd", "kafka", "kafka streams"]
EXCLUDE_TITLES = ["frontend", "front-end", "angular", "react developer", "ios", "android",
                  "qa engineer", "test engineer", "data engineer", "data scientist",
                  "ml engineer", "machine learning", "devops engineer", "ui developer",
                  "php", "ruby", ".net developer", "c# developer", "salesforce"]

def relevance_score(job: dict) -> int:
    text = (job.get("title", "") + " " + job.get("description", "")).lower()

    # Exclude clearly irrelevant roles
    title_l = job.get("title", "").lower()
    if any(x in title_l for x in EXCLUDE_TITLES):
        return 0

    # Must have Java
    if not any(m in text for m in MUST_HAVE):
        return 0

    score = 10  # baseline — passed Java check

    # Core skills
    for skill in CORE_SKILLS:
        if skill in text:
            score += 5

    # Bonus skills from your stack
    for skill in BONUS_SKILLS:
        if skill in text:
            score += 3

    # Salary present = more specific posting
    if job.get("salary"):
        score += 5

    return score


def is_target_vendor(job: dict) -> bool:
    company = job.get("company", "").lower()
    return any(v in company for v in TARGET_VENDORS)


def filter_relevant(jobs: list, min_score: int = 15) -> list:
    scored = [(relevance_score(j), j) for j in jobs]
    relevant = [(s, j) for s, j in scored if s >= min_score]
    relevant.sort(key=lambda x: -x[0])  # highest score first
    return [j for _, j in relevant]


# ── Email ─────────────────────────────────────────────────────────────────────

def build_html_email(new_jobs: list) -> str:
    vendor_jobs = [j for j in new_jobs if is_target_vendor(j)]
    yc_jobs     = [j for j in new_jobs if j.get("source") == "YCombinator"]
    contract    = [j for j in new_jobs if j["type"] == "Contract"]
    fulltime    = [j for j in new_jobs if j["type"] == "Full-time"]
    other       = [j for j in new_jobs if j["type"] == "Contract/FT"]

    def type_badge(t):
        if t == "Contract":
            return '<span style="background:#065f46;color:#34d399;padding:2px 8px;border-radius:12px;font-size:11px;font-weight:bold">Contract</span>'
        if t == "Full-time":
            return '<span style="background:#1e3a5f;color:#60a5fa;padding:2px 8px;border-radius:12px;font-size:11px;font-weight:bold">Full-time</span>'
        return '<span style="background:#3b1f5e;color:#c084fc;padding:2px 8px;border-radius:12px;font-size:11px;font-weight:bold">Contract/FT</span>'

    def job_rows(jobs):
        rows = ""
        for j in jobs:
            apply_url = j.get("url") or j.get("adzuna_url", "#")
            rows += f"""
            <tr style="border-bottom:1px solid #334155">
              <td style="padding:10px 8px">
                <div style="font-weight:600;color:#e2e8f0">{j['title']}</div>
                <div style="color:#94a3b8;font-size:12px">{j['company']}</div>
              </td>
              <td style="padding:10px 8px;color:#94a3b8;font-size:13px">{j['location']}</td>
              <td style="padding:10px 8px;color:#34d399;font-size:13px;font-weight:600">{j['salary'] or '—'}</td>
              <td style="padding:10px 8px">{type_badge(j['type'])}</td>
              <td style="padding:10px 8px;color:#64748b;font-size:12px">{j['posted']}</td>
              <td style="padding:10px 8px">
                <a href="{apply_url}" style="background:#059669;color:#fff;padding:6px 14px;border-radius:6px;text-decoration:none;font-size:12px;font-weight:bold">Apply →</a>
              </td>
            </tr>"""
        return rows

    def section(title, jobs, color):
        if not jobs:
            return ""
        return f"""
        <h3 style="color:{color};margin:24px 0 8px">{title} ({len(jobs)})</h3>
        <table style="width:100%;border-collapse:collapse;background:#1e293b;border-radius:8px;overflow:hidden">
          <thead>
            <tr style="background:#0f172a">
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">ROLE</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">LOCATION</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">SALARY</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">TYPE</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">POSTED</th>
              <th style="padding:8px;text-align:left;color:#64748b;font-size:11px">APPLY</th>
            </tr>
          </thead>
          <tbody>{job_rows(jobs)}</tbody>
        </table>"""

    return f"""
    <html><body style="background:#0f172a;color:#e2e8f0;font-family:system-ui,sans-serif;padding:24px;margin:0">
      <div style="max-width:900px;margin:0 auto">
        <h2 style="color:#34d399;margin-bottom:4px">🎯 {len(new_jobs)} New Java Jobs Found</h2>
        <p style="color:#64748b;margin:0 0 20px">{datetime.now().strftime('%B %d, %Y %H:%M')} — Contract + Full-time across USA</p>
        {section("🚀 Y Combinator — Who's Hiring", yc_jobs, "#f97316")}
        {section("🎯 Target Staffing Vendors", vendor_jobs, "#f59e0b")}
        {section("📋 Contract Roles", contract, "#34d399")}
        {section("💼 Full-time Roles", fulltime, "#60a5fa")}
        {section("🔀 Contract or Full-time", other, "#c084fc")}
        <p style="color:#475569;font-size:11px;margin-top:24px">Powered by MarketCompass Job Hunter · Adzuna API</p>
      </div>
    </body></html>"""


def send_email(new_jobs: list):
    if not all([EMAIL_FROM, EMAIL_TO, EMAIL_PASSWORD]):
        return

    msg = MIMEMultipart("alternative")
    msg["Subject"] = f"[Job Hunter] {len(new_jobs)} new Java jobs — Contract + Full-time"
    msg["From"]    = EMAIL_FROM
    msg["To"]      = EMAIL_TO

    # Plain text fallback
    plain = f"Found {len(new_jobs)} new Java jobs.\n\n"
    for j in new_jobs:
        plain += f"[{j['type']}] {j['title']} @ {j['company']} | {j['location']} | {j['salary'] or 'N/A'}\n{j.get('url') or j.get('adzuna_url','')}\n\n"
    msg.attach(MIMEText(plain, "plain"))

    # Rich HTML email
    msg.attach(MIMEText(build_html_email(new_jobs), "html"))

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

    # ── General keyword searches ───────────────────────────────────────────────
    for keyword in KEYWORDS:
        log.info(f"  Keyword: '{keyword}'")
        for page in range(1, PAGES_PER_KEYWORD + 1):
            batch = search_adzuna(keyword, page)
            log.info(f"    Page {page}: {len(batch)} jobs")
            for job in batch:
                if not job.get("url"):
                    continue
                jid = job_id(job["url"])
                if jid not in seen:
                    seen.add(jid)
                    job["found_at"] = datetime.now().strftime("%Y-%m-%d %H:%M")
                    new_jobs.append(job)
            time.sleep(0.5)

    # ── Y Combinator Who's Hiring ─────────────────────────────────────────────
    log.info("  --- Y Combinator Who's Hiring ---")
    yc_batch = search_yc_hiring()
    for job in yc_batch:
        if not job.get("url"):
            continue
        jid = job_id(job["url"])
        if jid not in seen:
            seen.add(jid)
            job["found_at"] = datetime.now().strftime("%Y-%m-%d %H:%M")
            new_jobs.append(job)

    # ── Staffing vendor searches ───────────────────────────────────────────────
    log.info("  --- Staffing vendor searches ---")
    for keyword in VENDOR_SEARCHES:
        log.info(f"  Vendor search: '{keyword}'")
        for page in range(1, PAGES_PER_KEYWORD + 1):
            batch = search_adzuna(keyword, page)
            log.info(f"    Page {page}: {len(batch)} jobs")
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

    # Filter to only relevant jobs matching your profile
    relevant_jobs = filter_relevant(new_jobs)
    log.info(f"Relevance filter: {len(new_jobs)} total → {len(relevant_jobs)} relevant")

    # Always write ALL jobs to CSV/JSON for reference
    append_csv(new_jobs)
    append_json(new_jobs)

    if not relevant_jobs:
        log.info("No relevant new jobs this cycle.")
        return

    print(f"\n{'='*70}")
    print(f"  {len(relevant_jobs)} RELEVANT JOBS  (filtered from {len(new_jobs)} total) — {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    print(f"{'='*70}")
    for j in relevant_jobs:
        print(f"  [{j['type']:<10}] {j['title'][:44]:<44}  {j['company'][:22]:<22}  {j['salary'] or '—'}")
    print(f"{'='*70}")
    print(f"  Saved -> {OUTPUT_CSV}\n")

    # Email only the relevant, scored jobs
    send_email(relevant_jobs)

if __name__ == "__main__":
    log.info("Contract Job Hunter (Adzuna API) started")
    run_search()
