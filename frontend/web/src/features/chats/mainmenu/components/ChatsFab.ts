import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { mainMenuT } from '../i18n';

export function createChatsFab(onClick: () => void): HTMLElement {
  const label = mainMenuT().newMessage;
  return el(
    'button',
    { type: 'button', class: 'ag-chats-fab', title: label, 'aria-label': label, onClick },
    icons.edit(24),
  );
}
