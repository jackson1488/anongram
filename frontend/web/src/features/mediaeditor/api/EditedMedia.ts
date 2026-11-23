export interface EditableMediaItem {
  id: string;
  source: Blob | File;
}

export interface EditedMedia {
  id: string;
  blob: Blob;
}
