import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { createSettingsToggleGroup } from '../common/SettingsToggleRow';
import { SettingsI18n } from '../i18n';
import { settingsStore, type AutoLockOption } from '../settingsStore';

const AUTO_LOCK_OPTIONS: AutoLockOption[] = ['off', '1m', '5m', '1h'];

export function createSecuritySettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-settings-content' });
  let showPinEditor = false;

  const render = () => {
    clear(root);
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;

    // 1. Passcode Lock Card (Toggle + Inline PIN Setter)
    const sw = el(
      'div',
      { class: `ag-switch ${prefs.passcodeEnabled ? 'is-on' : ''}` },
      el('div', { class: 'ag-switch__thumb' }),
    );

    const passcodeCard = el(
      'div',
      { class: 'ag-card ag-settings-card' },
      el(
        'div',
        {
          class: 'ag-settings-row',
          style: 'padding:0;border-bottom:none',
          onClick: () => {
            if (prefs.passcodeEnabled) {
              settingsStore.disablePasscode();
              showPinEditor = false;
              showToast(s.pinDisabledToast);
            } else {
              showPinEditor = !showPinEditor;
              render();
            }
          },
        },
        el('span', { class: 'ag-settings-row__icon' }, icons.lock(18)),
        el('span', { class: 'ag-settings-row__label' }, s.passcodeEnabled),
        sw,
      ),
    );

    if (prefs.passcodeEnabled && !showPinEditor) {
      passcodeCard.appendChild(
        el(
          'button',
          {
            class: 'ag-pill-btn ag-pill-btn--secondary',
            onClick: () => {
              showPinEditor = true;
              render();
            },
          },
          s.changePasscodeBtn,
        ),
      );
    }

    if (showPinEditor) {
      const pin1 = el('input', {
        type: 'password',
        inputmode: 'numeric',
        maxlength: 6,
        class: 'ag-input',
        placeholder: s.enterPinPlaceholder,
      }) as HTMLInputElement;
      const pin2 = el('input', {
        type: 'password',
        inputmode: 'numeric',
        maxlength: 6,
        class: 'ag-input',
        placeholder: s.confirmPinPlaceholder,
      }) as HTMLInputElement;

      passcodeCard.appendChild(
        el(
          'div',
          { class: 'ag-pin-form' },
          el('div', { class: 'ag-section-label' }, s.setPasscodeTitle),
          pin1,
          pin2,
          el(
            'div',
            { class: 'ag-inline-row' },
            el(
              'button',
              {
                class: 'ag-pill-btn ag-pill-btn--primary',
                onClick: () => {
                  const v1 = pin1.value.trim();
                  const v2 = pin2.value.trim();
                  if (!/^\d{4,6}$/.test(v1)) {
                    showToast(s.pinTooShort);
                    return;
                  }
                  if (v1 !== v2) {
                    showToast(s.pinMismatch);
                    return;
                  }
                  settingsStore.setPasscode(v1);
                  showPinEditor = false;
                  showToast(s.pinSavedToast);
                },
              },
              s.savePinBtn,
            ),
            el(
              'button',
              {
                class: 'ag-pill-btn ag-pill-btn--ghost',
                onClick: () => {
                  showPinEditor = false;
                  render();
                },
              },
              s.cancelBtn,
            ),
          ),
        ),
      );
    }

    if (prefs.passcodeEnabled) {
      passcodeCard.appendChild(el('div', { class: 'ag-section-label' }, s.autoLockTitle));
      passcodeCard.appendChild(
        el(
          'div',
          { class: 'ag-chips-wrap' },
          ...AUTO_LOCK_OPTIONS.map((opt) =>
            el(
              'button',
              {
                class: `ag-chip ${prefs.autoLockTimeout === opt ? 'is-active' : ''}`,
                onClick: () => settingsStore.set('autoLockTimeout', opt),
              },
              s.autoLockOptions[opt],
            ),
          ),
        ),
      );
    }

    root.appendChild(passcodeCard);

    // 2. Biometrics, Screenshot Protection, 2FA
    root.appendChild(
      createSettingsToggleGroup([
        [s.biometricLock, 'biometricLock'],
        [s.blockScreenshots, 'blockScreenshots'],
        [s.twoFactorEnabled, 'twoFactorEnabled'],
      ]),
    );

    // 3. Post-Quantum / E2EE Crypto Status Card
    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el(
          'div',
          { style: 'display:flex;align-items:center;gap:10px' },
          el('span', { class: 'ag-settings-row__icon' }, icons.shield(20)),
          el('strong', null, s.cryptoStatusTitle),
        ),
        el('div', { style: 'font-size:13px;color:var(--ag-text-secondary);line-height:1.45' }, s.cryptoStatusDesc),
      ),
    );
  };

  const u1 = settingsStore.subscribe(render);
  const u2 = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
  };

  return root;
}
