#!/usr/bin/env python3
import urllib.request
import json
import os
import sys
import time
import subprocess

token = ""
with open(os.path.expanduser("~/.git-credentials")) as f:
    for line in f:
        if "@github.com" in line:
            token = line.split("https://")[1].split(":")[1].split("@")[0]
            break

if not token:
    print("Error: GitHub token not found in ~/.git-credentials")
    sys.exit(1)

headers = {
    "Authorization": f"Bearer {token}",
    "Accept": "application/vnd.github+json"
}

head_sha = sys.argv[1] if len(sys.argv) > 1 else None
print(f"Waiting for Cloud CI build (target SHA: {head_sha or 'latest'})...")

run_id = None
for _ in range(30):
    list_url = "https://api.github.com/repos/jackson1488/anongram/actions/runs?per_page=10"
    req = urllib.request.Request(list_url, headers=headers)
    try:
        with urllib.request.urlopen(req) as res:
            data = json.loads(res.read())
            runs = [r for r in data.get("workflow_runs", []) if r.get("name") == "Build AnonGram Android APK (Cloud CI)"]
            if head_sha:
                matching = [r for r in runs if r.get("head_sha", "").startswith(head_sha)]
                if matching:
                    run_id = str(matching[0]["id"])
                    print(f"Found run #{matching[0]['run_number']} for commit {head_sha[:7]} (ID: {run_id})")
                    break
            elif runs:
                run_id = str(runs[0]["id"])
                print(f"Selected latest run #{runs[0]['run_number']} (ID: {run_id})")
                break
    except Exception as e:
        print(f"Waiting to detect run: {e}")
    time.sleep(5)

if not run_id:
    print("Error: Could not find workflow run")
    sys.exit(1)

run_url = f"https://api.github.com/repos/jackson1488/anongram/actions/runs/{run_id}"
for attempt in range(60):
    try:
        req = urllib.request.Request(run_url, headers=headers)
        with urllib.request.urlopen(req) as res:
            data = json.loads(res.read())
            status = data.get("status")
            conclusion = data.get("conclusion")
            print(f"[{time.strftime('%H:%M:%S')}] Run #{data.get('run_number')} ({run_id}): status={status}, conclusion={conclusion}")
            if status == "completed":
                if conclusion == "success":
                    print("CI Build completed successfully!")
                    break
                else:
                    print(f"CI Build failed with conclusion={conclusion}")
                    sys.exit(1)
    except Exception as e:
        print(f"Error checking status: {e}")
    time.sleep(15)
else:
    print("Timeout waiting for CI build")
    sys.exit(2)

print("Fetching build artifacts...")
artifacts_url = f"https://api.github.com/repos/jackson1488/anongram/actions/runs/{run_id}/artifacts"
req = urllib.request.Request(artifacts_url, headers=headers)
with urllib.request.urlopen(req) as res:
    data = json.loads(res.read())
    artifacts = data.get("artifacts", [])
    target = None
    for a in artifacts:
        if a.get("name") == "anongram-signed-apk":
            target = a
            break
    if not target and artifacts:
        target = artifacts[0]

if not target:
    print("Error: No artifacts found for run")
    sys.exit(1)

download_url = target["archive_download_url"]
print(f"Downloading artifact {target['name']} (ID: {target['id']})...")

dest_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "build-artifacts", "debug")
os.makedirs(dest_dir, exist_ok=True)
zip_path = os.path.join(dest_dir, "artifact.zip")
dest_apk = os.path.join(dest_dir, "app-debug.apk")

subprocess.run([
    "curl", "-sL",
    "-H", f"Authorization: Bearer {token}",
    "-H", "Accept: application/vnd.github+json",
    "-o", zip_path,
    download_url
], check=True)

subprocess.run(["unzip", "-o", zip_path, "-d", dest_dir], check=True)

for root, _, files in os.walk(dest_dir):
    for f in files:
        if f.endswith(".apk") and os.path.join(root, f) != dest_apk:
            src = os.path.join(root, f)
            os.replace(src, dest_apk)
            break

print(f"Successfully saved APK to: {dest_apk}")
apk_size_kb = os.path.getsize(dest_apk) / 1024
print(f"APK Size: {apk_size_kb:.1f} KB")
