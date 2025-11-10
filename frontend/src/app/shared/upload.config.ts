export type UploadConfig = {
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
  presignBatchSize: number;
};

export const DEFAULT_UPLOAD_CONFIG: UploadConfig = {
  acceptedMimeTypes: [
    'image/jpeg',
    'image/png',
    'image/heic',
    'image/heif',
    'image/webp',
  ],
  maxFileSizeBytes: 50 * 1024 * 1024, // 50 MB
  presignBatchSize: 100,
};

