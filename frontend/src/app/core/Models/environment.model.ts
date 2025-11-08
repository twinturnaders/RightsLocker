export interface Evidence {
  id: string;
  title?: string | null;
  description?: string | null;

  capturedAt?: string | null;   // ISO
  createdAt?: string | null;    // ISO
  updatedAt?: string | null;    // ISO

  captureLatlon?: string | null;   // your format (e.g., "46.87,-113.99")
  captureAccuracyM?: number | null;

  originalKey?: string | null;
  originalSizeB?: number | null;
  originalSha256?: string | null;

  redactedKey?: string | null;
  redactedSize?: number | null;
  thumbnailKey?: string | null;

  status?: 'RECEIVED'|'PROCESSING'|'READY'|'ERROR'|string;
  legalHold?: boolean | null;

  // rich metadata
  exifDateOriginal?: string | null;   // ISO
  tzOffsetMinutes?: number | null;
  captureAltitudeM?: number | null;
  captureHeadingDeg?: number | null;

  cameraMake?: string | null;
  cameraModel?: string | null;
  lensModel?: string | null;
  software?: string | null;

  widthPx?: number | null;
  heightPx?: number | null;
  orientationDeg?: number | null;

  container?: string | null;
  videoCodec?: string | null;
  audioCodec?: string | null;
  durationMs?: number | null;
  videoFps?: number | null;
  videoRotationDeg?: number | null;
}
