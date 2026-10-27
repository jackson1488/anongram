import urllib.request
import json
import os
import time
import sys

token = ""
with open(os.path.expanduser("~/.git-credentials")) as f:
    for line in f:
        if "@github.com" in line:
            token = line.split("https://")[1].split(":")[1].split("@")[0]
            break

run_id = sys.argv[1] if len(sys.argv) > 1 else "40520256644"
url = f"https://api.github.com/repos/jackson1488/anongram/actions/runs/{run_id}"

headers = {"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json"}

for _ in range(60):
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as res:
            data = json.loads(res.read())
            status = data.get("status")
            conclusion = data.get("conclusion")
            print(f"[{time.strftime('%H:%M:%S')}] Run #{data.get('run_number')} ({run_id}): status={status}, conclusion={conclusion}")
            if status == "completed":
                if conclusion == "success":
                    print("SUCCESS! CI build passed.")
                    sys.exit(0)
                else:
                    print(f"FAILED with conclusion={conclusion}")
                    sys.exit(1)
    except Exception as e:
        print(f"Error fetching status: {e}")
    time.sleep(15)

print("Timeout waiting for CI")
sys.exit(2)
