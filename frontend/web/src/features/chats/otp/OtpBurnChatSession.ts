import type { ChatThread } from '../models';

/** Port of Android features/chats/otp/OtpBurnChatSession (session kind marker; transport is mocked in web). */
export const SESSION_KIND = 'otp-burn' as const;

export function isOtpBurnChat(thread: ChatThread): boolean {
  return !!thread.isOtpBurn;
}
