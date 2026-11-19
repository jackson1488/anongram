import { findSuggestions, normalizeSeed } from '../../core/crypto/bip39';
import { AVATAR_GRADIENTS, type UserIdentity } from '../../core/crypto/identity';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { authStore } from './authStore';
import { createWelcomeShieldAnimation } from './ui/welcome/WelcomeShieldAnimation';
import './auth.css';

type AuthStep = 'welcome' | 'create' | 'backup' | 'restore';

export function renderAuthShield(): HTMLElement {
  const shield = createWelcomeShieldAnimation(130);
  const root = el(
    'div',
    { class: 'ag-auth-screen' },
    el(
      'div',
      { class: 'ag-auth-container', style: 'justify-content:center;align-items:center;' },
      shield,
      el('h1', { class: 'ag-welcome-title' }, 'AnonGram'),
      el('p', { class: 'ag-welcome-sub' }, 'Zero-Knowledge Blind Relay Messenger'),
    ),
  );
  (root as unknown as { __cleanup?: () => void }).__cleanup = (
    shield as unknown as { __cleanup?: () => void }
  ).__cleanup;
  return root;
}

function renderFeatureItem(icon: string, title: string, subtitle: string): HTMLElement {
  return el(
    'div',
    { class: 'ag-feature-item' },
    el('div', { class: 'ag-feature-item__icon' }, icon),
    el(
      'div',
      { class: 'ag-feature-item__body' },
      el('strong', null, title),
      el('span', null, subtitle),
    ),
  );
}

