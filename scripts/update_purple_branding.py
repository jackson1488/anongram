#!/usr/bin/env python3
"""
Скрипт генерации фирменного фиолетового брендинга AnonGram:
- Векторные Adaptive Icons (ic_launcher_foreground.xml, ic_launcher_background.xml)
- Растровые Mipmaps всех плотностей (ic_launcher.png, ic_launcher_round.png)
- Системные drawable (ic_stat_anongram.xml, ic_splash_logo.xml)
- Мастер SVG 1024x1024 и PNG 512x512
"""

import os
import subprocess

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES_DIR = os.path.join(BASE_DIR, "frontend", "android", "app", "src", "main", "res")
DRAWABLE_DIR = os.path.join(RES_DIR, "drawable")
ASSETS_DIR = os.path.join(BASE_DIR, "frontend", "assets", "icons")

os.makedirs(DRAWABLE_DIR, exist_ok=True)
os.makedirs(ASSETS_DIR, exist_ok=True)

# -----------------------------------------------------------------------------
# 1. Мастер SVG 1024x1024: Неоновый фиолетовый щит с оригами-самолётиком
# -----------------------------------------------------------------------------
SVG_MASTER_1024 = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <defs>
    <!-- Глубокий обсидианово-фиолетовый фон с радиальным свечением -->
    <radialGradient id="bgGlow" cx="50%" cy="50%" r="60%">
      <stop offset="0%" stop-color="#281347" />
      <stop offset="50%" stop-color="#140924" />
      <stop offset="100%" stop-color="#07030D" />
    </radialGradient>

    <!-- Фирменный неоновый фиолетовый градиент щита -->
    <linearGradient id="shieldNeonGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#E9D5FF" />
      <stop offset="25%" stop-color="#C084FC" />
      <stop offset="70%" stop-color="#8B5CF6" />
      <stop offset="100%" stop-color="#6D28D9" />
    </linearGradient>

    <!-- Градиент самолётика: сияющий белый с лиловым отливом -->
    <linearGradient id="planeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" />
      <stop offset="60%" stop-color="#F3E8FF" />
      <stop offset="100%" stop-color="#D8B4FE" />
    </linearGradient>

    <!-- Неоновое свечение щита -->
    <filter id="neonShieldGlow" x="-30%" y="-30%" width="160%" height="160%">
      <feGaussianBlur stdDeviation="40" result="blur1" />
      <feGaussianBlur stdDeviation="16" result="blur2" />
      <feMerge>
        <feMergeNode in="blur1" />
        <feMergeNode in="blur2" />
        <feMergeNode in="SourceGraphic" />
      </feMerge>
    </filter>

    <!-- Мягкая тень под самолётиком -->
    <filter id="planeShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="-4" dy="12" stdDeviation="18" flood-color="#4C1D95" flood-opacity="0.8" />
    </filter>
  </defs>

  <!-- Закруглённая форма иконки (Squircle) -->
  <rect width="1024" height="1024" rx="230" fill="url(#bgGlow)" />

  <!-- Декоративные кибер-контуры фона -->
  <path d="M512 60 L912 280 V744 L512 964 L112 744 V280 Z" 
        fill="none" stroke="#8B5CF6" stroke-width="4" stroke-opacity="0.15" />
  <circle cx="512" cy="512" r="410" fill="none" stroke="#C084FC" stroke-width="3" stroke-dasharray="12 12" stroke-opacity="0.12" />

  <!-- Внешний неоновый щит AnonGram -->
  <path d="M512 870 C512 870 835 700 835 450 V210 L512 90 L189 210 V450 C189 700 512 870 512 870 Z" 
        fill="#120822" stroke="url(#shieldNeonGrad)" stroke-width="48" stroke-linecap="round" stroke-linejoin="round"
        filter="url(#neonShieldGlow)" />

  <!-- Внутренний тонкий светящийся контур щита -->
  <path d="M512 820 C512 820 790 660 790 440 V235 L512 135 L234 235 V440 C234 660 512 820 512 820 Z" 
        fill="none" stroke="#C084FC" stroke-width="8" stroke-opacity="0.4" />

  <!-- Самолётик (Telegram-стиль со стрелой в щите) -->
  <g filter="url(#planeShadow)">
    <!-- Левое крыло (с тенью) -->
    <polygon points="695,305 315,525 495,510" fill="#9333EA" opacity="0.95" />
    
    <!-- Правое крыло (основное, ярко освещённое) -->
    <polygon points="695,305 725,335 550,710 495,510" fill="url(#planeGrad)" />
    
    <!-- Нижний клапан / хвост -->
    <polygon points="495,510 550,710 445,565" fill="#6B21A8" opacity="0.9" />
    
    <!-- Линии сгиба оригами -->
    <line x1="695" y1="305" x2="495" y2="510" stroke="#581C87" stroke-width="8" stroke-linecap="round" />
    <line x1="495" y1="510" x2="550" y2="710" stroke="#581C87" stroke-width="8" stroke-linecap="round" />
  </g>
