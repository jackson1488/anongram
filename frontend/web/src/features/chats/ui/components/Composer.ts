import { i18nStore } from '../../../../core/i18n/i18nStore';
import { clear, el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { inputT } from '../../input/i18n/InputI18n';
import { openVideoCircleRecorder, type VideoCircleRecorderHandle } from '../../input/video/VideoCircleRecordOverlay';
import { mediaRecorderController, type RecorderState } from '../../input/voice/MediaRecorderController';
import { createVoiceRecordingBar } from '../../input/voice/VoiceRecordingBar';

export interface ComposerOptions {
  initialText: string;
  onTextChange: (text: string) => void;
  onSend: (text: string) => void;
  onVoice?: () => void;
  onSendVoiceRecorded?: (dataUrl: string, durationSec: number) => void;
  onSendVideoCircleRecorded?: (dataUrl: string, durationSec: number) => void;
  onCancelEdit?: () => void;
  onToggleAttach: () => void;
  onToggleEmoji: () => void;
}

export interface ComposerHandle {
  root: HTMLElement;
  getText: () => string;
  setText: (text: string) => void;
  insertText: (text: string) => void;
  focus: () => void;
  setEditing: (editTitle: string, initialText: string) => void;
  clearEditing: () => void;
}

const HOLD_RECORD_DELAY_MS = 220;
const SLIDE_CANCEL_PX = -70;
const SLIDE_LOCK_PX = -70;

type ActionVisualMode = 'edit' | 'send' | 'video' | 'mic';

/**
 * Composer is built once per chat and updated in place so `<input class="ag-msg-input">`
 * is never recreated and never loses focus across typing, sending, editing, or breakpoints.
 */
export function createComposer(o: ComposerOptions): ComposerHandle {
  const t = i18nStore.t;
  const inpT = inputT();

  let isEditing = false;
  let editTitleText = '';
  let editPreviewText = '';

  let isCameraMode = false;
  let isHolding = false;
  let ownsVoiceSession = false;
  let finishingVoice = false;
  let voiceBarEl: HTMLElement | null = null;
  let activeVideoRecorder: VideoCircleRecorderHandle | null = null;
  let unsubRec: (() => void) | null = null;

  // Pointer gesture tracking for ActionButton (ActionButton.kt parity)
  let activePointerId: number | null = null;
  let startX = 0;
  let startY = 0;
  let downTimeMs = 0;
  let holdTimer: number | null = null;
  let clickSuppressTimer: number | null = null;
  let suppressNextClick = false;
  let recordingStarted = false;
  let gestureCancelled = false;
  let gestureLocked = false;

  const editBarSlot = el('div', {
    class: 'ag-composer__editbar',
    hidden: true,
  });

  const input = el('input', {
    type: 'text',
    class: 'ag-msg-input',
    placeholder: t.input.placeholder,
    value: o.initialText,
    autocomplete: 'off',
    enterkeyhint: 'send',
    'aria-label': t.input.placeholder,
  }) as HTMLInputElement;
  input.value = o.initialText;

  const attachBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn ag-composer__attach',
      title: t.input.attach,
      'aria-label': t.input.attach,
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: () => {
        if (!ownsVoiceSession) o.onToggleAttach();
      },
    },
    icons.attach(20),
  ) as HTMLButtonElement;

  const emojiBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn ag-composer__emoji',
      title: t.input.emoji,
      'aria-label': t.input.emoji,
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: () => {
        if (!ownsVoiceSession) o.onToggleEmoji();
      },
    },
    icons.smile(20),
  ) as HTMLButtonElement;

  const recBarSlot = el('div', {
    class: 'ag-voice-recbar ag-composer__recbar',
    hidden: true,
  });

  const innerPill = el(
    'div',
    { class: 'ag-composer__inner-pill' },
    emojiBtn,
    input,
    attachBtn,
    recBarSlot,
  );

  const actionIconSlot = el('span', {
    style: 'display:inline-flex;align-items:center;justify-content:center;pointer-events:none;',
  });

  const actionBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-send-fab',
      title: inpT.micDesc,
      'aria-label': inpT.micDesc,
      style: 'touch-action:none;user-select:none;-webkit-user-select:none;-webkit-touch-callout:none;',
      onMouseDown: (e: Event) => e.preventDefault(),
      onContextMenu: (e: Event) => e.preventDefault(),
    },
    actionIconSlot,
  ) as HTMLButtonElement;

  const lockIco = el('span', {
    class: 'ag-composer__lock-ico',
    style: 'display:inline-flex;align-items:center;justify-content:center;transition:color 0.14s ease;',
  }, icons.lock(16));
  const lockArrow = el('span', {
    class: 'ag-composer__lock-arrow',
    style: 'display:inline-flex;align-items:center;justify-content:center;color:var(--ag-text-secondary);',
  }, tgIcons.chevronUp(14));

  const lockTrack = el(
    'div',
    {
      class: 'ag-composer__lock-track',
      'aria-hidden': 'true',
      hidden: true,
      style: [
        'position:absolute',
        'z-index:8',
        'width:40px',
        'height:78px',
        'padding:9px 0',
        'border-radius:20px',
        'background:var(--ag-island-bg)',
        'border:1px solid var(--ag-island-border)',
        'color:var(--ag-text-primary)',
        'box-shadow:0 6px 20px var(--ag-shadow)',
        'backdrop-filter:blur(14px)',
        '-webkit-backdrop-filter:blur(14px)',
        'display:flex',
        'flex-direction:column',
        'align-items:center',
        'justify-content:space-between',
        'pointer-events:none',
      ].join(';'),
    },
    lockIco,
    lockArrow,
  );

  const pill = el('div', { class: 'ag-composer__pill' }, editBarSlot, innerPill, actionBtn);
  const root = el('div', { class: 'ag-composer', style: 'position:relative;' }, pill, lockTrack);

  const positionLockTrack = (ready: boolean) => {
    const rootRect = root.getBoundingClientRect();
    const btnRect = actionBtn.getBoundingClientRect();
    if (rootRect.width > 0 && btnRect.width > 0) {
      const left = Math.round(btnRect.left - rootRect.left + (btnRect.width - 40) / 2);
      const bottom = Math.round(rootRect.bottom - btnRect.top + 12);
      lockTrack.style.left = `${left}px`;
      lockTrack.style.right = '';
      lockTrack.style.bottom = `${bottom}px`;
    } else {
      lockTrack.style.left = '';
      lockTrack.style.right = '10px';
      lockTrack.style.bottom = 'calc(100% + 10px)';
    }
    lockIco.style.color = ready ? 'var(--ag-accent)' : 'var(--ag-text-primary)';
    lockTrack.style.borderColor = ready ? 'var(--ag-accent)' : 'var(--ag-island-border)';
  };

  const setLockTrackVisible = (visible: boolean, ready = false) => {
    lockTrack.hidden = !visible;
    lockTrack.style.display = visible ? 'flex' : 'none';
    lockTrack.classList.toggle('is-ready', visible && ready);
    if (visible) {
      positionLockTrack(ready);
    }
  };
  setLockTrackVisible(false);

  const syncVoiceBarMode = () => {
    if (!voiceBarEl) return;
    const recState = mediaRecorderController.getState();
    const locked = recState.locked;
    recBarSlot.classList.toggle('is-full', locked);
    const innerBar = voiceBarEl.querySelector('.ag-voice-rec-bar') as HTMLElement | null;
    const sendWrap = voiceBarEl.querySelector('.ag-voice-rec-send-wrap') as HTMLElement | null;
    const cancelBtn = voiceBarEl.querySelector('.ag-voice-rec-cancel') as HTMLButtonElement | null;
    const cur = inputT();

    if (innerBar) {
      if (locked) {
        innerBar.style.background = '';
        innerBar.style.border = '';
        innerBar.style.boxShadow = '';
      } else {
        innerBar.style.background = 'transparent';
        innerBar.style.border = 'none';
        innerBar.style.boxShadow = 'none';
      }
    }
    if (sendWrap) {
      sendWrap.style.display = locked ? '' : 'none';
    }
    if (cancelBtn) {
      cancelBtn.textContent = locked ? cur.cancelUpper : cur.slideToCancel;
      cancelBtn.style.textTransform = locked ? '' : 'none';
      cancelBtn.style.fontWeight = locked ? '' : '500';
      cancelBtn.style.fontSize = locked ? '' : '12.5px';
      cancelBtn.style.letterSpacing = locked ? '' : '0';
      cancelBtn.style.color = locked ? '' : 'var(--ag-text-muted)';
    }
  };

  const unmountVoiceBar = () => {
    if (voiceBarEl) {
      (voiceBarEl as unknown as { __cleanup?: () => void }).__cleanup?.();
      voiceBarEl = null;
    }
    clear(recBarSlot);
    recBarSlot.hidden = true;
    recBarSlot.classList.remove('is-full');
  };

  const detachRecSubscription = () => {
    unsubRec?.();
    unsubRec = null;
  };

  const ensureRecSubscription = () => {
    if (unsubRec) return;
    unsubRec = mediaRecorderController.subscribe((st: RecorderState) => {
      if (ownsVoiceSession && (!st.active || st.mode !== 'voice')) {
        ownsVoiceSession = false;
        unmountVoiceBar();
        detachRecSubscription();
      } else if (ownsVoiceSession) {
        syncVoiceBarMode();
      }
      syncState();
    });
  };

  const mountVoiceBar = () => {
    if (voiceBarEl) {
      syncVoiceBarMode();
      return;
    }
    voiceBarEl = createVoiceRecordingBar({
      onCancel: () => cancelVoiceRecording(),
      onSend: () => {
        void finishVoiceRecording();
      },
    });
    recBarSlot.replaceChildren(voiceBarEl);
    recBarSlot.hidden = false;
    syncVoiceBarMode();
  };

  let lastVisualMode: ActionVisualMode | null = null;
  let lastLang = i18nStore.language;

  const syncState = () => {
    const hasText = input.value.trim().length > 0;
    const canSend = hasText || isEditing;
    const recState = mediaRecorderController.getState();
    const isVoiceRec = ownsVoiceSession && recState.active && recState.mode === 'voice';
    const isLockedVoice = isVoiceRec && recState.locked;

    root.classList.toggle('is-has-text', hasText);
    root.classList.toggle('is-editing', isEditing);
    root.classList.toggle('is-camera-mode', !canSend && isCameraMode);
    root.classList.toggle('is-recording', isVoiceRec);
    root.classList.toggle('is-holding', isHolding);
    root.classList.toggle('is-locked', isLockedVoice);

    actionBtn.classList.toggle('is-camera-mode', !canSend && isCameraMode);
    actionBtn.classList.toggle('is-recording', isVoiceRec);
    actionBtn.classList.toggle('is-holding', isHolding);
    actionBtn.classList.toggle('is-locked', isLockedVoice);

    if (isLockedVoice) {
      actionBtn.style.opacity = '0';
      actionBtn.style.pointerEvents = 'none';
    } else {
      actionBtn.style.opacity = '';
      actionBtn.style.pointerEvents = '';
    }

    const mode: ActionVisualMode = isEditing
      ? 'edit'
      : hasText
        ? 'send'
        : isCameraMode
          ? 'video'
          : 'mic';

    const curLang = i18nStore.language;
    if (mode !== lastVisualMode || curLang !== lastLang) {
      lastVisualMode = mode;
      lastLang = curLang;
      const cur = i18nStore.t;
      const inp = inputT();
      const label =
        mode === 'edit'
          ? cur.common.save
          : mode === 'send'
            ? cur.input.send
            : mode === 'video'
              ? inp.videoRoundDesc
              : inp.micDesc;
      actionBtn.title = label;
      actionBtn.setAttribute('aria-label', label);
      const iconEl =
        mode === 'edit'
          ? icons.check(20)
          : mode === 'send'
            ? icons.send(20)
            : mode === 'video'
              ? icons.video(20)
              : icons.mic(20);
      actionIconSlot.replaceChildren(iconEl);
    }
  };

  const renderEditBar = () => {
    clear(editBarSlot);
    if (!isEditing) {
      editBarSlot.hidden = true;
      return;
    }
    editBarSlot.hidden = false;
    const cancelLabel = i18nStore.t.common.cancel;
    editBarSlot.append(
      el('span', { class: 'ag-composer__editbar-ico' }, icons.edit(16)),
      el(
        'div',
        { class: 'ag-composer__editbar-body' },
        el('strong', { class: 'ag-composer__editbar-title' }, editTitleText),
        el('span', { class: 'ag-composer__editbar-text' }, editPreviewText),
      ),
      el(
        'button',
        {
          type: 'button',
          class: 'ag-icon-btn ag-composer__editbar-close',
          title: cancelLabel,
          'aria-label': cancelLabel,
          onMouseDown: (e: Event) => e.preventDefault(),
          onClick: () => {
            input.value = '';
            clearEditing();
            o.onCancelEdit?.();
            input.focus();
          },
        },
        icons.close(16),
      ),
    );
  };

  function clearEditing(): void {
    if (!isEditing) return;
    isEditing = false;
    editTitleText = '';
    editPreviewText = '';
    renderEditBar();
    syncState();
  }

  function setEditing(editTitle: string, initialText: string): void {
    if (ownsVoiceSession) {
      cancelVoiceRecording();
    }
    isEditing = true;
    editTitleText = editTitle;
    editPreviewText = initialText;
    input.value = initialText;
    renderEditBar();
    syncState();
    input.focus();
    const len = input.value.length;
    try {
      input.setSelectionRange(len, len);
    } catch {
      // Ignore selection range errors on unsupported input states
    }
  }

  function clearHoldTimersAndVisuals(): void {
    if (holdTimer !== null) {
      window.clearTimeout(holdTimer);
      holdTimer = null;
    }
    isHolding = false;
    activePointerId = null;
    actionBtn.style.transform = '';
    setLockTrackVisible(false);
  }

  function cancelVoiceRecording(): void {
    clearHoldTimersAndVisuals();
    ownsVoiceSession = false;
    detachRecSubscription();
    mediaRecorderController.cancel();
    unmountVoiceBar();
    syncState();
    input.focus();
  }

  async function finishVoiceRecording(): Promise<void> {
    if (finishingVoice) return;
    const st = mediaRecorderController.getState();
    if (!st.active || st.mode !== 'voice') {
      ownsVoiceSession = false;
      detachRecSubscription();
      unmountVoiceBar();
      syncState();
      return;
    }
    finishingVoice = true;
    ownsVoiceSession = false;
    detachRecSubscription();
    unmountVoiceBar();
    clearHoldTimersAndVisuals();
    syncState();
    try {
      const result = await mediaRecorderController.stopAndSave();
      if (o.onSendVoiceRecorded) {
        o.onSendVoiceRecorded(result.dataUrl, result.durationSec);
      } else {
        o.onVoice?.();
      }
      input.focus();
    } finally {
      finishingVoice = false;
    }
  }

  function startVoiceRecordingSession(locked: boolean): void {
    if (activeVideoRecorder) return;
    ownsVoiceSession = true;
    ensureRecSubscription();
    void mediaRecorderController.startVoice();
    if (locked) {
      mediaRecorderController.lock();
    }
    mountVoiceBar();
    syncState();
  }

  function openVideoCircle(): void {
    if (activeVideoRecorder) return;
    if (ownsVoiceSession) cancelVoiceRecording();
    const handle = openVideoCircleRecorder({
      onSend: (dataUrl, durationSec) => {
        if (activeVideoRecorder === handle) activeVideoRecorder = null;
        syncState();
        o.onSendVideoCircleRecorded?.(dataUrl, durationSec);
        input.focus();
      },
      onCancel: () => {
        if (activeVideoRecorder === handle) activeVideoRecorder = null;
        syncState();
        input.focus();
      },
    });
    activeVideoRecorder = handle;
    syncState();
  }

  const submit = () => {
    const text = input.value;
    if (!text.trim() && !isEditing) return;
    input.value = '';
    if (isEditing) {
      clearEditing();
    } else {
      syncState();
    }
    o.onSend(text);
    input.focus();
  };

  syncState();

  input.addEventListener('input', () => {
    syncState();
    o.onTextChange(input.value);
  });

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
      e.preventDefault();
      submit();
      return;
    }
    if (e.key === 'Escape') {
      if (ownsVoiceSession) {
        e.preventDefault();
        e.stopPropagation();
        cancelVoiceRecording();
      } else if (isEditing) {
        e.preventDefault();
        e.stopPropagation();
        input.value = '';
        clearEditing();
        o.onCancelEdit?.();
      }
    }
  });

  const markSuppressClick = () => {
    suppressNextClick = true;
    if (clickSuppressTimer !== null) {
      window.clearTimeout(clickSuppressTimer);
    }
    clickSuppressTimer = window.setTimeout(() => {
      suppressNextClick = false;
      clickSuppressTimer = null;
    }, 350);
  };

  actionBtn.addEventListener('pointerdown', (e: PointerEvent) => {
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    e.preventDefault();
    if (activePointerId !== null) return;

    const hasText = input.value.trim().length > 0;
    const canSend = hasText || isEditing;
    const recState = mediaRecorderController.getState();
    const isVoiceRec = ownsVoiceSession && recState.active && recState.mode === 'voice';

    if (canSend) {
      activePointerId = e.pointerId;
      downTimeMs = performance.now();
      recordingStarted = false;
      gestureCancelled = false;
      gestureLocked = false;
      try {
        actionBtn.setPointerCapture(e.pointerId);
      } catch {
        // Ignore if pointer capture is unavailable
      }
      return;
    }

    if (isVoiceRec && recState.locked) {
      markSuppressClick();
      void finishVoiceRecording();
      return;
    }

    if (isVoiceRec || activeVideoRecorder) return;

    activePointerId = e.pointerId;
    startX = e.clientX;
    startY = e.clientY;
    downTimeMs = performance.now();
    recordingStarted = false;
    gestureCancelled = false;
    gestureLocked = false;
    isHolding = true;

    try {
      actionBtn.setPointerCapture(e.pointerId);
    } catch {
      // Ignore if pointer capture is unavailable
    }

    syncState();

    holdTimer = window.setTimeout(() => {
      holdTimer = null;
      if (activePointerId === null || gestureCancelled || gestureLocked) return;
      recordingStarted = true;
      navigator.vibrate?.(10);

      if (isCameraMode) {
        clearHoldTimersAndVisuals();
        syncState();
        openVideoCircle();
        return;
      }

      setLockTrackVisible(true, false);
      startVoiceRecordingSession(false);
    }, HOLD_RECORD_DELAY_MS);
  });

  actionBtn.addEventListener('pointermove', (e: PointerEvent) => {
    if (activePointerId === null || e.pointerId !== activePointerId) return;
    const hasText = input.value.trim().length > 0;
    if (hasText || isEditing) return;

    const dx = e.clientX - startX;
    const dy = e.clientY - startY;

    if (!recordingStarted) {
      if (dx < SLIDE_CANCEL_PX) {
        gestureCancelled = true;
        clearHoldTimersAndVisuals();
        syncState();
      }
      return;
    }

    if (isCameraMode) return;

    const clampedX = Math.max(-140, Math.min(0, dx));
    const clampedY = Math.max(-96, Math.min(0, dy));
    actionBtn.style.transform = `translate3d(${clampedX}px, ${clampedY}px, 0) scale(1.12)`;
    setLockTrackVisible(true, dy <= -55);

    if (!gestureCancelled && dx < SLIDE_CANCEL_PX) {
      gestureCancelled = true;
      navigator.vibrate?.(12);
      try {
        actionBtn.releasePointerCapture(e.pointerId);
      } catch {
        // Ignore capture release error
      }
      cancelVoiceRecording();
    } else if (!gestureLocked && dy < SLIDE_LOCK_PX) {
      gestureLocked = true;
      navigator.vibrate?.(12);
      try {
        actionBtn.releasePointerCapture(e.pointerId);
      } catch {
        // Ignore capture release error
      }
      clearHoldTimersAndVisuals();
      mediaRecorderController.lock();
      syncVoiceBarMode();
      syncState();
    }
  });

  const handlePointerFinish = (e: PointerEvent) => {
    if (activePointerId === null || e.pointerId !== activePointerId) return;
    const pointerId = e.pointerId;
    const hasText = input.value.trim().length > 0;
    const canSend = hasText || isEditing;
    const elapsed = performance.now() - downTimeMs;
    const dx = e.clientX - startX;
    const wasStarted = recordingStarted;
    const wasCancelled = gestureCancelled;
    const wasLocked = gestureLocked;

    clearHoldTimersAndVisuals();
    try {
      if (actionBtn.hasPointerCapture?.(pointerId)) {
        actionBtn.releasePointerCapture(pointerId);
      }
    } catch {
      // Ignore capture release error
    }

    markSuppressClick();

    if (canSend) {
      if (e.type === 'pointerup') {
        const r = actionBtn.getBoundingClientRect();
        const inside =
          e.clientX >= r.left - 12 &&
          e.clientX <= r.right + 12 &&
          e.clientY >= r.top - 12 &&
          e.clientY <= r.bottom + 12;
        if (inside) {
          submit();
        } else {
          syncState();
        }
      } else {
        syncState();
      }
      return;
    }

    if (e.type === 'pointercancel') {
      if (wasStarted && !isCameraMode && !wasCancelled && !wasLocked) {
        cancelVoiceRecording();
      } else {
        syncState();
      }
      return;
    }

    if (wasCancelled || wasLocked) {
      syncState();
      return;
    }

    if (!wasStarted && elapsed < HOLD_RECORD_DELAY_MS) {
      isCameraMode = !isCameraMode;
      syncState();
      return;
    }

    if (wasStarted && !isCameraMode) {
      if (dx < SLIDE_CANCEL_PX) {
        cancelVoiceRecording();
      } else {
        void finishVoiceRecording();
      }
    }
  };

  actionBtn.addEventListener('pointerup', handlePointerFinish);
  actionBtn.addEventListener('pointercancel', handlePointerFinish);

  // Double-tap / double-click fallback when input is empty
  actionBtn.addEventListener('dblclick', (e: MouseEvent) => {
    const hasText = input.value.trim().length > 0;
    if (hasText || isEditing) return;
    const st = mediaRecorderController.getState();
    if (st.active || activeVideoRecorder) return;
    e.preventDefault();
    if (isCameraMode) {
      openVideoCircle();
    } else {
      startVoiceRecordingSession(true);
    }
  });

  // Keyboard & accessibility click handler (Enter / Space / assistive tech)
  actionBtn.addEventListener('click', () => {
    if (suppressNextClick) {
      suppressNextClick = false;
      return;
    }
    const hasText = input.value.trim().length > 0;
    if (hasText || isEditing) {
      submit();
      return;
    }
    const st = mediaRecorderController.getState();
    if (ownsVoiceSession && st.active && st.mode === 'voice') {
      void finishVoiceRecording();
      return;
    }
    if (isCameraMode) {
      openVideoCircle();
    } else {
      startVoiceRecordingSession(true);
    }
  });

  const unsubI18n = i18nStore.subscribe(() => {
    const cur = i18nStore.t;
    input.placeholder = cur.input.placeholder;
    input.setAttribute('aria-label', cur.input.placeholder);
    attachBtn.title = cur.input.attach;
    attachBtn.setAttribute('aria-label', cur.input.attach);
    emojiBtn.title = cur.input.emoji;
    emojiBtn.setAttribute('aria-label', cur.input.emoji);
    if (isEditing) renderEditBar();
    syncState();
    if (ownsVoiceSession) {
      queueMicrotask(syncVoiceBarMode);
    }
  });

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubI18n();
    detachRecSubscription();
    clearHoldTimersAndVisuals();
    if (clickSuppressTimer !== null) {
      window.clearTimeout(clickSuppressTimer);
      clickSuppressTimer = null;
    }
    if (ownsVoiceSession) {
      ownsVoiceSession = false;
      mediaRecorderController.cancel();
    }
    unmountVoiceBar();
    if (activeVideoRecorder) {
      activeVideoRecorder.close();
      activeVideoRecorder = null;
    }
  };

  return {
    root,
    getText: () => input.value,
    setText: (text) => {
      input.value = text;
      syncState();
    },
    insertText: (text) => {
      const s = input.selectionStart ?? input.value.length;
      const e = input.selectionEnd ?? s;
      input.value = input.value.slice(0, s) + text + input.value.slice(e);
      input.setSelectionRange(s + text.length, s + text.length);
      syncState();
      o.onTextChange(input.value);
      input.focus();
    },
    focus: () => input.focus(),
    setEditing,
    clearEditing,
  };
}
