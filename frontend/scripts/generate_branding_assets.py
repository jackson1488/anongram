#!/usr/bin/env python3
"""
AnonGram Master Asset Generator
Generates full SVG icon suites, Android Vector Drawables, and raster mipmap sets
based on the approved Cryptographic Shield + Stealth Origami Airplane identity.
"""

import os
import subprocess

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS_DIR = os.path.join(BASE_DIR, "assets", "icons")
DRAWABLE_DIR = os.path.join(BASE_DIR, "android", "app", "src", "main", "res", "drawable")
RES_DIR = os.path.join(BASE_DIR, "android", "app", "src", "main", "res")

os.makedirs(ASSETS_DIR, exist_ok=True)
os.makedirs(DRAWABLE_DIR, exist_ok=True)

# ---------------------------------------------------------------------------
# 1. SVGs for Flutter & Web Suite
# ---------------------------------------------------------------------------

SVG_LOGO_COLOR = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100" fill="none">
  <defs>
    <linearGradient id="shieldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#00f2fe" />
      <stop offset="50%" stop-color="#06b6d4" />
      <stop offset="100%" stop-color="#10b981" />
    </linearGradient>
    <linearGradient id="planeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#ffffff" />
      <stop offset="100%" stop-color="#a7f3d0" />
    </linearGradient>
    <filter id="neonGlow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="3" result="blur" />
      <feComposite in="SourceGraphic" in2="blur" operator="over" />
    </filter>
  </defs>

  <!-- Ambient Shield Glow -->
  <path d="M50 90S84 73 84 48V20L50 8 16 20v28c0 25 34 42 34 42z" 
        stroke="url(#shieldGrad)" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"
        opacity="0.95" filter="url(#neonGlow)" />

  <!-- Origami Stealth Airplane -->
  <!-- Left wing -->
  <polygon points="50,26 28,52 46,47" fill="#6ee7b7" opacity="0.9" />
  <!-- Right wing (main body) -->
  <polygon points="50,26 72,52 50,72 46,47" fill="url(#planeGrad)" />
  <!-- Center spine fold -->
  <line x1="50" y1="26" x2="46" y2="47" stroke="#047857" stroke-width="1.5" stroke-linecap="round" />
  <line x1="46" y1="47" x2="50" y2="72" stroke="#047857" stroke-width="1.5" stroke-linecap="round" />
</svg>"""

SVG_LOGO_MONOCHROME = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
  <path d="M7 11l10-4-4 10-2-4-4-2z"/>
</svg>"""

