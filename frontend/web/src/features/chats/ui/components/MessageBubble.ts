import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { audioPlayer } from '../../../player/audioPlayer';
import { chatStore } from '../../chatStore';
import { chatScreenT } from '../../i18n';
import { chatActionsT } from '../../i18n/ChatActionsI18n';
import { openExternalLink } from '../../links/InAppBrowserHost';
import type { ChatMessage, MessageAttachment } from '../../models';
import { localTranslator } from '../../tools/translate/LocalMessageTranslator';
import '../../tools/translate/translate.css';
import { openDocumentViewer, openMediaViewer } from '../../viewer/MediaViewerOverlay';

export interface BubbleContext {
  chatId: string;
  selectionMode: boolean;
  selected: boolean;
  isDesktop: boolean;
  isGroupFirst?: boolean;
  isGroupLast?: boolean;
  onContextMenu: (msg: ChatMessage, x: number, y: number) => void;
  onOpenMenu?: (msg: ChatMessage, e: Event) => void;
  onToggleSelect: (msg: ChatMessage) => void;
  onEnterSelect: (msg: ChatMessage) => void;
  onSwipeReply: (msg: ChatMessage) => void;
  onReplyJump: (messageId: string) => void;
  onTranscribeVoice?: (msg: ChatMessage) => void;
  onOpenLink?: (url: string) => void;
}

const SWIPE_TRIGGER = 56;
const QUICK_TAP_MAX_MS = 380;
const LONG_PRESS_MS = 420;
const TAP_SLOP_PX = 10;

const INTERACTIVE_SELECTOR = [
  'button',
  'a[href]',
  '.ag-att-audio',
  '.ag-att-photo',
  '.ag-att-card',
  '.ag-att-video-circle',
  '.ag-vc-bubble',
  '.ag-voice-stt-btn',
  '.ag-reaction-chip',
  '.ag-reply-strip',
].join(',');

type SyncFn = () => void;
const activeTranslatorRows = new Map<HTMLElement, SyncFn>();
let translatorSubscribed = false;
let pruneScheduled = false;

function scheduleRowPrune(): void {
  if (pruneScheduled) return;
  pruneScheduled = true;
  window.setTimeout(() => {
    pruneScheduled = false;
    for (const node of activeTranslatorRows.keys()) {
      if (!node.isConnected) {
        activeTranslatorRows.delete(node);
      }
    }
  }, 0);
}

function registerTranslatorSync(row: HTMLElement, sync: SyncFn): void {
  if (!translatorSubscribed) {
    translatorSubscribed = true;
    localTranslator.subscribe(() => {
      for (const [node, fn] of activeTranslatorRows) {
        if (!node.isConnected) {
          activeTranslatorRows.delete(node);
          continue;
        }
        fn();
      }
    });
  }
  activeTranslatorRows.set(row, sync);
  scheduleRowPrune();
}

function isInteractiveTarget(target: EventTarget | null): boolean {
  return target instanceof Element && Boolean(target.closest(INTERACTIVE_SELECTOR));
}

function formatDuration(sec?: number): string {
  const total = Math.max(0, Math.round(sec ?? 0));
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function appendTextWithLinks(
  container: HTMLElement,
  text: string,
  selectionMode: boolean,
  onSelect: () => void,
  onOpenLink?: (url: string) => void,
): void {
  const urlRe = /https?:\/\/[^\s<]+/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = urlRe.exec(text)) !== null) {
    if (match.index > lastIndex) {
      container.appendChild(document.createTextNode(text.slice(lastIndex, match.index)));
    }

    let url = match[0];
    let trailing = '';
    while (url.length > 8 && /[.,!?;:)\]]$/.test(url)) {
      const lastCh = url[url.length - 1];
      if (lastCh === ')' && url.includes('(')) break;
      trailing = lastCh + trailing;
      url = url.slice(0, -1);
    }

    const targetUrl = url;
    const anchor = el(
      'a',
      {
        href: '#',
        class: 'ag-msg-link',
        style: 'color:inherit;text-decoration:underline;text-underline-offset:2px;word-break:break-all;',
        onClick: (e: Event) => {
          e.preventDefault();
          e.stopPropagation();
          if (selectionMode) {
            onSelect();
            return;
          }
          if (onOpenLink) onOpenLink(targetUrl);
          else openExternalLink(targetUrl);
        },
      },
      targetUrl,
    );
    container.appendChild(anchor);

    if (trailing) {
      container.appendChild(document.createTextNode(trailing));
    }
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < text.length) {
    container.appendChild(document.createTextNode(text.slice(lastIndex)));
  }
}

