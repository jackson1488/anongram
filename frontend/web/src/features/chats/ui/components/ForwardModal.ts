import type { ChatMessage } from '../../models';
import { openForwardTargetPicker } from '../../tools/ForwardTargetPickerScreen';

/** Anonymous forward (forwardedFrom = null always) of one or many messages. */
export function openForwardModal(messages: ChatMessage[], onDone?: () => void): void {
  openForwardTargetPicker({ messages, onDone });
}
