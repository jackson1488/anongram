import { clear, el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { audioPlayer, type PlayerTrack } from '../../../player/audioPlayer';
import { openExternalLink } from '../../links/InAppBrowserHost';
import type { MessageAttachment } from '../../models';
import { openDocumentViewer, openMediaViewer } from '../../viewer/MediaViewerOverlay';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';
import {
  formatDurationSec,
  type UserProfileAudioItem,
  type UserProfileBuckets,
  type UserProfileFileItem,
  type UserProfileLinkItem,
  type UserProfileMediaItem,
  type UserProfileTab,
} from '../model/UserProfileModels';

export interface UserProfileMediaTabsProps {
  buckets: UserProfileBuckets;
  activeTab: UserProfileTab;
  strings: UserProfileStrings;
  onSelectTab: (tab: UserProfileTab) => void;
}

const ALL_TABS: UserProfileTab[] = ['MEDIA', 'FILES', 'LINKS', 'VOICE'];

function tabLabel(tab: UserProfileTab, strings: UserProfileStrings, buckets: UserProfileBuckets): string {
  switch (tab) {
    case 'MEDIA':
      return buckets.media.length > 0 ? `${strings.tabMedia} (${buckets.media.length})` : strings.tabMedia;
    case 'FILES':
      return buckets.files.length > 0 ? `${strings.tabFiles} (${buckets.files.length})` : strings.tabFiles;
    case 'LINKS':
      return buckets.links.length > 0 ? `${strings.tabLinks} (${buckets.links.length})` : strings.tabLinks;
    case 'VOICE':
      return buckets.voice.length > 0 ? `${strings.tabVoice} (${buckets.voice.length})` : strings.tabVoice;
  }
}

function toMediaAttachment(item: UserProfileMediaItem): MessageAttachment {
  return (
    item.message.attachment ?? {
      type: item.isVideoCircle ? 'video_circle' : 'photo',
      title: item.title,
      previewUrl: item.previewUrl,
      durationSec: item.durationSec,
      isVideo: item.isVideo,
    }
  );
}

function renderMediaGrid(items: UserProfileMediaItem[], emptyText: string): HTMLElement {
  if (items.length === 0) {
    return el('div', { class: 'ag-up-empty' }, emptyText);
  }
  const gallery = items.map(toMediaAttachment);
  const grid = el('div', { class: 'ag-up-media-grid' });

  items.forEach((item, idx) => {
    const thumb = item.previewUrl
      ? el('img', { class: 'ag-up-media-tile__img', src: item.previewUrl, alt: item.title, loading: 'lazy' })
      : el(
          'div',
          { class: 'ag-up-media-tile__placeholder' },
          item.isVideoCircle ? icons.video(22) : icons.image(22),
        );

    const badge =
      item.isVideo && item.durationSec
        ? el('span', { class: 'ag-up-media-tile__badge' }, formatDurationSec(item.durationSec))
        : null;

    grid.appendChild(
      el(
        'button',
        {
          type: 'button',
          class: 'ag-up-media-tile' + (item.isVideoCircle ? ' is-circle' : ''),
          style: 'cursor:pointer;min-width:0;',
          title: item.title,
          'aria-label': item.title,
          onClick: () => openMediaViewer(gallery[idx], gallery, idx, item.message),
        },
        thumb,
        badge,
      ),
    );
  });

  return grid;
}

function renderFilesList(items: UserProfileFileItem[], emptyText: string): HTMLElement {
  if (items.length === 0) {
    return el('div', { class: 'ag-up-empty' }, emptyText);
  }
  const list = el('div', { class: 'ag-up-list' });
  for (const item of items) {
    const meta = [item.fileSizeLabel, item.subtitle, item.message.timestamp]
      .filter(Boolean)
      .join(' • ');
    const attachment: MessageAttachment = item.message.attachment ?? {
      type: 'file',
      title: item.title,
      subtitle: item.subtitle,
      fileSizeLabel: item.fileSizeLabel,
    };
    list.appendChild(
      el(
        'button',
        {
          type: 'button',
          class: 'ag-up-list-row',
          title: item.title,
          onClick: () => openDocumentViewer(attachment, item.message),
        },
        el('div', { class: 'ag-up-list-row__icon' }, tgIcons.file(18)),
        el(
          'div',
          { class: 'ag-up-list-row__body' },
          el('div', { class: 'ag-up-list-row__title' }, item.title),
          el('div', { class: 'ag-up-list-row__sub' }, meta),
        ),
      ),
    );
  }
  return list;
}

function renderLinksList(items: UserProfileLinkItem[], emptyText: string): HTMLElement {
  if (items.length === 0) {
    return el('div', { class: 'ag-up-empty' }, emptyText);
  }
  const list = el('div', { class: 'ag-up-list' });
  for (const item of items) {
    list.appendChild(
      el(
        'a',
        {
          class: 'ag-up-list-row ag-up-list-row--link',
          href: item.url,
          target: '_blank',
          rel: 'noopener noreferrer',
          title: item.url,
          onClick: (e: Event) => {
            e.preventDefault();
            openExternalLink(item.url);
          },
        },
        el('div', { class: 'ag-up-list-row__icon' }, tgIcons.link(18)),
        el(
          'div',
          { class: 'ag-up-list-row__body' },
          el('div', { class: 'ag-up-list-row__title' }, item.domain),
          el('div', { class: 'ag-up-list-row__sub' }, item.url),
        ),
      ),
    );
  }
  return list;
}

function renderVoiceList(items: UserProfileAudioItem[], emptyText: string): HTMLElement {
  if (items.length === 0) {
    return el('div', { class: 'ag-up-empty' }, emptyText);
  }
  const list = el('div', { class: 'ag-up-list' });
  const tracks: PlayerTrack[] = items.map((item) => ({
    id: item.message.id,
    title: item.title,
    subtitle:
      (item.isVoice ? item.message.senderName : item.subtitle) ||
      item.message.senderName ||
      formatDurationSec(item.durationSec),
    durationSec: item.durationSec ?? item.message.attachment?.durationSec ?? 24,
    url: item.message.attachment?.previewUrl,
  }));

  const rebuild = () => {
    clear(list);
    items.forEach((item, idx) => {
      const track = tracks[idx];
      const isCurrent = audioPlayer.currentTrack?.id === item.message.id;
      const isPlaying = isCurrent && audioPlayer.playing;
      const dur = formatDurationSec(track.durationSec);
      const authorOrArtist = item.isVoice
        ? item.message.senderName
        : item.subtitle || item.message.senderName;
      const sub = [dur, authorOrArtist, item.message.timestamp].filter(Boolean).join(' • ');

      list.appendChild(
        el(
          'button',
          {
            type: 'button',
            class: 'ag-up-list-row' + (isPlaying ? ' is-playing' : ''),
            'aria-pressed': isPlaying ? 'true' : 'false',
            onClick: () => audioPlayer.toggleTrack(track, tracks),
          },
          el(
            'div',
            { class: 'ag-up-list-row__icon is-accent' },
            isPlaying ? icons.pause(16) : icons.play(16),
          ),
          el(
            'div',
            { class: 'ag-up-list-row__body' },
            el('div', { class: 'ag-up-list-row__title' }, item.title),
            el('div', { class: 'ag-up-list-row__sub' }, sub),
          ),
        ),
      );
    });
  };

  rebuild();

  let audioKey = `${audioPlayer.currentTrack?.id ?? ''}|${audioPlayer.playing}`;
  let wasConnected = false;
  queueMicrotask(() => {
    if (list.isConnected) wasConnected = true;
  });
  const unsub = audioPlayer.subscribe(() => {
    if (list.isConnected) {
      wasConnected = true;
    } else if (wasConnected) {
      unsub();
      return;
    }
    const nextKey = `${audioPlayer.currentTrack?.id ?? ''}|${audioPlayer.playing}`;
    if (nextKey === audioKey) return;
    audioKey = nextKey;
    rebuild();
  });

  return list;
}

export function createUserProfileMediaTabs(props: UserProfileMediaTabsProps): HTMLElement {
  const { buckets, activeTab, strings, onSelectTab } = props;

  const tabBar = el(
    'div',
    { class: 'ag-up-tabs', role: 'tablist' },
    ...ALL_TABS.map((tab) =>
      el(
        'button',
        {
          type: 'button',
          role: 'tab',
          'aria-selected': String(tab === activeTab),
          class: 'ag-up-tab' + (tab === activeTab ? ' is-active' : ''),
          onClick: () => onSelectTab(tab),
        },
        tabLabel(tab, strings, buckets),
      ),
    ),
  );

  let content: HTMLElement;
  switch (activeTab) {
    case 'MEDIA':
      content = renderMediaGrid(buckets.media, strings.emptyTab);
      break;
    case 'FILES':
      content = renderFilesList(buckets.files, strings.emptyTab);
      break;
    case 'LINKS':
      content = renderLinksList(buckets.links, strings.emptyTab);
      break;
    case 'VOICE':
      content = renderVoiceList(buckets.voice, strings.emptyTab);
      break;
  }

  return el('div', { class: 'ag-up-media-section' }, tabBar, content);
}
