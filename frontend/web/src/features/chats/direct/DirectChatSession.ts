import type { ChatThread } from '../models';

/** Port of Android features/chats/direct/DirectChatSession (session kind marker; transport is mocked in web). */
export const SESSION_KIND = 'direct' as const;

export function isDirectChat(thread: ChatThread): boolean {
  return !thread.isOtpBurn && !thread.id.startsWith('group-');
}
