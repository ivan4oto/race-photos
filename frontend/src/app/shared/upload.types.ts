export type UploadStatus = 'queued' | 'skipped' | 'presigning' | 'uploading' | 'success' | 'error';

export interface UploadItem {
  file: File;
  key: string; // S3 key to upload to
  name: string; // original file name
  size: number;
  status: UploadStatus;
  progress: number; // 0-100
  errorMessage?: string;
  skippedReason?: string;
}

export interface PresignedDto {
  name: string;
  url: string;
}