export function createAuthFlow(): HTMLElement {
  const root = el('div', { class: 'ag-auth-screen' });
  let step: AuthStep = 'welcome';
  let nickname = 'GhostWalker';
  let avatarIndex = 0;
  let genStep = 0;
  let draftIdentity: UserIdentity | null = null;
  let restoreWords = Array.from({ length: 12 }, () => '');
  let activeSlot = 0;
  let restoreError = '';
  let childCleanup: (() => void) | null = null;

  const openMemoryQuiz = (identity: UserIdentity) => {
    const overlay = el('div', {
      class: 'ag-modal-overlay',
      onClick: (e: Event) => {
        if (e.target === overlay) overlay.remove();
      },
    });
    const w3 = el('input', { class: 'ag-input', placeholder: 'Слово #3' }) as HTMLInputElement;
    const w7 = el('input', { class: 'ag-input', placeholder: 'Слово #7' }) as HTMLInputElement;
    const err = el('div', { class: 'ag-auth-error' });

    overlay.appendChild(
      el(
        'div',
        { class: 'ag-card ag-quiz-modal' },
        el('h3', { style: 'margin:0;font-size:18px;' }, 'Проверка памяти'),
        el(
          'p',
          { class: 'ag-auth-desc' },
          'Введите 3-е и 7-е слово вашей seed-фразы, чтобы подтвердить сохранение.',
        ),
        w3,
        w7,
        err,
        el(
          'div',
          { style: 'display:flex;gap:10px;' },
          el(
            'button',
            {
              class: 'ag-cta-outline',
              style: 'height:46px;',
              onClick: () => overlay.remove(),
            },
            'Отмена',
          ),
          el(
            'button',
            {
              class: 'ag-cta-gradient',
              style: 'height:46px;',
              onClick: () => {
                const a = w3.value.trim().toLowerCase();
                const b = w7.value.trim().toLowerCase();
                if (a === identity.seedPhrase[2] && b === identity.seedPhrase[6]) {
                  overlay.remove();
                  authStore.confirmIdentity(identity);
                } else {
                  err.textContent = 'Слова не совпадают. Проверьте 3-е и 7-е слово.';
                }
              },
            },
            'Подтвердить',
          ),
        ),
      ),
    );
    document.body.appendChild(overlay);
  };

  const render = () => {
    if (childCleanup) {
      childCleanup();
      childCleanup = null;
    }
    clear(root);

    if (step === 'welcome') {
      const shield = createWelcomeShieldAnimation(130);
      childCleanup = (shield as unknown as { __cleanup?: () => void }).__cleanup ?? null;

      root.appendChild(
        el(
          'div',
          { class: 'ag-auth-container' },
          el(
            'div',
            { class: 'ag-auth-top' },
            shield,
            el('h1', { class: 'ag-welcome-title' }, 'AnonGram'),
            el('div', { class: 'ag-welcome-sub' }, 'Zero-Knowledge Blind Relay Messenger'),
            el(
              'div',
              { class: 'ag-feature-list' },
              renderFeatureItem(
                '🛡️',
                '100% анонимность',
                'Без привязки к SIM-картам, email или паспортам',
              ),
              renderFeatureItem(
                '⚡',
                'Слепой релей',
                'Сервер видит только зашифрованные конверты',
              ),
              renderFeatureItem(
                '🔐',
                'Квантовая защита',
                '12 слов BIP-39 + криптография Ed25519 & ML-DSA',
              ),
            ),
          ),
          el(
            'div',
            { class: 'ag-auth-bottom' },
            el(
              'button',
              {
                class: 'ag-cta-gradient',
                onClick: () => {
                  genStep = 0;
                  draftIdentity = null;
                  step = 'create';
                  render();
                },
              },
              'Создать анонимную личность',
            ),
            el(
              'button',
              {
                class: 'ag-cta-outline',
                onClick: () => {
                  restoreError = '';
                  step = 'restore';
                  render();
                },
              },
              'Войти по Seed-фразе (12 слов)',
            ),
          ),
        ),
      );
      return;
    }

    if (step === 'create') {
      const grad = AVATAR_GRADIENTS[avatarIndex];
      const nameInput = el('input', {
        class: 'ag-input',
        value: nickname,
        placeholder: 'GhostWalker',
        onInput: (e: Event) => {
          nickname = (e.target as HTMLInputElement).value;
        },
      }) as HTMLInputElement;

      const startPipeline = async () => {
        if (genStep > 0 && genStep < 4) return;
        genStep = 1;
        render();
        await new Promise((r) => setTimeout(r, 320));
        genStep = 2;
        render();
        await new Promise((r) => setTimeout(r, 320));
        genStep = 3;
        render();
        await new Promise((r) => setTimeout(r, 320));
        draftIdentity = await authStore.generateDraftIdentity(nickname, avatarIndex);
        genStep = 4;
        render();
      };

      const steps = [
        '1. Генерация 12 слов BIP-39 энтропии',
        '2. Деривация мастер-ключа Ed25519',
        '3. Пост-квантовый отпечаток ML-DSA-87',
        '4. Анонимный ID сформирован локально',
      ];

      root.appendChild(
        el(
          'div',
          { class: 'ag-auth-container' },
          el(
            'div',
            { style: 'display:flex;flex-direction:column;gap:14px;' },
            el(
              'div',
              { class: 'ag-auth-toolbar' },
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
              el('h2', null, 'Создание личности'),
            ),
            el(
              'div',
              {
                class: 'ag-avatar-preview',
                style: `background:linear-gradient(135deg, ${grad[0]}, ${grad[1]});`,
              },
              (nickname.trim() || 'G').slice(0, 1).toUpperCase(),
            ),
            el(
              'div',
              { class: 'ag-avatar-swatches' },
              ...AVATAR_GRADIENTS.map((g, idx) =>
                el('button', {
                  class: `ag-avatar-swatch ${avatarIndex === idx ? 'is-selected' : ''}`,
                  style: `background:linear-gradient(135deg, ${g[0]}, ${g[1]});`,
                  onClick: () => {
                    avatarIndex = idx;
                    render();
                  },
                }),
              ),
            ),
            nameInput,
            el(
              'div',
              { class: 'ag-pipeline-card' },
              ...steps.map((label, idx) =>
                el(
                  'div',
                  { class: `ag-pipeline-step ${genStep > idx ? 'is-done' : ''}` },
                  genStep > idx ? icons.check(15) : icons.shield(15),
                  el('span', null, label),
                ),
              ),
              draftIdentity
                ? el('code', { class: 'ag-ag1-code', style: 'margin-top:4px;' }, draftIdentity.publicId)
                : null,
            ),
          ),
          el(
            'div',
            { class: 'ag-auth-bottom' },
            draftIdentity
              ? el(
                  'button',
                  {
                    class: 'ag-cta-gradient',
                    onClick: () => {
                      step = 'backup';
                      render();
                    },
                  },
                  'Перейти к резервной фразе',
                )
              : el(
                  'button',
                  { class: 'ag-cta-gradient', onClick: startPipeline },
                  genStep > 0 ? 'Генерация ключей...' : 'Сгенерировать ключи',
                ),
          ),
        ),
      );
      return;
    }

    if (step === 'backup' && draftIdentity) {
      const identity = draftIdentity;
      root.appendChild(
        el(
          'div',
          { class: 'ag-auth-container' },
          el(
            'div',
            { style: 'display:flex;flex-direction:column;gap:14px;' },
            el(
              'div',
              { class: 'ag-auth-toolbar' },
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
              el('h2', null, 'Резервная копия (12 слов)'),
            ),
            el(
              'div',
              { class: 'ag-warning-banner' },
              'Никому не показывайте эти 12 слов. При потере фразы восстановить доступ к аккаунту невозможно.',
            ),
            el(
              'div',
              { class: 'ag-seed-grid' },
              ...identity.seedPhrase.map((word, idx) =>
                el(
                  'div',
                  { class: 'ag-seed-word' },
                  el('span', null, `${idx + 1}.`),
                  el('strong', null, word),
                ),
              ),
            ),
          ),
          el(
            'div',
            { class: 'ag-auth-bottom' },
            el(
              'button',
              {
                class: 'ag-cta-outline',
                onClick: async () => {
                  await copyToClipboard(identity.seedPhrase.join(' '));
                  showToast('12 слов скопированы в буфер обмена');
                },
              },
              icons.copy(16),
              'Скопировать 12 слов',
            ),
            el(
              'button',
              {
                class: 'ag-cta-gradient',
                onClick: () => openMemoryQuiz(identity),
              },
              'Я записал, проверить и войти',
            ),
          ),
        ),
      );
      return;
    }

    // RestoreAccountScreen (12 slots + BIP-39 autocomplete + paste)
    const suggestions = findSuggestions(restoreWords[activeSlot] || '', 6);
    root.appendChild(
      el(
        'div',
        { class: 'ag-auth-container' },
        el(
          'div',
          { style: 'display:flex;flex-direction:column;gap:12px;' },
          el(
            'div',
            { class: 'ag-auth-toolbar' },
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
            el('h2', null, 'Восстановление (12 слов)'),
          ),
          el(
            'p',
            { class: 'ag-auth-desc' },
            'Введите 12 слов мнемоники BIP-39 по порядку или вставьте всю фразу из буфера.',
          ),
          el(
            'div',
            { class: 'ag-restore-grid' },
            ...restoreWords.map((word, idx) => {
              const inp = el('input', {
                type: 'text',
                value: word,
                placeholder: 'слово',
                onFocus: () => {
                  activeSlot = idx;
                },
                onInput: (e: Event) => {
                  const val = (e.target as HTMLInputElement).value;
                  const split = normalizeSeed(val);
                  if (split.length >= 12) {
                    restoreWords = split.slice(0, 12);
                    render();
                    return;
                  }
                  restoreWords[idx] = val.trim().toLowerCase();
                  activeSlot = idx;
                  renderSuggestions();
                },
              }) as HTMLInputElement;
              return el(
                'div',
                { class: `ag-restore-slot ${activeSlot === idx ? 'is-active' : ''}` },
                el('span', null, `${idx + 1}.`),
                inp,
              );
            }),
          ),
          (() => {
            const sugRow = el('div', { class: 'ag-suggestions-row' });
            for (const sug of suggestions) {
              sugRow.appendChild(
                el(
                  'button',
                  {
                    class: 'ag-chip',
                    onClick: () => {
                      restoreWords[activeSlot] = sug;
                      if (activeSlot < 11) activeSlot += 1;
                      render();
                    },
                  },
                  sug,
                ),
              );
            }
            return sugRow;
          })(),
          el('div', { class: 'ag-auth-error' }, restoreError),
        ),
        el(
          'div',
          { class: 'ag-auth-bottom' },
          el(
            'div',
            { style: 'display:flex;gap:10px;' },
            el(
              'button',
              {
                class: 'ag-cta-outline',
                style: 'flex:1;',
                onClick: async () => {
                  try {
                    const text = await navigator.clipboard.readText();
                    const pasted = normalizeSeed(text);
                    if (pasted.length >= 12) {
                      restoreWords = pasted.slice(0, 12);
                      restoreError = '';
                      showToast('Вставлено 12 слов мнемоники');
                      render();
                    } else {
                      showToast('В буфере меньше 12 слов');
                    }
                  } catch {
                    showToast('Вставьте фразу прямо в первый слот (Ctrl+V)');
                  }
                },
              },
              'Вставить фразу',
            ),
            el(
              'button',
              {
                class: 'ag-cta-outline',
                style: 'width:54px;',
                onClick: () => {
                  restoreWords = Array.from({ length: 12 }, () => '');
                  restoreError = '';
                  render();
                },
              },
              icons.trash(18),
            ),
          ),
          el(
            'button',
            {
              class: 'ag-cta-gradient',
              onClick: async () => {
                const filled = restoreWords.filter((w) => w.trim().length > 0);
                if (filled.length < 12) {
                  restoreError = 'Заполните все 12 слов seed-фразы';
                  render();
                  return;
                }
                await authStore.restoreFromSeed(restoreWords.join(' '));
              },
            },
            'Восстановить доступ',
          ),
        ),
      ),
    );

    function renderSuggestions(): void {
      const sugRow = root.querySelector('.ag-suggestions-row');
      if (!sugRow) return;
      clear(sugRow as HTMLElement);
      for (const sug of findSuggestions(restoreWords[activeSlot] || '', 6)) {
        sugRow.appendChild(
          el(
            'button',
            {
              class: 'ag-chip',
              onClick: () => {
                restoreWords[activeSlot] = sug;
                if (activeSlot < 11) activeSlot += 1;
                render();
              },
            },
            sug,
          ),
        );
      }
    }
  };

  render();
  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    if (childCleanup) childCleanup();
  };
  return root;
}
