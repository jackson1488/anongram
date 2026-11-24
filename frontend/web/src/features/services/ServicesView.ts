import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { createCallsView } from '../calls/CallsView';
import { servicesT } from './i18n';
import { createOnionView, onionStore } from './onion/OnionView';
import { createOtpNotepadView, otpStore } from './otp/OtpNotepadView';
import './services.css';

type ServicesSubView = 'hub' | 'onion' | 'otp' | 'calls';

export function createServicesView(): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });
  let subView: ServicesSubView = 'hub';
  let childCleanup: (() => void) | null = null;

  const mountSubView = (view: HTMLElement) => {
    childCleanup?.();
    childCleanup = (view as unknown as { __cleanup?: () => void }).__cleanup ?? null;
    root.appendChild(view);
  };

  const goHub = () => {
    subView = 'hub';
    render();
  };

  const render = () => {
    childCleanup?.();
    childCleanup = null;
    clear(root);
    const s = servicesT();

    if (subView === 'onion') {
      mountSubView(createOnionView(goHub));
      return;
    }

    if (subView === 'otp') {
      mountSubView(createOtpNotepadView(goHub));
      return;
    }

    if (subView === 'calls') {
      root.appendChild(
        el(
          'div',
          { class: 'ag-chat-header' },
          el('button', { class: 'ag-icon-btn', onClick: goHub }, icons.back(20)),
          el('div', { class: 'ag-chat-header__title' }, s.callsTitle),
        ),
      );
      mountSubView(createCallsView());
      return;
    }

    const onionState = onionStore.getState();
    const otpCount = otpStore.getPages().length;

    root.appendChild(
      el(
        'div',
        { class: 'ag-pane-header' },
        el('h1', { class: 'ag-pane-title' }, s.title),
      ),
    );

    const cards = [
      {
        emoji: '🧅',
        badgeClass: 'is-success',
        title: s.onionTitle,
        sub: onionState.vpnEnabled ? s.onionActive : s.onionPaused,
        detail: s.onionDetail,
        action: () => {
          subView = 'onion';
          render();
        },
      },
      {
        emoji: '🛡️',
        badgeClass: 'is-danger',
        title: s.killSwitchTitle,
        sub: onionState.killSwitchEnabled ? s.killSwitchArmed : s.killSwitchDisarmed,
        detail: s.killSwitchDetail,
        action: () => {
          subView = 'onion';
          render();
        },
      },
      {
        emoji: '🔥',
        badgeClass: 'is-warning',
        title: s.otpTitle,
        sub: s.otpRemaining(otpCount),
        detail: s.otpDetail,
        action: () => {
          subView = 'otp';
          render();
        },
      },
      {
        emoji: '📞',
        badgeClass: '',
        title: s.callsTitle,
        sub: s.callsSub,
        detail: s.callsDetail,
        action: () => {
          subView = 'calls';
          render();
        },
      },
    ];

    root.appendChild(
      el(
        'div',
        { class: 'ag-scroll-list ag-services-scroll' },
        ...cards.map((c) =>
          el(
            'div',
            { class: 'ag-service-card-v2', onClick: c.action },
            el('div', { class: `ag-service-icon-badge ${c.badgeClass}` }, c.emoji),
            el(
              'div',
              { class: 'ag-service-card-v2__body' },
              el('div', { class: 'ag-service-card-v2__title' }, c.title),
              el('div', { class: 'ag-service-card-v2__sub' }, c.sub),
              el('div', { class: 'ag-service-card-v2__detail' }, c.detail),
            ),
            icons.chevronRight(18),
          ),
        ),
      ),
    );
  };

  const unsubOnion = onionStore.subscribe(() => {
    if (subView === 'hub') render();
  });
  const unsubOtp = otpStore.subscribe(() => {
    if (subView === 'hub') render();
  });
  const unsubI18n = i18nStore.subscribe(render);

  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    childCleanup?.();
    unsubOnion();
    unsubOtp();
    unsubI18n();
  };

  return root;
}
