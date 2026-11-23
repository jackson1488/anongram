import { clear, el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
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

function renderMediaGrid(items: UserProfileMediaItem[], emptyText: string): HTMLElement {
  if (items.length === 0) {
    return el('div', { class: 'ag-up-empty' }, emptyText);
  }
  const grid = el('div', { class: 'ag-up-media-grid' });
  for (const item of items) {
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
        'div',
        {
          class: 'ag-up-media-tile' + (item.isVideoCircle ? ' is-circle' : ''),
          title: item.title,
        },
        thumb,
        badge,
      ),
    );
  }
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
    list.appendChild(
      el(
        'div',
        { class: 'ag-up-list-row' },
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
  let playingId: string | null = null;

  const rebuild = () => {
    clear(list);
    for (const item of items) {
      const isPlaying = playingId === item.message.id;
      const dur = formatDurationSec(item.durationSec);
      const sub = [dur, item.message.senderName, item.message.timestamp].filter(Boolean).join(' • ');
      list.appendChild(
        el(
          'button',
          {
            class: 'ag-up-list-row' + (isPlaying ? ' is-playing' : ''),
            onClick: () => {
              playingId = isPlaying ? null : item.message.id;
              rebuild();
            },
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
    }
  };
  rebuild();
  return list;
}

export function createUserProfileMediaTabs(props: UserProfileMediaTabsProps): HTMLElement {
  const { buckets, activeTab, strings, onSelectTab } = props;

  const tabBar = el(
    'div',
    { class: 'ag-up-tabs' },
    ...ALL_TABS.map((tab) =>
      el(
        'button',
        {
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