</svg>"""

# Круглая версия для ic_launcher_round
SVG_MASTER_ROUND_1024 = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <defs>
    <radialGradient id="bgGlowR" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#281347" />
      <stop offset="60%" stop-color="#140924" />
      <stop offset="100%" stop-color="#07030D" />
    </radialGradient>
    <linearGradient id="shieldNeonGradR" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#E9D5FF" />
      <stop offset="25%" stop-color="#C084FC" />
      <stop offset="70%" stop-color="#8B5CF6" />
      <stop offset="100%" stop-color="#6D28D9" />
    </linearGradient>
    <linearGradient id="planeGradR" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" />
      <stop offset="60%" stop-color="#F3E8FF" />
      <stop offset="100%" stop-color="#D8B4FE" />
    </linearGradient>
    <filter id="neonShieldGlowR" x="-30%" y="-30%" width="160%" height="160%">
      <feGaussianBlur stdDeviation="40" result="blur1" />
      <feGaussianBlur stdDeviation="16" result="blur2" />
      <feMerge>
        <feMergeNode in="blur1" />
        <feMergeNode in="blur2" />
        <feMergeNode in="SourceGraphic" />
      </feMerge>
    </filter>
    <filter id="planeShadowR" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="-4" dy="12" stdDeviation="18" flood-color="#4C1D95" flood-opacity="0.8" />
    </filter>
  </defs>

  <!-- Круглый фон -->
  <circle cx="512" cy="512" r="512" fill="url(#bgGlowR)" />
  <circle cx="512" cy="512" r="500" fill="none" stroke="#8B5CF6" stroke-width="4" stroke-opacity="0.3" />

  <!-- Внешний неоновый щит -->
  <path d="M512 870 C512 870 835 700 835 450 V210 L512 90 L189 210 V450 C189 700 512 870 512 870 Z" 
        fill="#120822" stroke="url(#shieldNeonGradR)" stroke-width="48" stroke-linecap="round" stroke-linejoin="round"
        filter="url(#neonShieldGlowR)" />

  <!-- Самолётик -->
  <g filter="url(#planeShadowR)">
    <polygon points="695,305 315,525 495,510" fill="#9333EA" opacity="0.95" />
    <polygon points="695,305 725,335 550,710 495,510" fill="url(#planeGradR)" />
    <polygon points="495,510 550,710 445,565" fill="#6B21A8" opacity="0.9" />
    <line x1="695" y1="305" x2="495" y2="510" stroke="#581C87" stroke-width="8" stroke-linecap="round" />
    <line x1="495" y1="510" x2="550" y2="710" stroke="#581C87" stroke-width="8" stroke-linecap="round" />
  </g>
</svg>"""

master_svg = os.path.join(ASSETS_DIR, "anongram_purple_master.svg")
with open(master_svg, "w", encoding="utf-8") as f:
    f.write(SVG_MASTER_1024)

master_round_svg = os.path.join(ASSETS_DIR, "anongram_purple_round_master.svg")
with open(master_round_svg, "w", encoding="utf-8") as f:
    f.write(SVG_MASTER_ROUND_1024)

print("Saved master SVGs")

# -----------------------------------------------------------------------------
# 2. Рендеринг Mipmaps всех размеров через ImageMagick
# -----------------------------------------------------------------------------
MIPMAPS = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

master_png = os.path.join(ASSETS_DIR, "anongram_purple_1024.png")
master_round_png = os.path.join(ASSETS_DIR, "anongram_purple_round_1024.png")

subprocess.run(["convert", "-background", "none", "-density", "300", master_svg, master_png], check=True)
subprocess.run(["convert", "-background", "none", "-density", "300", master_round_svg, master_round_png], check=True)