function renderVideoCircleAttachment(
  msg: ChatMessage,
  att: MessageAttachment,
  ctx: BubbleContext,
): HTMLElement {
  const ca = chatActionsT();
  const durLabel = formatDuration(att.durationSec ?? 14);

  const circleBox = el('div', {
    class: 'ag-vc-bubble',
    style: [
      'position:relative',
      att.previewUrl ? 'width:176px;height:176px' : 'width:148px;height:148px',
      'border-radius:9999px',
      'overflow:hidden',
      'border:2px solid var(--ag-accent)',
      'background:color-mix(in srgb, var(--ag-accent) 12%, var(--ag-surface))',
      'display:flex',
      'align-items:center',
      'justify-content:center',
      'cursor:pointer',
      'flex-shrink:0',
    ].join(';'),
  });

  if (att.previewUrl) {
    const video = el('video', {
      class: 'ag-vc-video',
      src: att.previewUrl,
      playsinline: true,
      muted: true,
      loop: true,
      autoplay: true,
      style: 'width:100%;height:100%;object-fit:cover;display:block;border-radius:9999px;',
    }) as HTMLVideoElement;
    video.muted = true;
    video.playsInline = true;
    video.loop = true;

    const toggleIcon = icons.pause(12);
    const playToggleBtn = el(
      'button',
      {
        type: 'button',
        class: 'ag-vc-play-btn',
        title: ca.videoCircle,
        'aria-label': ca.videoCircle,
        style: [
          'position:absolute',
          'right:8px',
          'bottom:8px',
          'width:26px',
          'height:26px',
          'border-radius:9999px',
          'background:var(--ag-island-bg)',
          'border:1px solid var(--ag-island-border)',
          'color:var(--ag-text-primary)',
          'display:inline-flex',
          'align-items:center',
          'justify-content:center',
          'backdrop-filter:blur(8px)',
          '-webkit-backdrop-filter:blur(8px)',
        ].join(';'),
        onClick: (e: Event) => {
          e.stopPropagation();
          if (ctx.selectionMode) {
            ctx.onToggleSelect(msg);
            return;
          }
          if (video.paused) {
            void video.play().catch(() => {});
          } else {
            video.pause();
          }
        },
      },
      toggleIcon,
    );

    const syncIcon = () => {
      playToggleBtn.replaceChildren(video.paused ? icons.play(12) : icons.pause(12));
    };
    video.addEventListener('play', syncIcon);
    video.addEventListener('pause', syncIcon);
    void video.play().catch(() => syncIcon());

    const durBadge = el(
      'span',
      {
        style: [
          'position:absolute',
          'left:8px',
          'bottom:8px',
          'padding:2px 7px',
          'border-radius:9999px',
          'background:var(--ag-island-bg)',
          'border:1px solid var(--ag-island-border)',
          'color:var(--ag-text-primary)',
          'font-size:10.5px',
          'font-weight:700',
          'pointer-events:none',
        ].join(';'),
      },
      durLabel,
    );

    circleBox.addEventListener('click', (e: Event) => {
      e.stopPropagation();
      if (ctx.selectionMode) {
        ctx.onToggleSelect(msg);
        return;
      }
      if (video.paused) {
        void video.play().catch(() => {});
      }
      openMediaViewer(att, undefined, undefined, msg);
    });

    circleBox.append(video, durBadge, playToggleBtn);
  } else {
    const fallbackBadge = el(
      'div',
      {
        style: [
          'display:flex',
          'flex-direction:column',
          'align-items:center',
          'justify-content:center',
          'gap:6px',
          'padding:12px',
          'text-align:center',
          'color:inherit',
        ].join(';'),
      },
      el(
        'span',
        {
          style: [
            'width:42px',
            'height:42px',
            'border-radius:9999px',
            'background:var(--ag-accent-soft)',
            'color:var(--ag-accent)',
            'display:inline-flex',
            'align-items:center',
            'justify-content:center',
          ].join(';'),
        },
        icons.video(20),
      ),
      el(
        'span',
        { style: 'font-size:11.5px;font-weight:600;line-height:1.2;' },
        att.title || ca.videoCircle,
      ),
      el(
        'span',
        {
          style: [
            'padding:2px 8px',
            'border-radius:9999px',
            'background:var(--ag-accent-soft)',
            'color:var(--ag-accent)',
            'font-size:10.5px',
            'font-weight:700',
          ].join(';'),
        },
        durLabel,
      ),
    );

    circleBox.addEventListener('click', (e: Event) => {
      e.stopPropagation();
      if (ctx.selectionMode) {
        ctx.onToggleSelect(msg);
        return;
      }
      openMediaViewer(att, undefined, undefined, msg);
    });

    circleBox.appendChild(fallbackBadge);
  }

  return el(
    'div',
    {
      class: 'ag-att-video-circle',
      style: 'display:flex;flex-direction:column;align-items:center;gap:4px;padding:2px 0;',
    },
    circleBox,
  );
}

