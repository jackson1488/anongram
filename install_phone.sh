#!/usr/bin/env bash
# install_phone.sh — Скачивание свежего APK с GitHub CI в корень проекта и установка на смартфон по ADB в 1 клик.
set -euo pipefail

REPO="jackson1488/anongram"
WORKFLOW_NAME="Build AnonGram Android APK (Cloud CI)"
PKG="com.anongram.app"
ROOT="$(cd "$(dirname "$0")" && pwd)"
ROOT_APK="$ROOT/AnonGram.apk"
TEMP_DIR="$ROOT/_tmp_phone_install"

SKIP_DOWNLOAD=0
for arg in "$@"; do
  case "$arg" in
    --skip-download|-s) SKIP_DOWNLOAD=1 ;;
  esac
done

notify() {
  local title="$1" msg="$2"
  echo "[$title] $msg"
  if command -v notify-send >/dev/null 2>&1; then
    notify-send -a "AnonGram" "$title" "$msg" 2>/dev/null || true
  fi
}

error_exit() {
  local msg="$1"
  echo "❌ Ошибка: $msg" >&2
  if [ -t 1 ]; then
    :
  elif command -v zenity >/dev/null 2>&1; then
    zenity --error --text="$msg" --title="AnonGram Install Error" 2>/dev/null || true
  fi
  exit 1
}

# 1. Скачивание свежего APK с GitHub (если не пропущен флаг)
if [ $SKIP_DOWNLOAD -eq 0 ]; then
  echo "📡 Поиск последней успешной сборки на GitHub Actions..."
  TOKEN="$(sed -E 's#https://[^:]+:([^@]+)@.*#\1#' ~/.git-credentials 2>/dev/null || true)"
  [ -n "$TOKEN" ] || error_exit "Токен GitHub не найден в ~/.git-credentials"

  api() { curl -sL -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" "$@"; }

  RUN_INFO="$(api "https://api.github.com/repos/$REPO/actions/runs?per_page=10&status=completed" | python3 -c '
import sys, json
data = json.load(sys.stdin)
wf_name = "'"$WORKFLOW_NAME"'"
runs = [r for r in data.get("workflow_runs", []) if r.get("name") == wf_name and r.get("conclusion") == "success"]
if runs:
    latest = runs[0]
    print(str(latest.get("id")) + " " + str(latest.get("run_number")))
else:
    print("- -")
')"

  read -r RUN_ID RUN_NUMBER <<< "$RUN_INFO"
  [ "$RUN_ID" != "-" ] || error_exit "Не найдено успешных сборок '$WORKFLOW_NAME' в репозитории."

  echo "⬇️ Найдена сборка #$RUN_NUMBER (Run ID: $RUN_ID). Скачиваем APK..."
  AID="$(api "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/artifacts" | python3 -c '
import sys, json
data = json.load(sys.stdin)
arts = data.get("artifacts", [])
print(arts[0]["id"] if arts else "")
')"
  [ -n "$AID" ] || error_exit "В сборке #$RUN_NUMBER нет артефактов APK."

  rm -rf "$TEMP_DIR"
  mkdir -p "$TEMP_DIR"
  api "https://api.github.com/repos/$REPO/actions/artifacts/$AID/zip" -o "$TEMP_DIR/artifact.zip"
  unzip -qo "$TEMP_DIR/artifact.zip" -d "$TEMP_DIR/extracted"

  NEW_APK="$(find "$TEMP_DIR/extracted" -name '*.apk' | head -1)"
  [ -n "$NEW_APK" ] || error_exit "В архиве артефакта не найден .apk файл."

  # Удаляем старый APK в корне и кладем новый
  echo "🗑️ Удаляем старый APK в корне..."
  rm -f "$ROOT_APK"
  cp -f "$NEW_APK" "$ROOT_APK"
  rm -rf "$TEMP_DIR"
  echo "✅ Свежий APK сохранён в корень: $ROOT_APK"
fi

[ -f "$ROOT_APK" ] || error_exit "Файл $ROOT_APK не найден."

# 2. Определение версии APK
AAPT2="$(find "$HOME/Android/Sdk/build-tools" -name aapt2 2>/dev/null | sort | tail -1 || true)"
VERSION_INFO="AnonGram"
if [ -n "$AAPT2" ] && [ -x "$AAPT2" ]; then
  APK_VER="$("$AAPT2" dump badging "$ROOT_APK" 2>/dev/null | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1)"
  APK_CODE="$("$AAPT2" dump badging "$ROOT_APK" 2>/dev/null | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" | head -1)"
  [ -n "$APK_VER" ] && VERSION_INFO="AnonGram v$APK_VER (build $APK_CODE)"
