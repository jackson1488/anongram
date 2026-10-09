#!/usr/bin/env python3
"""
AnonGram Android APK Builder & Waydroid Installer
Builds a lean native Android APK using Android SDK build-tools (aapt2, javac, d8, apksigner)
and directly installs & launches it in Waydroid.
"""

import os
import subprocess
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ANDROID_DIR = os.path.join(BASE_DIR, "android", "app", "src", "main")
BUILD_DIR = os.path.join(BASE_DIR, "build", "android")
os.makedirs(BUILD_DIR, exist_ok=True)

SDK_DIR = "/home/jackson/Android/Sdk"
BUILD_TOOLS = os.path.join(SDK_DIR, "build-tools", "35.0.0")
PLATFORM_JAR = os.path.join(SDK_DIR, "platforms", "android-35", "android.jar")
AAPT2 = os.path.join(BUILD_TOOLS, "aapt2")
D8 = os.path.join(BUILD_TOOLS, "d8")
ZIPALIGN = os.path.join(BUILD_TOOLS, "zipalign")
APKSIGNER = os.path.join(BUILD_TOOLS, "apksigner")

MANIFEST = os.path.join(ANDROID_DIR, "AndroidManifest.xml")
RES_DIR = os.path.join(ANDROID_DIR, "res")
ASSETS_DIR = os.path.join(ANDROID_DIR, "assets")
JAVA_SRC = os.path.join(ANDROID_DIR, "java", "com", "anongram", "app", "MainActivity.java")

print("=== [1/6] Compiling Android Resources with aapt2 ===")
compiled_res = os.path.join(BUILD_DIR, "compiled_res.zip")
res_flat_dir = os.path.join(BUILD_DIR, "res_flat")
os.makedirs(res_flat_dir, exist_ok=True)

subprocess.run([AAPT2, "compile", "--dir", RES_DIR, "-o", compiled_res], check=True)
print("Resources compiled to:", compiled_res)

print("=== [2/6] Linking APK and generating R.java with aapt2 ===")
gen_dir = os.path.join(BUILD_DIR, "gen")
os.makedirs(gen_dir, exist_ok=True)
base_apk = os.path.join(BUILD_DIR, "base.apk")

subprocess.run([
    AAPT2, "link",
    "-o", base_apk,
    "-I", PLATFORM_JAR,
    "--min-sdk-version", "26",
    "--target-sdk-version", "35",
    "--manifest", MANIFEST,
    "--java", gen_dir,
    "-A", ASSETS_DIR,
    compiled_res,
    "--auto-add-overlay"
], check=True)
print("Base APK linked:", base_apk)

print("=== [3/6] Compiling Java classes ===")
bin_dir = os.path.join(BUILD_DIR, "bin")
os.makedirs(bin_dir, exist_ok=True)
r_java = os.path.join(gen_dir, "com", "anongram", "app", "R.java")

subprocess.run([
    "javac",
    "-source", "1.8",
    "-target", "1.8",
    "-cp", f"{PLATFORM_JAR}:{gen_dir}",
    JAVA_SRC, r_java,
    "-d", bin_dir
], check=True)
print("Java classes compiled successfully")

print("=== [4/6] Converting Java bytecode to DEX with d8 ===")
dex_dir = os.path.join(BUILD_DIR, "dex")
os.makedirs(dex_dir, exist_ok=True)

class_files = []
for root, _, files in os.walk(bin_dir):
    for f in files:
        if f.endswith(".class"):
            class_files.append(os.path.join(root, f))

subprocess.run([
    D8,
    "--output", dex_dir,
    "--min-api", "26",
    *class_files
], check=True)
print("DEX files generated in:", dex_dir)

print("=== [5/6] Packaging & Signing APK ===")
classes_dex = os.path.join(dex_dir, "classes.dex")
# Add classes.dex into base.apk
subprocess.run(["zip", "-uj", base_apk, classes_dex], check=True)

aligned_apk = os.path.join(BUILD_DIR, "anongram-aligned.apk")
if os.path.exists(aligned_apk):
    os.remove(aligned_apk)
subprocess.run([ZIPALIGN, "-v", "-p", "4", base_apk, aligned_apk], check=True)

keystore = os.path.join(BUILD_DIR, "debug.keystore")
if not os.path.exists(keystore):
    subprocess.run([
        "/opt/android-studio/jbr/bin/keytool",
        "-genkeypair",
        "-keystore", keystore,
        "-storepass", "android",
        "-alias", "androiddebugkey",
        "-keypass", "android",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=AnonGram,O=Security,C=US"
    ], check=True)

final_apk = os.path.join(BUILD_DIR, "anongram-release.apk")
subprocess.run(["cp", aligned_apk, final_apk], check=True)

subprocess.run([
    APKSIGNER, "sign",
    "--ks", keystore,
    "--ks-pass", "pass:android",
    "--key-pass", "pass:android",
    final_apk
], check=True)

print("\n🎉 SUCCESS! AnonGram APK is ready at:", final_apk)
apk_size = os.path.getsize(final_apk) / 1024
print(f"📦 Final APK Size: {apk_size:.1f} KB")

print("\n=== [6/6] Installing & Launching in Waydroid ===")
try:
    subprocess.run(["adb", "connect", "192.168.240.112:5555"], check=False)
    install_res = subprocess.run(["adb", "-s", "192.168.240.112:5555", "install", "-r", final_apk], capture_output=True, text=True)
    print("ADB Install Result:", install_res.stdout.strip())
    
    launch_res = subprocess.run(["waydroid", "app", "launch", "com.anongram.app"], capture_output=True, text=True)
    print("Waydroid Launch Result:", launch_res.stdout.strip())
except Exception as e:
    print("Notice during Waydroid launch:", e)