for folder, size in MIPMAPS.items():
    target_dir = os.path.join(RES_DIR, folder)
    os.makedirs(target_dir, exist_ok=True)
    out_square = os.path.join(target_dir, "ic_launcher.png")
    out_round = os.path.join(target_dir, "ic_launcher_round.png")

    subprocess.run(["convert", master_png, "-resize", f"{size}x{size}", out_square], check=True)
    subprocess.run(["convert", master_round_png, "-resize", f"{size}x{size}", out_round], check=True)
    print(f"Generated {folder}: {size}x{size}")

# -----------------------------------------------------------------------------
# 3. Android Adaptive Vector Drawables в фиолетовом стиле
# -----------------------------------------------------------------------------
XML_FOREGROUND = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group android:translateX="18" android:translateY="18" android:scaleX="3.0" android:scaleY="3.0">
        <!-- Cryptographic Shield with Neon Purple Accent -->
        <path
            android:pathData="M12,22 C12,22 20,18 20,12 V5 L12,2 L4,5 V12 C4,18 12,22 12,22 Z"
            android:strokeColor="#C084FC"
            android:strokeWidth="1.9"
            android:fillColor="#150B28"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"/>
        <!-- Inner Glow Contour -->
        <path
            android:pathData="M12,20 C12,20 18.5,16.5 18.5,11.5 V5.8 L12,3.3 L5.5,5.8 V11.5 C5.5,16.5 12,20 12,20 Z"
            android:strokeColor="#8B5CF6"
            android:strokeWidth="0.8"
            android:strokeAlpha="0.6"/>
        <!-- Stealth Origami Airplane with Neon Violet Wings -->
        <!-- Left Wing -->
        <path
            android:pathData="M16.5,7.5 L7,12 L11.5,11.8 Z"
            android:fillColor="#9333EA"/>
        <!-- Right Wing / Main Body -->
        <path
            android:pathData="M16.5,7.5 L17.2,8.2 L13,16.5 L11.5,11.8 Z"
            android:fillColor="#FFFFFF"/>
        <!-- Center Fold -->
        <path
            android:pathData="M11.5,11.8 L13,16.5 L10.5,13 Z"
            android:fillColor="#6B21A8"/>
        <path
            android:pathData="M7,12 L17,7.2 L13,16.8 L11.5,12 Z"
            android:strokeColor="#C084FC"
            android:strokeWidth="0.6"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"/>
    </group>
</vector>
"""

XML_BACKGROUND = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Deep AMOLED Obsidian Violet Background -->
    <path
        android:pathData="M0,0 H108 V108 H0 Z"
        android:fillColor="#0D0B14"/>
    <!-- Subtle Hex Glow Lines -->
    <path
        android:pathData="M54,14 L88,34 V74 L54,94 L20,74 V34 Z"
        android:strokeColor="#8B5CF6"
        android:strokeWidth="1.2"
        android:strokeAlpha="0.25"/>
    <path
        android:pathData="M54,26 L80,41 V71 L54,86 L28,71 V41 Z"
        android:strokeColor="#C084FC"
        android:strokeWidth="0.8"
        android:strokeAlpha="0.18"/>
</vector>
"""

XML_IC_STAT = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
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

XML_SPLASH_LOGO = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="192dp"
    android:height="192dp"
    android:viewportWidth="100"
    android:viewportHeight="100">
    <!-- Neon Purple Master Splash Shield -->
    <path
        android:pathData="M50,90 C50,90 84,73 84,48 V20 L50,8 L16,20 V48 C16,73 50,90 50,90 Z"
        android:strokeColor="#C084FC"
        android:strokeWidth="6"
        android:fillColor="#150B28"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
    <!-- Stealth Plane Left Wing -->
    <path
        android:pathData="M69,30 L31,52 L49,50 Z"
        android:fillColor="#9333EA"/>
    <!-- Stealth Plane Right Wing -->
    <path
        android:pathData="M69,30 L72,33 L54,71 L49,50 Z"
        android:fillColor="#FFFFFF"/>
</vector>
"""

with open(os.path.join(DRAWABLE_DIR, "ic_launcher_foreground.xml"), "w") as f:
    f.write(XML_FOREGROUND.strip() + "\n")

with open(os.path.join(DRAWABLE_DIR, "ic_launcher_background.xml"), "w") as f:
    f.write(XML_BACKGROUND.strip() + "\n")

with open(os.path.join(DRAWABLE_DIR, "ic_stat_anongram.xml"), "w") as f:
    f.write(XML_IC_STAT.strip() + "\n")

with open(os.path.join(DRAWABLE_DIR, "ic_splash_logo.xml"), "w") as f:
    f.write(XML_SPLASH_LOGO.strip() + "\n")

print("All purple vector drawables written successfully!")