fi
echo "📦 Готов к установке: $VERSION_INFO ($ROOT_APK)"

# 3. Поиск подключенного телефона через ADB
echo "📱 Поиск подключенного Android смартфона..."

# Авто-обнаружение по mDNS (Wi-Fi ADB), если телефон ещё не подключен в adb devices
EXISTING_PHONE="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}' | grep -v 'emulator\|192.168.240' || true)"
if [ -z "$EXISTING_PHONE" ]; then
  if command -v avahi-browse >/dev/null 2>&1; then
    MDNS_ADDR="$(avahi-browse -r -t _adb-tls-connect._tcp 2>/dev/null | awk -F'[][]' '/address = / {addr=$2} /port = / {port=$2; if(addr && port) {print addr":"port; exit}}')"
    if [ -n "$MDNS_ADDR" ]; then
      echo "🔗 Найдено устройство в Wi-Fi сети ($MDNS_ADDR). Автоподключение..."
      adb connect "$MDNS_ADDR" >/dev/null 2>&1 || true
    fi
  fi
fi

RAW_DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
[ -n "$RAW_DEVICES" ] || error_exit "Нет подключенных устройств по ADB. Включите отладку по USB/Wi-Fi на телефоне и подключите его."

# Выбираем устройства для установки
DEVICES_TO_INSTALL=()
for dev in $RAW_DEVICES; do
  if [[ "$dev" =~ \._adb-tls-connect\._tcp ]]; then
    continue
  fi
  DEVICES_TO_INSTALL+=("$dev")
done

if [ ${#DEVICES_TO_INSTALL[@]} -eq 0 ]; then
  DEVICES_TO_INSTALL+=("$(echo "$RAW_DEVICES" | head -1)")
fi

ANY_SUCCESS=0
for TARGET_DEV in "${DEVICES_TO_INSTALL[@]}"; do
  MODEL="$(adb -s "$TARGET_DEV" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
  DEVICE_NAME="$MODEL ($TARGET_DEV)"
  echo "🎯 Устройство: $DEVICE_NAME"
  notify "AnonGram" "Установка $VERSION_INFO на $DEVICE_NAME..."

  echo "🚀 Установка $ROOT_APK на $TARGET_DEV..."
  INSTALL_RES="$(adb -s "$TARGET_DEV" install -r -d "$ROOT_APK" 2>&1 || true)"
  echo "$INSTALL_RES"

  if echo "$INSTALL_RES" | grep -q "Success"; then
    echo "🎉 Успешно установлено на $DEVICE_NAME!"
    adb -s "$TARGET_DEV" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
    notify "AnonGram установлен!" "Версия $VERSION_INFO запущена на $DEVICE_NAME"
    ANY_SUCCESS=1
  elif echo "$INSTALL_RES" | grep -q "INSTALL_FAILED_USER_RESTRICTED"; then
    echo "⚠️ Установка на $DEVICE_NAME заблокирована: включите 'Установка через USB' в настройках разработчика."
  else
    echo "❌ Ошибка при установке на $DEVICE_NAME: $INSTALL_RES"
  fi
done

if [ $ANY_SUCCESS -eq 1 ]; then
  echo "✅ Установка завершена!"
else
  error_exit "Не удалось установить ни на одно устройство."
fi

