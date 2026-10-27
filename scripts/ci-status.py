import urllib.request
import json
import os

token = ""
with open(os.path.expanduser("~/.git-credentials")) as f:
    for line in f:
        if "@github.com" in line:
            token = line.split("https://")[1].split(":")[1].split("@")[0]
            break

req = urllib.request.Request(
    "https://api.github.com/repos/jackson1488/anongram/actions/runs?per_page=5",
    headers={"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json"}
)
res = urllib.request.urlopen(req)
data = json.loads(res.read())
for r in data.get("workflow_runs", []):
    num = r.get("run_number")
    rid = r.get("id")
    status = r.get("status")
    conclusion = r.get("conclusion")
    sha = (r.get("head_sha") or "")[:7]
    commit = r.get("head_commit") or {}
    msg = (commit.get("message") or "").splitlines()[0] if commit else ""
    print(f"Run #{num}: id={rid} status={status} conclusion={conclusion} commit={sha} msg={msg}")
