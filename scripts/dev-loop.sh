#!/usr/bin/env bash
# dev-loop.sh — «правка -> APK на Waydroid» без нагрузки на ПК.
# Ждёт облачную сборку коммита, при ошибке показывает строки компилятора,
# при успехе скачивает APK, ставит поверх, запускает и снимает скриншот.
#
# Использование:
#   scripts/dev-loop.sh               # для HEAD
#   scripts/dev-loop.sh <sha>         # для конкретного коммита
#   scripts/dev-loop.sh --wipe        # при конфликте подписи удалить старую версию
#   scripts/dev-loop.sh --no-launch   # только установка
set -uo pipefail

REPO="jackson1488/anongram"
WORKFLOW_NAME="Build AnonGram Android APK (Cloud CI)"
PKG="com.anongram.app"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/_tmp_plans"
APK_DIR="$ROOT/build-artifacts"
WIPE=0; LAUNCH=1; SHA=""

for a in "$@"; do
  case "$a" in
    --wipe) WIPE=1 ;;
    --no-launch) LAUNCH=0 ;;
    *) SHA="$a" ;;
  esac
done
[ -n "$SHA" ] || SHA="HEAD"
SHA="$(git -C "$ROOT" rev-parse "$SHA")" || { echo "❌ неизвестный коммит"; exit 2; }

TOKEN="$(sed -E 's#https://[^:]+:([^@]+)@.*#\1#' ~/.git-credentials 2>/dev/null)"
[ -n "$TOKEN" ] || { echo "❌ нет токена в ~/.git-credentials"; exit 2; }
api() { curl -sL -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" "$@"; }
mkdir -p "$OUT" "$APK_DIR"

echo "🔎 Жду сборку коммита ${SHA:0:7} ..."
RUN_ID=""; STATUS=""; CONCL=""
for i in $(seq 1 90); do   # до ~15 минут
  read -r RUN_ID STATUS CONCL < <(api "https://api.github.com/repos/$REPO/actions/runs?head_sha=$SHA&per_page=20" | python3 -c '
import sys, json
n = sys.argv[1]
d = json.load(sys.stdin).get("workflow_runs", [])
r = [x for x in d if x["name"] == n]
if not r: print("- none none")
else:
    r = max(r, key=lambda x: x["run_number"])
    print(r["id"], r["status"], r["conclusion"])
' "$WORKFLOW_NAME")
  [ "$STATUS" = "completed" ] && break
  [ "$RUN_ID" = "-" ] && [ $i -gt 18 ] && { echo "⚠️ Сборка для этого коммита не запускалась (path-фильтр?). Правки вне frontend/android не собирают APK."; exit 3; }
  sleep 10
done
[ "$STATUS" = "completed" ] || { echo "⏱️ Таймаут ожидания (статус: $STATUS)"; exit 4; }

if [ "$CONCL" != "success" ]; then
  echo "❌ Сборка упала ($CONCL): https://github.com/$REPO/actions/runs/$RUN_ID"
  JOB_ID="$(api "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/jobs" | python3 -c 'import sys,json; print(json.load(sys.stdin)["jobs"][0]["id"])')"
  api "https://api.github.com/repos/$REPO/actions/jobs/$JOB_ID/logs" | tr -d '\r' \
    | grep -E '(^|Z )e: |error:|::error::|FAILED|What went wrong' | sed -E 's/^[0-9T:.-]+Z //; s#file:///home/runner/work/anongram/anongram/##' \
    | tee "$OUT/last_errors.txt" | head -60
  echo "(полный список: $OUT/last_errors.txt) — по правилу показываю ошибки, не чиню."
  exit 1
fi

echo "✅ Сборка зелёная (run $RUN_ID). Скачиваю APK ..."
AID="$(api "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/artifacts" | python3 -c 'import sys,json; print(json.load(sys.stdin)["artifacts"][0]["id"])')"
rm -rf "$APK_DIR/latest"; mkdir -p "$APK_DIR/latest"
api "https://api.github.com/repos/$REPO/actions/artifacts/$AID/zip" -o "$APK_DIR/latest.zip" && unzip -qo "$APK_DIR/latest.zip" -d "$APK_DIR/latest"
APK="$(find "$APK_DIR/latest" -name '*.apk' | head -1)"
[ -n "$APK" ] || { echo "❌ APK не найден в артефакте"; exit 5; }

# Waydroid: запустить сессию, если остановлена
if ! waydroid status 2>/dev/null | grep -q 'Session:.*RUNNING'; then
  echo "▶️ Запускаю сессию Waydroid ..."
  nohup waydroid session start >/dev/null 2>&1 &
  for i in $(seq 1 30); do sleep 2; waydroid status 2>/dev/null | grep -q 'Session:.*RUNNING' && break; done
  sleep 6
fi

installed_version() { sudo waydroid shell -- dumpsys package "$PKG" 2>/dev/null | grep -m1 versionName | tr -d ' \r' | cut -d= -f2; }
AAPT2="$(find "$HOME/Android/Sdk/build-tools" -name aapt2 2>/dev/null | sort | tail -1)"
APK_VER=""
[ -n "$AAPT2" ] && APK_VER="$("$AAPT2" dump badging "$APK" 2>/dev/null | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1)"

# `waydroid app install` молча «успешен» даже при отказе, поэтому ставим через pm.
SHARED="$HOME/.local/share/waydroid/data/local/tmp"
pm_install() {
  sudo cp "$APK" "$SHARED/ag.apk" && sudo waydroid shell -- chmod 644 /data/local/tmp/ag.apk
  sudo waydroid shell -- pm install -r -d /data/local/tmp/ag.apk 2>&1 | tr -d '\r' | tail -3
}

BEFORE="$(installed_version)"
echo "📦 Установка поверх (было: ${BEFORE:-нет}, ставим: ${APK_VER:-?}) ..."
RES="$(pm_install)"
if echo "$RES" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
  if [ $WIPE -eq 1 ]; then
    echo "♻️ Другая подпись у установленной версии — удаляю её и ставлю заново (данные приложения сотрутся) ..."
    sudo waydroid shell -- pm uninstall "$PKG" >/dev/null 2>&1
    RES="$(pm_install)"
  else
    echo "⚠️ Установленная версия подписана другим ключом. Повтори с --wipe (сотрёт данные приложения в Waydroid)."
    exit 6
  fi
fi
echo "   pm: $RES"
AFTER="$(installed_version)"
echo "   сейчас: ${AFTER:-нет}"
if [ -n "$APK_VER" ] && [ "$AFTER" != "$APK_VER" ]; then
  echo "❌ Установилась не та версия (ожидали $APK_VER)."; exit 7
fi

if [ $LAUNCH -eq 1 ]; then
  waydroid app launch "$PKG" >/dev/null 2>&1; sleep 5
  rm -f "$OUT/last.png"; sudo waydroid shell -- screencap -p 2>/dev/null > "$OUT/last.png"
  echo "📸 Скриншот: $OUT/last.png"
fi
echo "🏁 Готово: ${AFTER:-?}"
