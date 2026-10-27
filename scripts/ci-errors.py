import subprocess
import json
import os
import sys

token = ""
with open(os.path.expanduser("~/.git-credentials")) as f:
    for line in f:
        if "@github.com" in line:
            token = line.split("https://")[1].split(":")[1].split("@")[0]
            break

run_id = sys.argv[1] if len(sys.argv) > 1 else "40520256644"
cmd = [
    "curl", "-sL",
    "-H", f"Authorization: Bearer {token}",
    "-H", "Accept: application/vnd.github+json",
    f"https://api.github.com/repos/jackson1488/anongram/actions/runs/{run_id}/jobs"
]
out = subprocess.check_output(cmd)
jobs = json.loads(out).get("jobs", [])

for j in jobs:
    print("Job:", j.get("name"), "conclusion:", j.get("conclusion"))
    job_id = j.get("id")
    log_cmd = [
        "curl", "-sL",
        "-H", f"Authorization: Bearer {token}",
        f"https://api.github.com/repos/jackson1488/anongram/actions/jobs/{job_id}/logs"
    ]
    raw_logs = subprocess.check_output(log_cmd).decode("utf-8", errors="replace").splitlines()
    err_lines = [l for l in raw_logs if "e: " in l or "FAILED" in l or "error:" in l.lower()]
    print("--- Found", len(err_lines), "error lines ---")
    for l in err_lines:
        print(l)