function renderAttachment(
  msg: ChatMessage,
  att: MessageAttachment,
  ctx: BubbleContext,
): { node: HTMLElement; syncVoice?: () => void } {
  if (att.type === 'video_circle') {
    return { node: renderVideoCircleAttachment(msg, att, ctx) };
  }

  const isAudio = att.type === 'voice' || att.type === 'music';
  if (isAudio) {
    const playingThis = audioPlayer.currentTrack?.id === msg.id && audioPlayer.playing;
    const durSec = att.durationSec ?? 24;
    const audioRow = el(
      'div',
      {
        class: 'ag-att-audio',
        style: att.type === 'voice' ? 'min-width:206px;' : undefined,
        onClick: (e: Event) => {
          e.stopPropagation();
          if (ctx.selectionMode) {
            ctx.onToggleSelect(msg);
            return;
          }
          audioPlayer.toggleTrack({
            id: msg.id,
            title: att.title,
            subtitle: msg.senderName,
            durationSec: durSec,
            url: att.previewUrl,
          });
        },
      },
      el('div', { class: 'ag-att-audio__btn' }, playingThis ? icons.pause(16) : icons.play(16)),
      el(
        'div',
        { class: 'ag-att-audio__meta', style: 'flex:1;min-width:0;' },
        el(
          'strong',
          { style: 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;' },
          att.title,
        ),
        el('span', null, formatDuration(durSec)),
      ),
    );

    if (att.type !== 'voice') {
      return { node: audioRow };
    }

    const ca = chatActionsT();
    const sttBtn = el(
      'button',
      {
        type: 'button',
        class: 'ag-voice-stt-btn',
        title: ca.transcribe,
        'aria-label': ca.transcribe,
        style: [
          'height:26px',
          'padding:0 9px',
          'border-radius:9999px',
          'background:color-mix(in srgb, currentColor 14%, transparent)',
          'color:inherit',
          'font-size:11.5px',
          'font-weight:700',
          'line-height:1',
          'display:inline-flex',
          'align-items:center',
          'justify-content:center',
          'flex-shrink:0',
          'margin-left:auto',
          'transition:background-color 0.14s ease,transform 0.12s ease',
        ].join(';'),
        onClick: (e: Event) => {
          e.stopPropagation();
          if (ctx.selectionMode) {
            ctx.onToggleSelect(msg);
            return;
          }
          if (ctx.onTranscribeVoice) {
            ctx.onTranscribeVoice(msg);
            return;
          }
          if (localTranslator.getVoiceTranscript(msg.id)) {
            localTranslator.clearVoiceTranscript(msg.id);
          } else {
            void localTranslator.transcribeVoice(msg.id, durSec, att.previewUrl);
          }
        },
      },
      ca.transcribeShort,
    ) as HTMLButtonElement;

    audioRow.appendChild(sttBtn);

    const sttBox = el('div', {
      class: 'ag-stt-box',
      style: [
        'display:none',
        'margin-top:4px',
        'padding:7px 10px',
        'border-radius:12px',
        'background:color-mix(in srgb, currentColor 9%, transparent)',
        'border:1px solid color-mix(in srgb, currentColor 16%, transparent)',
        'color:inherit',
      ].join(';'),
    });

    const syncVoice = () => {
      const curCa = chatActionsT();
      const transcript = localTranslator.getVoiceTranscript(msg.id);
      const transcribing = localTranslator.isVoiceTranscribing(msg.id);
      const hasContent = Boolean(transcript) || transcribing;

      sttBtn.style.background = transcript
        ? 'var(--ag-accent)'
        : 'color-mix(in srgb, currentColor 14%, transparent)';
      sttBtn.style.color = transcript ? 'var(--ag-on-accent)' : 'inherit';
      sttBtn.classList.toggle('is-active', Boolean(transcript));

      sttBox.replaceChildren();
      if (!hasContent) {
        sttBox.style.display = 'none';
        return;
      }

      sttBox.style.display = 'flex';
      if (transcribing && !transcript) {
        sttBox.appendChild(
          el(
            'div',
            {
              class: 'ag-stt-box__status',
              style: 'color:inherit;opacity:0.8;font-size:12px;',
            },
            curCa.transcribing,
            el('span', { class: 'ag-stt-cursor' }, ' |'),
          ),
        );
      } else if (transcript) {
        const closeBtn = el(
          'button',
          {
            type: 'button',
            title: i18nStore.t.common.close,
            'aria-label': i18nStore.t.common.close,
            style: [
              'width:20px',
              'height:20px',
              'border-radius:9999px',
              'display:inline-flex',
              'align-items:center',
              'justify-content:center',
              'color:inherit',
              'opacity:0.65',
              'flex-shrink:0',
            ].join(';'),
            onClick: (e: Event) => {
              e.stopPropagation();
              localTranslator.clearVoiceTranscript(msg.id);
            },
          },
          icons.close(12),
        );
        sttBox.appendChild(
          el(
            'div',
            { style: 'display:flex;align-items:flex-start;justify-content:space-between;gap:8px;' },
            el(
              'div',
              {
                class: 'ag-stt-box__text',
                style: 'color:inherit;font-size:12.5px;line-height:1.4;flex:1;min-width:0;',
              },
              transcript,
            ),
            closeBtn,
          ),
        );
      }
    };

    syncVoice();
    const wrap = el(
      'div',
      {
        class: 'ag-voice-wrap',
        style: 'display:flex;flex-direction:column;gap:2px;',
      },
      audioRow,
      sttBox,
    );
    return { node: wrap, syncVoice };
  }

  if (att.previewUrl && (att.type === 'photo' || att.type === 'album')) {
    return {
      node: el(
        'div',
        {
          class: 'ag-att-photo',
          onClick: (e: Event) => {
            e.stopPropagation();
            if (ctx.selectionMode) {
              ctx.onToggleSelect(msg);
              return;
            }
            openMediaViewer(att, undefined, undefined, msg);
          },
        },
        el('img', { src: att.previewUrl, alt: att.title }),
      ),
    };
  }

  return {
    node: el(
      'div',
      {
        class: 'ag-att-card',
        onClick: (e: Event) => {
          e.stopPropagation();
          if (ctx.selectionMode) {
            ctx.onToggleSelect(msg);
            return;
          }
          if (att.type === 'file') {
            openDocumentViewer(att, msg);
          } else {
            openMediaViewer(att, undefined, undefined, msg);
          }
        },
      },
      el('span', { class: 'ag-att-card__badge' }, att.type.toUpperCase()),
      el(
        'div',
        { class: 'ag-att-card__info' },
        el('strong', null, att.title),
        att.fileSizeLabel || att.subtitle
          ? el('span', null, att.fileSizeLabel ?? att.subtitle ?? '')
          : null,
      ),
    ),
  };
}

function makeHoverActionButton(
  cls: string,
  label: string,
  icon: SVGElement,
  onClick: (e: Event, btn: HTMLButtonElement) => void,
): HTMLButtonElement {
  const baseBg = 'color-mix(in srgb, var(--ag-surface) 84%, transparent)';
  const btn = el(
    'button',
    {
      type: 'button',
      class: cls,
      title: label,
      'aria-label': label,
      style: [
        'width:28px',
        'height:28px',
        'border-radius:9999px',
        'display:inline-flex',
        'align-items:center',
        'justify-content:center',
        `background:${baseBg}`,
        'border:1px solid var(--ag-island-border)',
        'color:var(--ag-text-secondary)',
        'backdrop-filter:blur(8px)',
        '-webkit-backdrop-filter:blur(8px)',
        'transition:background-color 0.12s ease,color 0.12s ease,transform 0.12s ease',
      ].join(';'),
    },
    icon,
  ) as HTMLButtonElement;

  btn.addEventListener('mouseenter', () => {
    btn.style.background = 'var(--ag-accent-soft)';
    btn.style.color = 'var(--ag-accent)';
  });
  btn.addEventListener('mouseleave', () => {
    btn.style.background = baseBg;
    btn.style.color = 'var(--ag-text-secondary)';
  });
  btn.addEventListener('click', (e) => onClick(e, btn));
  return btn;
}

function createDesktopHoverActions(
  msg: ChatMessage,
  ctx: BubbleContext,
): { wrap: HTMLElement; setVisible: (visible: boolean) => void } {
  const replyLabel = i18nStore.t.chats.replyAction;
  const moreLabel = chatScreenT().more;

  const quickReplyBtn = makeHoverActionButton(
    'ag-cs-hover-btn ag-cs-quick-reply',
    replyLabel,
    icons.reply(15),
    (e) => {
      e.stopPropagation();
      ctx.onSwipeReply(msg);
    },
  );
  const moreBtn = makeHoverActionButton(
    'ag-cs-hover-btn ag-cs-hover-more',
    moreLabel,
    icons.more(15),
    (e, btn) => {
      e.stopPropagation();
      const r = btn.getBoundingClientRect();
      ctx.onContextMenu(msg, r.left, r.bottom + 4);
    },
  );

  const wrap = el(
    'div',
    {
      class: 'ag-cs-hover-actions',
      style: [
        'display:inline-flex',
        'align-items:center',
        'gap:4px',
        'margin:0 6px',
        'align-self:center',
        'flex-shrink:0',
        'opacity:0',
        'pointer-events:none',
        'transform:scale(0.94)',
        'transition:opacity 0.14s ease,transform 0.14s ease',
      ].join(';'),
    },
    quickReplyBtn,
    moreBtn,
  );

  const setVisible = (visible: boolean) => {
    wrap.style.opacity = visible ? '1' : '0';
    wrap.style.pointerEvents = visible ? 'auto' : 'none';
    wrap.style.transform = visible ? 'scale(1)' : 'scale(0.94)';
  };

  return { wrap, setVisible };
}

/** Message row: bubble + grouping corners + desktop hover quick-reply + mobile quick-tap/long-press + translation/STT/video circle. */
export function createMessageRow(msg: ChatMessage, ctx: BubbleContext): HTMLElement {
  const dir = msg.isOutgoing ? 'is-outgoing' : 'is-incoming';
  const isFirst = ctx.isGroupFirst ?? true;
  const isLast = ctx.isGroupLast ?? true;
  const groupClass =
    isFirst && !isLast
      ? 'is-group-first'
      : !isFirst && !isLast
        ? 'is-group-mid'
        : !isFirst && isLast
          ? 'is-group-last'
          : '';
  const groupSuffix = groupClass ? ` ${groupClass}` : '';
  const ca = chatActionsT();
  const isMobileViewport = window.innerWidth < 840;

  let textNode: HTMLElement | null = null;
  if (msg.text) {
    textNode = el('div', { class: 'ag-bubble__text' });
    appendTextWithLinks(
      textNode,
      msg.text,
      ctx.selectionMode,
      () => ctx.onToggleSelect(msg),
      ctx.onOpenLink,
    );
  }

  const translatedBox = el('div', {
    class: 'ag-translated-box ag-tr-box',
    style: [
      'display:none',
      'margin-top:6px',
      'padding:7px 10px',
      'border-radius:12px',
      'background:color-mix(in srgb, currentColor 9%, transparent)',
      'border:1px solid color-mix(in srgb, currentColor 16%, transparent)',
      'color:inherit',
    ].join(';'),
  });

  const syncTranslation = () => {
    const curCa = chatActionsT();
    const translated = localTranslator.getMessageTranslation(msg.id);
    const entry = localTranslator.getMessageTranslationEntry(msg.id);
    const isTranslating = localTranslator.isMessageTranslating(msg.id);

    translatedBox.replaceChildren();
    if (!translated && !isTranslating) {
      translatedBox.style.display = 'none';
      return;
    }

    translatedBox.style.display = 'flex';
    const langMeta =
      entry?.sourceLang && entry?.targetLang
        ? ` (${entry.sourceLang.toUpperCase()} → ${entry.targetLang.toUpperCase()})`
        : '';

    const metaRow = el(
      'div',
      {
        class: 'ag-tr-box__meta',
        style: `color:${msg.isOutgoing ? 'inherit' : 'var(--ag-accent)'};opacity:${msg.isOutgoing ? '0.86' : '1'};`,
      },
      el('span', null, `🌐 ${translated ? `${curCa.translated}${langMeta}` : curCa.translating}`),
      translated
        ? el(
            'button',
            {
              type: 'button',
              title: i18nStore.t.common.close,
              'aria-label': i18nStore.t.common.close,
              style: [
                'width:20px',
                'height:20px',
                'border-radius:9999px',
                'display:inline-flex',
                'align-items:center',
                'justify-content:center',
                'color:inherit',
                'opacity:0.7',
                'flex-shrink:0',
              ].join(';'),
              onClick: (e: Event) => {
                e.stopPropagation();
                localTranslator.clearMessageTranslation(msg.id);
              },
            },
            icons.close(12),
          )
        : null,
    );
    translatedBox.appendChild(metaRow);

    if (translated) {
      const bodyEl = el('div', {
        class: 'ag-tr-box__text',
        style: 'color:inherit;white-space:pre-wrap;word-break:break-word;',
      });
      appendTextWithLinks(
        bodyEl,
        translated,
        ctx.selectionMode,
        () => ctx.onToggleSelect(msg),
        ctx.onOpenLink,
      );
      translatedBox.appendChild(bodyEl);
    }
  };
  syncTranslation();

  const renderedAtt = msg.attachment ? renderAttachment(msg, msg.attachment, ctx) : null;

  const bubble = el(
    'div',
    { class: `ag-bubble ${dir}${groupSuffix}` },
    msg.replyTo
      ? el(
          'div',
          {
            class: 'ag-reply-strip',
            onClick: (e: Event) => {
              e.stopPropagation();
              if (ctx.selectionMode) {
                ctx.onToggleSelect(msg);
                return;
              }
              ctx.onReplyJump(msg.replyTo!.messageId);
            },
          },
          el('strong', null, msg.replyTo.senderName),
          el('span', null, msg.replyTo.snippet),
        )
      : null,
    renderedAtt?.node ?? null,
    textNode,
    translatedBox,
    el(
      'div',
      { class: 'ag-bubble__footer' },
      msg.reactions && Object.keys(msg.reactions).length
        ? el(
            'div',
            { class: 'ag-reactions' },
            ...Object.entries(msg.reactions).map(([em, count]) =>
              el(
                'button',
                {
                  type: 'button',
                  class: 'ag-reaction-chip',
                  onClick: (e: Event) => {
                    e.stopPropagation();
                    if (ctx.selectionMode) {
                      ctx.onToggleSelect(msg);
                      return;
                    }
                    chatStore.toggleReaction(ctx.chatId, msg.id, em);
                  },
                },
                `${em} ${count}`,
              ),
            ),
          )
        : null,
      msg.isEdited
        ? el(
            'span',
            {
              class: 'ag-edited-tag',
              style: 'opacity:0.72;font-style:italic;font-size:10.5px;',
            },
            `(${ca.edited})`,
          )
        : null,
      el('span', { class: 'ag-bubble__time' }, msg.timestamp),
    ),
  );

  // Prevent Android Chrome's native Copy/Search popup from interrupting mobile long-press
  if (isMobileViewport) {
    bubble.addEventListener('contextmenu', (e) => e.preventDefault());
    bubble.style.userSelect = 'none';
    bubble.style.webkitUserSelect = 'none';
    (bubble.style as CSSStyleDeclaration & { webkitTouchCallout?: string }).webkitTouchCallout =
      'none';
  }

  const check = el('span', { class: 'ag-cs-check' }, icons.check(14));
  const swipeIcon = el('span', { class: 'ag-cs-swipe-icon' }, icons.reply(18));
  const hoverActions =
    ctx.isDesktop && !ctx.selectionMode ? createDesktopHoverActions(msg, ctx) : null;

  const bubbleWrap = msg.isOutgoing
    ? el('div', { class: 'ag-cs-bubble-wrap' }, swipeIcon, hoverActions?.wrap, bubble)
    : el('div', { class: 'ag-cs-bubble-wrap' }, swipeIcon, bubble, hoverActions?.wrap);

  const row = el(
    'div',
    {
      class: `ag-msg-row ${dir}${groupSuffix} ag-cs-row${ctx.selectionMode ? ' is-selecting' : ''}${ctx.selected ? ' is-selected' : ''}`,
      'data-msg-id': msg.id,
    },
    check,
    bubbleWrap,
  );

  if (isMobileViewport) {
    row.style.userSelect = 'none';
    row.style.webkitUserSelect = 'none';
    (row.style as CSSStyleDeclaration & { webkitTouchCallout?: string }).webkitTouchCallout =
      'none';
  }

  if (msg.text || msg.attachment?.type === 'voice') {
    registerTranslatorSync(row, () => {
      syncTranslation();
      renderedAtt?.syncVoice?.();
    });
  }

  const onOpenMenu = (targetMsg: ChatMessage, e: Event, explicitX?: number, explicitY?: number) => {
    if (ctx.onOpenMenu) {
      ctx.onOpenMenu(targetMsg, e);
      return;
    }
    let x = explicitX;
    let y = explicitY;
    if (x === undefined || y === undefined) {
      const te = e as TouchEvent;
      const me = e as MouseEvent;
      const t = te.changedTouches?.[0] ?? te.touches?.[0];
      if (t) {
        x = t.clientX;
        y = t.clientY;
      } else if (
        typeof me.clientX === 'number' &&
        typeof me.clientY === 'number' &&
        (me.clientX !== 0 || me.clientY !== 0)
      ) {
        x = me.clientX;
        y = me.clientY;
      } else {
        const rect = bubble.getBoundingClientRect();
        x = targetMsg.isOutgoing ? rect.right : rect.left;
        y = rect.bottom;
      }
    }
    ctx.onContextMenu(targetMsg, x, y);
  };

  if (hoverActions) {
    row.addEventListener('mouseenter', () => hoverActions.setVisible(true));
    row.addEventListener('mouseleave', () => hoverActions.setVisible(false));
    row.addEventListener('focusin', () => hoverActions.setVisible(true));
    row.addEventListener('focusout', (e) => {
      if (!row.contains(e.relatedTarget as Node | null)) {
        hoverActions.setVisible(false);
      }
    });
  }

  let lastHandledTouchAt = 0;

  // Desktop click (selection mode) and narrow-window mouse fallback
  row.addEventListener('click', (e) => {
    if (performance.now() - lastHandledTouchAt < 500) return;
    if (ctx.selectionMode) {
      ctx.onToggleSelect(msg);
      return;
    }
    if (window.innerWidth < 840 && !isInteractiveTarget(e.target)) {
      onOpenMenu(msg, e);
    }
  });

  // Desktop right-click -> menu; on mobile (<840) native menu is suppressed in favor of quick-tap/long-press
  row.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    if (window.innerWidth < 840 || ctx.selectionMode) return;
    if (performance.now() - lastHandledTouchAt < 900) return;
    onOpenMenu(msg, e, e.clientX, e.clientY);
  });

  // Touch: quick-tap (<380ms) opens menu/toggles select; long-press (>=420ms) enters selectionMode; swipe-left replies
  let sx = 0;
  let sy = 0;
  let dx = 0;
  let startTime = 0;
  let tracking = false;
  let swiping = false;
  let movedBeyondSlop = false;
  let longPressed = false;
  let startedOnInteractive = false;
  let pressTimer = 0;
  const wrap = bubbleWrap;

  const cancelPress = () => {
    if (pressTimer) {
      window.clearTimeout(pressTimer);
      pressTimer = 0;
    }
  };

  row.addEventListener(
    'touchstart',
    (e) => {
      if (e.touches.length > 1) {
        cancelPress();
        tracking = false;
        return;
      }
      const t = e.touches[0];
      sx = t.clientX;
      sy = t.clientY;
      dx = 0;
      startTime = performance.now();
      tracking = true;
      swiping = false;
      movedBeyondSlop = false;
      longPressed = false;
      startedOnInteractive = isInteractiveTarget(e.target);

      cancelPress();
      pressTimer = window.setTimeout(() => {
        pressTimer = 0;
        if (!tracking || movedBeyondSlop || swiping) return;
        longPressed = true;
        tracking = false;
        lastHandledTouchAt = performance.now();
        navigator.vibrate?.(12);
        if (ctx.selectionMode) {
          ctx.onToggleSelect(msg);
        } else {
          ctx.onEnterSelect(msg);
        }
      }, LONG_PRESS_MS);
    },
    { passive: true },
  );

  row.addEventListener(
    'touchmove',
    (e) => {
      if (!tracking) return;
      const t = e.touches[0];
      const mx = t.clientX - sx;
      const my = t.clientY - sy;
      const dist = Math.hypot(mx, my);

      if (!swiping && dist >= TAP_SLOP_PX) {
        movedBeyondSlop = true;
        cancelPress();
      }
      if (!swiping && Math.abs(my) >= TAP_SLOP_PX && Math.abs(my) > Math.abs(mx)) {
        tracking = false;
        return;
      }
      if (ctx.selectionMode) return;
      if (mx < -TAP_SLOP_PX && Math.abs(mx) > Math.abs(my) * 1.2) {
        swiping = true;
        cancelPress();
      }
      if (swiping) {
        dx = Math.max(-80, Math.min(0, mx));
        wrap.style.transform = `translateX(${dx}px)`;
        row.style.setProperty('--ag-swipe-p', String(Math.min(1, -dx / SWIPE_TRIGGER)));
      }
    },
    { passive: true },
  );

  const onTouchEnd = (e: TouchEvent) => {
    cancelPress();
    if (swiping && -dx >= SWIPE_TRIGGER) {
      ctx.onSwipeReply(msg);
      lastHandledTouchAt = performance.now();
    }
    wrap.style.transform = '';
    row.style.removeProperty('--ag-swipe-p');

    if (longPressed) {
      if (e.cancelable) e.preventDefault();
    } else if (
      tracking &&
      !swiping &&
      !movedBeyondSlop &&
      !startedOnInteractive &&
      !isInteractiveTarget(e.target) &&
      performance.now() - startTime < QUICK_TAP_MAX_MS
    ) {
      if (e.cancelable) e.preventDefault();
      lastHandledTouchAt = performance.now();
      if (ctx.selectionMode) {
        ctx.onToggleSelect(msg);
      } else {
        onOpenMenu(msg, e);
      }
    }

    tracking = false;
    swiping = false;
    dx = 0;
  };

  row.addEventListener('touchend', onTouchEnd, { passive: false });
  row.addEventListener('touchcancel', () => {
    cancelPress();
    wrap.style.transform = '';
    row.style.removeProperty('--ag-swipe-p');
    tracking = false;
    swiping = false;
    dx = 0;
    longPressed = false;
    movedBeyondSlop = false;
  });

  return row;
}
