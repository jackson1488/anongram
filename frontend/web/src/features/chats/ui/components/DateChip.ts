import { el } from '../../../../core/ui/dom';
import { epochDateLabel } from '../../i18n';

/** Inline date separator ("Today", "Yesterday", "27 October") with subtle Telegram glass pill look. */
export function createDateChip(epochMs: number): HTMLElement {
  const pill = el(
    'span',
    {
      class: 'ag-cs-datechip__pill',
      style: [
        'display:inline-flex',
        'align-items:center',
        'justify-content:center',
        'padding:4px 12px',
        'border-radius:9999px',
        'background:color-mix(in srgb, var(--ag-island-bg) 72%, transparent)',
        'border:1px solid color-mix(in srgb, var(--ag-island-border) 82%, transparent)',
        'box-shadow:0 1px 4px color-mix(in srgb, var(--ag-shadow) 30%, transparent)',
        'color:var(--ag-text-secondary)',
        'font-size:12px',
        'font-weight:600',
        'line-height:1.3',
        'letter-spacing:0.01em',
        'backdrop-filter:blur(12px)',
        '-webkit-backdrop-filter:blur(12px)',
      ].join(';'),
    },
    epochDateLabel(epochMs),
  );
  return el(
    'div',
    {
      class: 'ag-cs-datechip is-inline',
      style: 'position:static;top:auto;z-index:1;display:flex;justify-content:center;padding:8px 0 4px;pointer-events:none;',
    },
    pill,
  );
}
