package org.rights.locker.DTOs;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record MediaMetadata(
        // common
        Instant dateOriginal,
        Integer tzMinutes,
        Double  lat,
        Double  lon,
        Double  altitudeM,
        Double  headingDeg,

        // photo-ish
        String  cameraMake,
        String  cameraModel,
        String  lensModel,
        String  software,
        Integer widthPx,
        Integer heightPx,
        Integer orientationDeg,   // EXIF orientation (1..8) or degrees if normalized

        // video-ish
        String  container,        // e.g. "mov,mp4,m4a,3gp,3g2,mj2"
        String  videoCodec,       // e.g. "h264"
        String  audioCodec,       // e.g. "aac"
        Long    durationMs,       // milliseconds
        Double  videoFps,         // frames per second
        Integer videoRotationDeg, // 0/90/180/270

        // heuristic authenticity signals
        AuthenticityAssessment authenticityAssessment,

        // debug / pass-through raw tags
        Map<String,Object> raw
) {
    public static MediaMetadata empty() {
        return new MediaMetadata(
                null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null,
                Collections.emptyMap()
        );
    }

    public MediaMetadata withAuthenticityAssessment(AuthenticityAssessment authenticityAssessment) {
        return new MediaMetadata(
                dateOriginal, tzMinutes, lat, lon, altitudeM, headingDeg,
                cameraMake, cameraModel, lensModel, software, widthPx, heightPx, orientationDeg,
                container, videoCodec, audioCodec, durationMs, videoFps, videoRotationDeg,
                authenticityAssessment,
                raw
        );
    }

    /** defensive copy + unmodifiable */
    public static Map<String,Object> safeRaw(Map<String,Object> m){
        if (m == null || m.isEmpty()) return Collections.emptyMap();
        return Collections.unmodifiableMap(new LinkedHashMap<>(m));
    }
}
