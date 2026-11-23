import type { ChatThread } from '../models';

/** Port of Android features/chats/group/GroupChatSession (session kind marker; transport is mocked in web). */
export const SESSION_KIND = 'group' as const;

export function isGroupChat(thread: ChatThread): boolean {
  return thread.id.startsWith('group-');
}
