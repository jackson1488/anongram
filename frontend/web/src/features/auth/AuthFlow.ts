import type { UserIdentity } from '../../core/crypto/identity';
import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { authStore } from './authStore';
import './auth.css';

type AuthStep = 'welcome' | 'create' | 'backup' | 'restore';

export function renderAuthShield(): HTMLElement {
  const t = i18nStore.t;
  return el(
    'div',
    { class: 'ag-auth-screen' },
    el(
      'div',
      { class: 'ag-auth-card' },
      el('div', { class: 'ag-shield-pulse' }, icons.shield(40)),
      el('h1', { class: 'ag-auth-title' }, t.auth.welcomeTitle),
      el('p', { class: 'ag-auth-subtitle' }, t.auth.shieldChecking),
    ),
  );
}

export function createAuthFlow(): HTMLElement {
  const root = el('div', { class: 'ag-auth-screen' });
  let step: AuthStep = 'welcome';
  let draftIdentity: UserIdentity | null = null;
  let restoreError = '';

  const render = () => {
    clear(root);
    const t = i18nStore.t;

    if (step === 'welcome') {
      root.appendChild(
        el(
          'div',
          { class: 'ag-auth-card ag-auth-card--boxed' },
          el('div', { class: 'ag-shield-pulse' }, icons.shield(40)),
          el('div', { class: 'ag-auth-badge' }, t.auth.anonBadge),
          el('h1', { class: 'ag-auth-title' }, t.auth.welcomeTitle),
          el('p', { class: 'ag-auth-subtitle' }, t.auth.welcomeSubtitle),
          el(
            'div',
            { class: 'ag-auth-actions' },
            el(
              'button',
              {
                class: 'ag-pill-btn ag-pill-btn--primary',
                onClick: () => {
                  step = 'create';
                  render();
                },
              },
              t.auth.createAccountBtn,
            ),
            el(
              'button',
              {
                class: 'ag-pill-btn ag-pill-btn--secondary',
                onClick: () => {
                  restoreError = '';
                  step = 'restore';
                  render();
                },
              },
              t.auth.restoreAccountBtn,
            ),
          ),
        ),
      );
      return;
    }

    if (step === 'create') {
      const nameInput = el('input', {
        class: 'ag-input',
        type: 'text',
        placeholder: t.auth.displayNamePlaceholder,
      }) as HTMLInputElement;
      const userInput = el('input', {
        class: 'ag-input',
        type: 'text',
        placeholder: t.auth.usernamePlaceholder,
      }) as HTMLInputElement;

      root.appendChild(
        el(
          'div',
          { class: 'ag-auth-card ag-auth-card--boxed' },
          el(
            'div',
            { class: 'ag-auth-header' },
            el(
              'button',
              {
                class: 'ag-icon-btn',
                onClick: () => {
                  step = 'welcome';
                  render();
                },
              },
              icons.back(22),
            ),
            el('h2', null, t.auth.createTitle),
          ),
          el('p', { class: 'ag-auth-subtitle', style: 'text-align:left' }, t.auth.createSubtitle),
          el(
            'div',
            { class: 'ag-field' },
            el('label', null, t.auth.displayNameLabel),
            nameInput,
          ),
          el(
            'div',
            { class: 'ag-field' },
            el('label', null, t.auth.usernameLabel),
            userInput,
          ),
          el(
            'div',
            { class: 'ag-auth-actions' },
            el(
              'button',
              {
                class: 'ag-pill-btn ag-pill-btn--primary',
                onClick: async () => {
                  draftIdentity = await authStore.generateDraftIdentity(
                    nameInput.value,
                    userInput.value,
                  );
                  step = 'backup';
                  render();
                },
              },
              t.auth.generateBtn,
            ),
          ),
        ),
      );
      return;
    }

    if (step === 'backup' && draftIdentity) {
      const identity = draftIdentity;
      const seedText = identity.seedPhrase.join(' ');
      root.appendChild(
        el(
          'div',
          { class: 'ag-auth-card ag-auth-card--boxed' },
          el(
            'div',
            { class: 'ag-auth-header' },
            el(
              'button',
              {
                class: 'ag-icon-btn',
                onClick: () => {
                  step = 'create';
                  render();
                },
              },
              icons.back(22),
            ),
            el('h2', null, t.auth.backupTitle),
          ),
          el('p', { class: 'ag-auth-subtitle', style: 'text-align:left' }, t.auth.backupSubtitle),
          el(
            'div',
            { class: 'ag-seed-grid' },
            ...identity.seedPhrase.map((word, idx) =>
              el(
                'div',
                { class: 'ag-seed-word' },
                el('span', null, `${idx + 1}`),
                el('strong', null, word),
              ),
            ),
          ),
          el(
            'div',
            { class: 'ag-auth-actions' },
            el(
              'button',
              {
                class: 'ag-pill-btn ag-pill-btn--secondary',
                onClick: async () => {
                  await copyToClipboard(seedText);
                  showToast(t.common.copied);
                },
              },
              icons.copy(16),
              t.auth.copySeedBtn,
            ),
            el(
              'button',
              {
                class: 'ag-pill-btn ag-pill-btn--primary',
                onClick: () => authStore.confirmIdentity(identity),
              },
              t.auth.savedConfirmBtn,
            ),
          ),
        ),
      );
      return;
    }

    // restore
    const seedArea = el('textarea', {
      class: 'ag-textarea',
      placeholder: t.auth.restorePlaceholder,
    }) as HTMLTextAreaElement;

    root.appendChild(
      el(
        'div',
        { class: 'ag-auth-card ag-auth-card--boxed' },
        el(
          'div',
          { class: 'ag-auth-header' },
          el(
            'button',
            {
              class: 'ag-icon-btn',
              onClick: () => {
                step = 'welcome';
                render();
              },
            },
            icons.back(22),
          ),
          el('h2', null, t.auth.restoreTitle),
        ),
        el('p', { class: 'ag-auth-subtitle', style: 'text-align:left' }, t.auth.restoreSubtitle),
        seedArea,
        el('div', { class: 'ag-auth-error' }, restoreError),
        el(
          'div',
          { class: 'ag-auth-actions' },
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--primary',
              onClick: async () => {
                const res = await authStore.restoreFromSeed(seedArea.value);
                if (!res) {
                  restoreError = t.auth.invalidSeedError;
                  render();
                }
              },
            },
            t.auth.restoreSubmitBtn,
          ),
        ),
      ),
    );
  };

  render();
  return root;
}