SVG_VPN_CONNECTED = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none">
  <defs>
    <linearGradient id="vpnGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#00f2fe" />
      <stop offset="100%" stop-color="#10b981" />
    </linearGradient>
  </defs>
  <!-- Shield with glowing gradient -->
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" stroke="url(#vpnGrad)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
  <!-- Stealth plane -->
  <path d="M7 11l10-4-4 10-2-4-4-2z" fill="#10b981" stroke="#ffffff" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
  <!-- Active signal arcs -->
  <path d="M19 8a5 5 0 0 1 0 7" stroke="#00f2fe" stroke-width="1.8" stroke-linecap="round" />
  <path d="M5 8a5 5 0 0 0 0 7" stroke="#00f2fe" stroke-width="1.8" stroke-linecap="round" />
</svg>"""

SVG_VPN_CONNECTING = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none">
  <!-- Dashed pulsing shield -->
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" stroke="#38bdf8" stroke-width="2" stroke-dasharray="3 2" stroke-linecap="round" stroke-linejoin="round" />
  <!-- Stealth plane rotating / pulsing -->
  <path d="M7 11l10-4-4 10-2-4-4-2z" stroke="#e0f2fe" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
  <!-- Connecting sync ring -->
  <circle cx="12" cy="12" r="9" stroke="#0284c7" stroke-width="1" stroke-dasharray="4 4" opacity="0.5"/>
</svg>"""

SVG_VPN_DISCONNECTED = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#6b7280" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" opacity="0.6" />
  <path d="M7 11l10-4-4 10-2-4-4-2z" opacity="0.5" />
  <line x1="3" y1="3" x2="21" y2="21" stroke="#9ca3af" stroke-width="1.8" stroke-linecap="round" />
</svg>"""

SVG_VPN_KILLSWITCH = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none">
  <!-- Red Shield for Zero-Leak KillSwitch -->
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
  <!-- Heavy Padlock inside -->
  <rect x="8" y="11" width="8" height="6" rx="1.5" fill="#ef4444" stroke="#fca5a5" stroke-width="1" />
  <path d="M9.5 11V8.5a2.5 2.5 0 0 1 5 0V11" stroke="#fca5a5" stroke-width="1.6" stroke-linecap="round" />
  <circle cx="12" cy="14" r="0.8" fill="#ffffff" />
</svg>"""

SVG_CALL_SHIELD = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
  <!-- Phone Handset inside -->
  <path d="M15 13.5c-.7 0-1.4-.1-2-.4-.3-.1-.6 0-.8.2l-.9.9a7.8 7.8 0 0 1-3.5-3.5l.9-.9c.2-.2.3-.5.2-.8-.3-.6-.4-1.3-.4-2 0-.6-.4-1-1-1H6c-.6 0-1 .4-1 1 0 5.5 4.5 10 10 10 .6 0 1-.4 1-1v-1.5c0-.6-.4-1-1-1z" fill="#10b981" stroke="#ffffff" stroke-width="1.2" />
</svg>"""

SVG_SECURITY_SHIELD = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
  <!-- Padlock Keyhole -->
  <rect x="8.5" y="10.5" width="7" height="5.5" rx="1" fill="#06b6d4" stroke="#ffffff" stroke-width="1" />
  <path d="M10 10.5V8.5a2 2 0 1 1 4 0v2" stroke="#ffffff" stroke-width="1.5" />
</svg>"""

SVG_GROUP_SHIELD = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
  <!-- Lead plane -->
  <path d="M6 10l8-3-3 8-1.5-3-3.5-2z" stroke-width="1.5"/>
  <!-- Wingman plane -->
  <path d="M12 13l6-2.5-2.5 6-1-2.2-2.5-1.3z" stroke-width="1.2" opacity="0.85"/>
</svg>"""

SVG_BURN_SHIELD = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
  <!-- Burning flame / timer countdown -->
  <path d="M12 7c1 2 3 3 3 5a3 3 0 0 1-6 0c0-1.5 1-2.5 3-5z" fill="#f59e0b" stroke="#ffffff" stroke-width="1"/>
  <circle cx="12" cy="13" r="1" fill="#ffffff"/>
</svg>"""

svg_files = {
    "anongram_logo_color.svg": SVG_LOGO_COLOR,
    "anongram_logo_monochrome.svg": SVG_LOGO_MONOCHROME,
    "vpn_shield_connected.svg": SVG_VPN_CONNECTED,
    "vpn_shield_connecting.svg": SVG_VPN_CONNECTING,
    "vpn_shield_disconnected.svg": SVG_VPN_DISCONNECTED,
    "vpn_shield_killswitch.svg": SVG_VPN_KILLSWITCH,
    "call_shield.svg": SVG_CALL_SHIELD,
    "security_shield.svg": SVG_SECURITY_SHIELD,
    "group_shield.svg": SVG_GROUP_SHIELD,
    "burn_shield.svg": SVG_BURN_SHIELD,
}

for name, content in svg_files.items():
    path = os.path.join(ASSETS_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")
    print(f"Created SVG asset: {path}")

# ---------------------------------------------------------------------------
# 2. Android Vector Drawables (res/drawable/)
# ---------------------------------------------------------------------------

XML_IC_STAT_ANONGRAM = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Cryptographic Shield -->
    <path
        android:pathData="M12,22 C12,22 20,18 20,12 V5 L12,2 L4,5 V12 C4,18 12,22 12,22 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Stealth Origami Plane -->
    <path
        android:pathData="M7,11 L17,7 L13,17 L11,13 L7,11 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
</vector>
"""

XML_IC_STAT_ANONGRAM_VPN = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Shield -->
    <path
        android:pathData="M12,22 C12,22 20,18 20,12 V5 L12,2 L4,5 V12 C4,18 12,22 12,22 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Stealth Plane -->
    <path
        android:pathData="M7,11 L17,7 L13,17 L11,13 L7,11 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Right Tunnel Arc -->
    <path
        android:pathData="M19,8 C20.5,9.5 20.5,12.5 19,14"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"/>
    <!-- Left Tunnel Arc -->
    <path
        android:pathData="M5,8 C3.5,9.5 3.5,12.5 5,14"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"/>
</vector>
"""

XML_IC_STAT_ANONGRAM_CALL = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Shield -->
    <path
        android:pathData="M12,22 C12,22 20,18 20,12 V5 L12,2 L4,5 V12 C4,18 12,22 12,22 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Secure Phone Handset -->
    <path
        android:pathData="M14.5,13.2 C13.8,13.2 13.2,13.1 12.6,12.8 C12.3,12.7 12,12.8 11.8,13 L10.9,13.9 C9.3,12.8 8.2,11.7 7.1,10.1 L8,9.2 C8.2,9 8.3,8.7 8.2,8.4 C7.9,7.8 7.8,7.2 7.8,6.5 C7.8,5.9 7.4,5.5 6.8,5.5 L5.5,5.5 C4.9,5.5 4.5,5.9 4.5,6.5 C4.5,12 8.9,16.5 14.5,16.5 C15.1,16.5 15.5,16.1 15.5,15.5 L15.5,14.2 C15.5,13.6 15.1,13.2 14.5,13.2 Z"
        android:strokeColor="#FFFFFFFF"
        android:fillColor="#FFFFFFFF"
        android:strokeWidth="0.8"/>
</vector>
"""

XML_IC_STAT_ANONGRAM_LOCK = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Shield -->
    <path
        android:pathData="M12,22 C12,22 20,18 20,12 V5 L12,2 L4,5 V12 C4,18 12,22 12,22 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Padlock Body -->
    <path
        android:pathData="M8.5,11 H15.5 V16.5 H8.5 Z"
        android:strokeColor="#FFFFFFFF"
        android:fillColor="#FFFFFFFF"
        android:strokeWidth="1"/>
    <!-- Padlock Shackle -->
    <path
        android:pathData="M10,11 V8.5 C10,7.4 10.9,6.5 12,6.5 C13.1,6.5 14,7.4 14,8.5 V11"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"/>
</vector>
"""

XML_IC_STAT_ANONGRAM_GROUP = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Shield -->
    <path
        android:pathData="M12,22 C12,22 20,18 20,12 V5 L12,2 L4,5 V12 C4,18 12,22 12,22 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Lead Plane -->
    <path
        android:pathData="M6,10 L14,7 L11,15 L9.5,12 L6,10 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Second Member Plane -->
    <path
        android:pathData="M12,13.5 L17.5,11 L15.5,16.5 L14.5,14.5 L12,13.5 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="1.3"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
</vector>
"""

XML_IC_STAT_ANONGRAM_BURN = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Shield -->
    <path
        android:pathData="M12,22 C12,22 20,18 20,12 V5 L12,2 L4,5 V12 C4,18 12,22 12,22 Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Self-Destruct Flame / Spark -->
    <path
        android:pathData="M12,7 C13,9 15,10 15,12 C15,13.6 13.6,15 12,15 C10.4,15 9,13.6 9,12 C9,10.5 10,9.5 12,7 Z"
        android:strokeColor="#FFFFFFFF"
        android:fillColor="#FFFFFFFF"
        android:strokeWidth="0.8"/>
</vector>
"""

# Android Adaptive Launcher Icons (108x108dp viewport)
XML_IC_LAUNCHER_FOREGROUND = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group android:translateX="18" android:translateY="18" android:scaleX="3.0" android:scaleY="3.0">
        <!-- Cryptographic Shield -->
        <path
            android:pathData="M12,22 C12,22 20,18 20,12 V5 L12,2 L4,5 V12 C4,18 12,22 12,22 Z"
            android:strokeColor="#00F2FE"
            android:strokeWidth="1.8"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"/>
        <!-- Stealth Origami Airplane -->
        <path
            android:pathData="M7,11 L17,7 L13,17 L11,13 L7,11 Z"
            android:fillColor="#10B981"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="1.2"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"/>
    </group>
</vector>
"""

XML_IC_LAUNCHER_BACKGROUND = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Deep AMOLED obsidian background -->
    <path
        android:pathData="M0,0 H108 V108 H0 Z"
        android:fillColor="#0A0D14"/>
    <!-- Subtle Hex Grid Circles -->
    <path
        android:pathData="M54,18 L85,36 V72 L54,90 L23,72 V36 Z"
        android:strokeColor="#1F2937"
        android:strokeWidth="1"
        android:strokeAlpha="0.4"/>
</vector>
"""

XML_IC_LAUNCHER = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""

XML_IC_SPLASH_LOGO = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="192dp"
    android:height="192dp"
    android:viewportWidth="100"
    android:viewportHeight="100">
    <!-- Master Splash Shield -->
    <path
        android:pathData="M50,90 C50,90 84,73 84,48 V20 L50,8 L16,20 V48 C16,73 50,90 50,90 Z"
        android:strokeColor="#00F2FE"
        android:strokeWidth="6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Stealth Plane Left Wing -->
    <path
        android:pathData="M50,26 L28,52 L46,47 Z"
        android:fillColor="#10B981"/>
    <!-- Stealth Plane Right Wing -->
    <path
        android:pathData="M50,26 L72,52 L50,72 L46,47 Z"
        android:fillColor="#FFFFFF"/>
</vector>
"""

drawables = {
    "ic_stat_anongram.xml": XML_IC_STAT_ANONGRAM,
    "ic_stat_anongram_vpn.xml": XML_IC_STAT_ANONGRAM_VPN,
    "ic_stat_anongram_call.xml": XML_IC_STAT_ANONGRAM_CALL,
    "ic_stat_anongram_lock.xml": XML_IC_STAT_ANONGRAM_LOCK,
    "ic_stat_anongram_group.xml": XML_IC_STAT_ANONGRAM_GROUP,
    "ic_stat_anongram_burn.xml": XML_IC_STAT_ANONGRAM_BURN,
    "ic_launcher_foreground.xml": XML_IC_LAUNCHER_FOREGROUND,
    "ic_launcher_background.xml": XML_IC_LAUNCHER_BACKGROUND,
    "ic_launcher.xml": XML_IC_LAUNCHER,
    "ic_launcher_round.xml": XML_IC_LAUNCHER,
    "ic_splash_logo.xml": XML_IC_SPLASH_LOGO,
}

for name, content in drawables.items():
    path = os.path.join(DRAWABLE_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")
    print(f"Created Android Vector Drawable: {path}")

# ---------------------------------------------------------------------------
# 3. Master Raster PNG Generation for Android Mipmaps & App Store
# ---------------------------------------------------------------------------

# Full Master SVG 1024x1024 with obsidian gradient background
SVG_MASTER_1024 = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <defs>
    <radialGradient id="bgGlow" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#0e1726" />
      <stop offset="100%" stop-color="#05070a" />
    </radialGradient>
    <linearGradient id="shieldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#00f2fe" />
      <stop offset="50%" stop-color="#06b6d4" />
      <stop offset="100%" stop-color="#10b981" />
    </linearGradient>
    <linearGradient id="planeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#ffffff" />
      <stop offset="100%" stop-color="#c1fbe2" />
    </linearGradient>
    <filter id="shieldShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="16" stdDeviation="28" flood-color="#00f2fe" flood-opacity="0.45" />
    </filter>
  </defs>

  <!-- Background rounded squircle -->
  <rect width="1024" height="1024" rx="224" fill="url(#bgGlow)" />
  
  <!-- Outer glowing shield outline -->
  <path d="M512 880 S860 710 860 460 V180 L512 60 L164 180 v280 c0 250 348 420 348 420 z" 
        fill="#0b1320" stroke="url(#shieldGrad)" stroke-width="44" stroke-linecap="round" stroke-linejoin="round"
        filter="url(#shieldShadow)" />

  <!-- Inner cyber facet lines -->
  <path d="M512 840 S820 680 820 450 V210 L512 95 L204 210 v240 c0 230 308 390 308 390 z" 
        fill="none" stroke="#00f2fe" stroke-width="6" stroke-opacity="0.3" />

  <!-- Origami Stealth Airplane (Centerpiece) -->
  <!-- Left wing -->
  <polygon points="512,240 280,520 470,470" fill="#34d399" opacity="0.9" />
  <!-- Right wing (main body) -->
  <polygon points="512,240 744,520 512,720 470,470" fill="url(#planeGrad)" />
  <!-- Underwing shadow -->
  <polygon points="470,470 512,720 430,550" fill="#047857" opacity="0.8" />
  <!-- Center spine folds -->
  <line x1="512" y1="240" x2="470" y2="470" stroke="#065f46" stroke-width="12" stroke-linecap="round" />
  <line x1="470" y1="470" x2="512" y2="720" stroke="#065f46" stroke-width="12" stroke-linecap="round" />
</svg>"""

master_svg_path = os.path.join(ASSETS_DIR, "master_icon_1024.svg")
with open(master_svg_path, "w", encoding="utf-8") as f:
    f.write(SVG_MASTER_1024.strip() + "\n")
print(f"Created Master 1024 SVG: {master_svg_path}")

# Mipmap densities and sizes
MIPMAPS = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

master_png_1024 = os.path.join(ASSETS_DIR, "master_icon_1024.png")
play_store_512 = os.path.join(ASSETS_DIR, "play_store_512.png")

print("Rendering raster PNGs with ImageMagick convert...")
# 1. 1024x1024
subprocess.run(["convert", "-background", "none", "-density", "300", master_svg_path, master_png_1024], check=True)
print(f"Generated: {master_png_1024}")

# 2. 512x512 Play Store
subprocess.run(["convert", master_png_1024, "-resize", "512x512", play_store_512], check=True)
print(f"Generated: {play_store_512}")

# 3. Mipmaps
for folder, size in MIPMAPS.items():
    target_dir = os.path.join(RES_DIR, folder)
    os.makedirs(target_dir, exist_ok=True)
    out_square = os.path.join(target_dir, "ic_launcher.png")
    out_round = os.path.join(target_dir, "ic_launcher_round.png")
    
    # Square squircle
    subprocess.run(["convert", master_png_1024, "-resize", f"{size}x{size}", out_square], check=True)
    # Round icon
    subprocess.run(["convert", master_png_1024, "-resize", f"{size}x{size}", out_round], check=True)
    print(f"Generated {folder}: {size}x{size} px")

print("All branding assets generated successfully!")
