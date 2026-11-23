import { el } from '../../../../core/ui/dom';
import { authT } from '../../i18n';
import { createWelcomeShieldAnimation } from './WelcomeShieldAnimation';

type Cleanable = HTMLElement & { __cleanup?: () => void };

function featureItem(icon: string, title: string, subtitle: string): HTMLElement {
  return el(
    'div',
    { class: 'ag-feature-item' },
    el('div', { class: 'ag-feature-item__icon' }, icon),
    el('div', { class: 'ag-feature-item__body' }, el('strong', null, title), el('span', null, subtitle)),
  );
}

export function renderAuthShield(): HTMLElement {
  const t = authT();
  const shield = createWelcomeShieldAnimation(130) as Cleanable;
  const root = el(
    'div',
    { class: 'ag-auth-screen' },
    el(
      'div',
      { class: 'ag-auth-container', style: 'justify-content:center;align-items:center;' },
      shield,
      el('h1', { class: 'ag-welcome-title' }, 'AnonGram'),
      el('p', { class: 'ag-welcome-sub' }, t.tagline),
    ),
  ) as Cleanable;
  root.__cleanup = shield.__cleanup;
  return root;
}

export function createWelcomeScreen(onCreate: () => void, onRestore: () => void): HTMLElement {
  const t = authT();
  const shield = createWelcomeShieldAnimation(130) as Cleanable;
  const root = el(
    'div',
    { class: 'ag-auth-container' },
    el(
      'div',
      { class: 'ag-auth-top' },
      shield,
      el('h1', { class: 'ag-welcome-title' }, 'AnonGram'),
      el('div', { class: 'ag-welcome-sub' }, t.tagline),
      el(
        'div',
        { class: 'ag-feature-list' },
        featureItem('🛡️', t.feat1Title, t.feat1Sub),
        featureItem('⚡', t.feat2Title, t.feat2Sub),
        featureItem('🔐', t.feat3Title, t.feat3Sub),
      ),
    ),
    el(
      'div',
      { class: 'ag-auth-bottom' },
      el('button', { class: 'ag-cta-gradient', onClick: onCreate }, t.createCta),
      el('button', { class: 'ag-cta-outline', onClick: onRestore }, t.restoreCta),
    ),
  ) as Cleanable;
  root.__cleanup = shield.__cleanup;
  return root;
}
