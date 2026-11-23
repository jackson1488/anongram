import { el } from '../../../../core/ui/dom';
import { epochDateLabel } from '../../i18n';

/** Android DateChip: centered date separator ("Today", "Yesterday", "27 October"). */
export function createDateChip(epochMs: number): HTMLElement {
  return el('div', { class: 'ag-cs-datechip' }, el('span', null, epochDateLabel(epochMs)));
}
