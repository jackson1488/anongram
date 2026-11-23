import { i18nStore } from '../../../core/i18n/i18nStore';
import { kvStore } from '../../../core/storage/kvStore';
import { clear, el } from '../../../core/ui/dom';
import { icons, tgIcons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { servicesT } from '../i18n';

export interface OnionHop {
  role: string;
  country: string;
  node: string;
  latencyMs: number;
}

export interface OnionState {
  vpnEnabled: boolean;
  killSwitchEnabled: boolean;
  hops: OnionHop[];
}

const KEY_ONION = 'services.onion.v1';

const REGIONS = ['CH', 'IS', 'RO', 'NL', 'SE', 'DE', 'FI', 'NO'];

function randomHops(): OnionHop[] {
  const pick = () => REGIONS[Math.floor(Math.random() * REGIONS.length)];
  const id = () => Math.random().toString(36).slice(2, 6).toUpperCase();
  return [
    { role: 'Entry Guard', country: pick(), node: `guard-${id()}.onion`, latencyMs: 12 + Math.floor(Math.random() * 14) },
    { role: 'Middle Relay', country: pick(), node: `relay-${id()}.onion`, latencyMs: 18 + Math.floor(Math.random() * 18) },
    { role: 'Exit Node', country: pick(), node: `exit-${id()}.onion`, latencyMs: 11 + Math.floor(Math.random() * 15) },
  ];
}

class OnionStore {
  private state: OnionState;
  private listeners = new Set<() => void>();

  constructor() {
    const saved = kvStore.getJson<OnionState | null>(KEY_ONION, null);
    this.state =
      saved && Array.isArray(saved.hops) && saved.hops.length === 3
        ? saved
        : { vpnEnabled: true, killSwitchEnabled: true, hops: randomHops() };
  }

  private persist(): void {
    kvStore.setJson(KEY_ONION, this.state);
    for (const fn of this.listeners) fn();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  getState(): OnionState {
    return this.state;
  }

  toggleVpn(): void {
    this.state = { ...this.state, vpnEnabled: !this.state.vpnEnabled };
    this.persist();
  }

  toggleKillSwitch(): void {
    this.state = { ...this.state, killSwitchEnabled: !this.state.killSwitchEnabled };
    this.persist();
  }

  rotateCircuit(): void {
    this.state = { ...this.state, hops: randomHops() };
    this.persist();
  }
}

export const onionStore = new OnionStore();

export function createOnionView(onBack: () => void): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });

  const render = () => {
    clear(root);
    const s = servicesT();
    const st = onionStore.getState();

    root.appendChild(
      el(
        'div',
        { class: 'ag-chat-header' },
        el('button', { class: 'ag-icon-btn', onClick: onBack }, icons.back(20)),
        el('div', { class: 'ag-chat-header__title' }, s.onionHeading),
      ),
    );

    const vpnCheck = el('input', {
      type: 'checkbox',
      style: 'width:18px;height:18px;accent-color:var(--ag-accent);cursor:pointer;',
      onChange: () => onionStore.toggleVpn(),
    }) as HTMLInputElement;
    vpnCheck.checked = st.vpnEnabled;

    const ksCheck = el('input', {
      type: 'checkbox',
      style: 'width:18px;height:18px;accent-color:var(--ag-danger);cursor:pointer;',
      onChange: () => onionStore.toggleKillSwitch(),
    }) as HTMLInputElement;
    ksCheck.checked = st.killSwitchEnabled;

    const totalMs = st.hops.reduce((acc, h) => acc + h.latencyMs, 0);

    root.appendChild(
      el(
        'div',
        { class: 'ag-scroll-list ag-services-scroll' },
        el(
          'label',
          { class: 'ag-contact-switch-row' },
          el(
            'div',
            { style: 'display:flex;flex-direction:column;gap:2px;' },
            el('strong', { style: 'font-size:14px;color:var(--ag-text-primary);' }, s.onionToggleVpn),
            el('span', { style: 'font-size:12px;color:var(--ag-text-secondary);' }, st.vpnEnabled ? s.onionActive : s.onionPaused),
          ),
          vpnCheck,
        ),
        el(
          'label',
          { class: 'ag-contact-switch-row' },
          el(
            'div',
            { style: 'display:flex;flex-direction:column;gap:2px;' },
            el('strong', { style: 'font-size:14px;color:var(--ag-text-primary);' }, s.onionToggleKillSwitch),
            el('span', { style: 'font-size:12px;color:var(--ag-text-secondary);' }, s.killSwitchDetail),
          ),
          ksCheck,
        ),
        el(
          'div',
          { class: 'ag-card ag-settings-card', style: 'gap:10px;' },
          el(
            'div',
            { style: 'display:flex;align-items:center;justify-content:space-between;' },
            el('strong', { style: 'font-size:14px;color:var(--ag-text-primary);' }, s.onionCircuitTitle),
            el('span', { class: 'ag-chip' }, `~${totalMs} ms`),
          ),
          ...st.hops.map((h, idx) => {
            const hopRole = idx === 0 ? s.onionHopGuard : idx === 1 ? s.onionHopRelay : s.onionHopExit;
            return el(
              'div',
              { class: 'ag-onion-hop-card' },
              el(
                'div',
                { style: 'display:flex;flex-direction:column;gap:2px;' },
                el('strong', { style: 'font-size:13px;color:var(--ag-text-primary);' }, `${idx + 1}. ${hopRole} (${h.country})`),
                el('span', { style: 'font-size:11px;color:var(--ag-text-secondary);font-family:monospace;' }, h.node),
              ),
              el('span', { style: 'font-size:12px;color:var(--ag-success);font-weight:600;' }, `${h.latencyMs} ms`),
            );
          }),
          el('div', { style: 'font-size:11px;color:var(--ag-text-muted);' }, s.onionCamouflage),
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--primary',
              onClick: () => {
                onionStore.rotateCircuit();
                showToast(s.onionRotatedToast);
              },
            },
            tgIcons.refresh(16),
            s.onionRotateBtn,
          ),
        ),
      ),
    );
  };

  const unsubOnion = onionStore.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);
  render();
  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubOnion();
    unsubI18n();
  };
  return root;
}
