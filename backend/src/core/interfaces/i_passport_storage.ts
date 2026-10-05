export interface PassportRecord {
  publicId: string;
  version: number;
  blobBase64: string;
  updatedAt: number;
}

export interface IPassportStorage {
  savePassport(record: PassportRecord): Promise<boolean>;
  getPassport(publicId: string): Promise<PassportRecord | null>;
}
