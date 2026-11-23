export interface PlayerTrack {
  id: string;
  title: string;
  subtitle: string;
  durationSec: number;
  url?: string;
  coverUrl?: string;
}

export type RepeatMode = 'off' | 'all' | 'one';

export interface PlayerState {
  track: PlayerTrack | null;
  queue: PlayerTrack[];
  index: number;
  playing: boolean;
  position: number;
  speed: number;
  shuffle: boolean;
  repeat: RepeatMode;
}
