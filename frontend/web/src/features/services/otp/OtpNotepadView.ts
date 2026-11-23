import { i18nStore } from '../../../core/i18n/i18nStore';
import { kvStore } from '../../../core/storage/kvStore';
import { clear, el } from '../../../core/ui/dom';
import { icons, tgIcons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { servicesT } from '../i18n';

export interface OtpPage {
  id: string;
  title: string;
  secret: string;
  revealed: boolean;
}

const KEY_OTP_PAGES = 'services.otp_pages.v1';

function makeDefaultPages(): OtpPage[] {
  return Array.from({ length: 7 }, (_, i) => ({
    id: `otp_${i + 1}_${Date.now().toString(36)}`,
    title: `OTP #${i + 1}`,
    secret: `${Math.random().toString(36).slice(2, 10).toUpperCase()}-${Math.random().toString(36).slice(2, 10).toUpperCase()}`,
    revealed: false,
  }));
}

class OtpStore {
  private pages: OtpPage[];
  private listeners = new Set<() => void>();

  constructor() {
    const saved = kvStore.getJson<OtpPage[] | null>(KEY_OTP_PAGES, null);
    this.pages = saved && Array.isArray(saved) ? saved : makeDefaultPages();
  }

  private persist(): void {
    kvStore.setJson(KEY_OTP_PAGES, this.pages);
    for (const fn of this.listeners) fn();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  getPages(): OtpPage[] {
    return this.pages;
  }

  addPage(title: string, secret: string): void {
    const p: OtpPage = {
      id: `otp_${Date.now().toString(36)}`,
      title: title.trim() || 'OTP Note',
      secret: secret.trim(),
      revealed: false,
    };
    this.pages = [p, ...this.pages];
    this.persist();
  }

  revealPage(id: string): void {
    this.pages = this.pages.map((p) => (p.id === id ? { ...p, revealed: true } : p));
    this.persist();
  }

  burnPage(id: string): void {
    this.pages = this.pages.filter((p) => p.id !== id);
    this.persist();
  }

  resetPad(): void {
    this.pages = makeDefaultPages();
    this.persist();
  }
}

export const otpStore = new OtpStore();

export function createOtpNotepadView(onBack: () => void): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });
  let titleDraft = '';
  let secretDraft = '';

  const render = () => {
    clear(root);
    const s = servicesT();
    const pages = otpStore.getPages();

    root.appendChild(
      el(
        'div',
        { class: 'ag-chat-header' },
        el('button', { class: 'ag-icon-btn', onClick: onBack }, icons.back(20)),
        el('div', { class: 'ag-chat-header__title' }, s.otpHeading),
        el(
          'button',
          {
            class: 'ag-chip',
            onClick: () => {
              otpStore.resetPad();
              showToast(s.otpResetToast);
            },
          },
          s.otpResetBtn,
        ),
      ),
    );

    const titleInput = el('input', {
      type: 'text',
      class: 'ag-contact-field',
      placeholder: s.otpTitlePlaceholder,
      value: titleDraft,
      onInput: (e: Event) => {
        titleDraft = (e.target as HTMLInputElement).value;
      },
    }) as HTMLInputElement;

    const secretInput = el('input', {
      type: 'text',
      class: 'ag-contact-field',
      placeholder: s.otpContentPlaceholder,
      value: secretDraft,
      onInput: (e: Event) => {
        secretDraft = (e.target as HTMLInputElement).value;
      },
    }) as HTMLInputElement;

    const scroll = el('div', { class: 'ag-scroll-list ag-services-scroll' });
    scroll.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card', style: 'gap:8px;' },
        titleInput,
        secretInput,
        el(
          'button',
          {
            class: 'ag-pill-btn ag-pill-btn--primary',
            onClick: () => {
              if (!secretDraft.trim()) return;
              otpStore.addPage(titleDraft, secretDraft);
              titleDraft = '';
              secretDraft = '';
              showToast(s.otpCreatedToast);
            },
          },
          icons.flame(15),
          s.otpCreateBtn,
        ),
      ),
    );

    if (pages.length === 0) {
      scroll.appendChild(
        el(
          'div',
          { style: 'text-align:center;padding:28px 14px;color:var(--ag-text-muted);font-size:13px;' },
          s.otpEmpty,
        ),
      );
    } else {
      for (const page of pages) {
        scroll.appendChild(
          el(
            'div',
            { class: 'ag-otp-page-card' },
            el(
              'div',
              { style: 'display:flex;align-items:center;justify-content:space-between;gap:8px;' },
              el('strong', { style: 'font-size:14px;color:var(--ag-text-primary);' }, page.title),
              el(
                'button',
                {
                  class: 'ag-chip',
                  onClick: () => {
                    otpStore.burnPage(page.id);
                    showToast(s.otpBurnedToast);
                  },
                },
                icons.flame(13),
                s.otpBurnBtn,
              ),
            ),
            page.revealed
              ? el('div', { class: 'ag-otp-secret' }, page.secret)
              : el(
                  'button',
                  {
                    class: 'ag-pill-btn ag-pill-btn--secondary',
                    style: 'min-height:36px;',
                    onClick: () => otpStore.revealPage(page.id),
                  },
                  tgIcons.eye(15),
                  s.otpRevealBtn,
                ),
          ),
        );
      }
    }

    root.appendChild(scroll);
  };

  const unsubOtp = otpStore.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);
  render();
  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubOtp();
    unsubI18n();
  };
  return root;
}
