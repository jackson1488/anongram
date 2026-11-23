import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { mainMenuT } from '../i18n';

export function createOnionOfflineBanner(): HTMLElement {
  return el('div', { class: 'ag-onion-banner' }, icons.alert(16), el('span', null, mainMenuT().onionOffline));
}
