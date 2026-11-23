import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { mainMenuT } from '../i18n';

export function createChatsFab(onClick: () => void): HTMLElement {
  return el('button', { class: 'ag-chats-fab', title: mainMenuT().newMessage, onClick }, icons.edit(22));
}
