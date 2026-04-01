package org.rights.locker.Utilities;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory;
import com.drew.metadata.mov.media.QuickTimeVideoDirectory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.DTOs.MediaMetadata;
import org.rights.locker.Repos.MetadataService; // keep your interface as-is
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataServiceImpl implements MetadataService {

    private final ObjectMapper om = new ObjectMapper();

    // configurable via env; defaults to PATH lookup
    private final String ffprobePath = System.getenv().getOrDefault("FFPROBE_PATH", "ffprobe");

    @Override
    public MediaMetadata extractFromUrl(String url) throws Exception {
        // 1) metadata-extractor (images + some MOV/MP4 atoms)
        MediaMetadata mm1 = null;
        try (InputStream in = new URL(url).openStream()) {
            mm1 = fromMetadataExtractor(ImageMetadataReader.readMetadata(in));
        } catch (Throwable t) {
            log.debug("metadata-extractor failed (ok): {}", t.toString());
        }

        // 2) ffprobe (video/container/streams/duration/rotation/fps)
        MediaMetadata mm2 = null;
        try {
            mm2 = fromFfprobe(url);
        } catch (Throwable t) {
            log.debug("ffprobe failed (ok for images): {}", t.toString());
        }

        return merge(mm1, mm2);
    }

    /* ===================== helpers ===================== */

    private MediaMetadata fromMetadataExtractor(Metadata md) {
        Instant dateOriginal = null;
        Integer tzMin = null;

        var sub = md.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (sub != null) {
            // Date/Time Original
            var d = sub.getDateOriginal();
            if (d != null) dateOriginal = d.toInstant();

            // EXIF OffsetTimeOriginal (preferred over hacking DateTimeOriginal string)
            String off = sub.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (off != null && off.matches("^[+\\-]\\d{2}:?\\d{2}$")) {
                int sign = off.charAt(0) == '-' ? -1 : 1;
                int hours = Integer.parseInt(off.substring(1,3));
                int mins  = Integer.parseInt(off.substring(off.length()-2));
                tzMin = sign * (hours * 60 + mins);
            }
        }

        Double lat=null, lon=null, alt=null, heading=null;
        var gps = md.getFirstDirectoryOfType(GpsDirectory.class);
        if (gps != null) {
            GeoLocation gl = gps.getGeoLocation();
            if (gl != null && !gl.isZero()) { lat = gl.getLatitude(); lon = gl.getLongitude(); }
            if (gps.containsTag(GpsDirectory.TAG_ALTITUDE))
                alt = gps.getRational(GpsDirectory.TAG_ALTITUDE).doubleValue();
            if (gps.containsTag(GpsDirectory.TAG_IMG_DIRECTION))
                heading = gps.getRational(GpsDirectory.TAG_IMG_DIRECTION).doubleValue();
        }

        String make=null, model=null, lens=null, software=null;
        Integer orientation=null, w=null, h=null;
        var ifd0 = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd0 != null) {
            make = ifd0.getString(ExifIFD0Directory.TAG_MAKE);
            model = ifd0.getString(ExifIFD0Directory.TAG_MODEL);
            software = ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE);
            orientation = ifd0.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
        }
        var jpeg = md.getFirstDirectoryOfType(JpegDirectory.class);
        if (jpeg != null) {
            w = jpeg.getInteger(JpegDirectory.TAG_IMAGE_WIDTH);
            h = jpeg.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT);
        }

        Integer rot=null;
        var qtVid = md.getFirstDirectoryOfType(QuickTimeVideoDirectory.class);
        if (qtVid != null) rot = qtVid.getInteger(QuickTimeVideoDirectory.TAG_ROTATION);

        // QuickTime location when no EXIF GPS
        if (lat == null || lon == null) {
            var qtMeta = md.getFirstDirectoryOfType(QuickTimeMetadataDirectory.class);
            if (qtMeta != null) {
                String iso6709 = qtMeta.getString(QuickTimeMetadataDirectory.TAG_LOCATION_ISO6709);
                double[] p = parseIso6709(iso6709);
                if (p != null) { lat = p[0]; lon = p[1]; }
            }
        }

        Map<String,Object> raw = new LinkedHashMap<>();
        try {
            md.getDirectories().forEach(d -> raw.put(d.getName(), d.getTags()));
        } catch (Exception ignored){}

        return new MediaMetadata(
                dateOriginal, tzMin,
                lat, lon, alt, heading,
                make, model, lens, software,
                w, h, orientation,
                null, null, null,
                null, null, rot,
                null,
                MediaMetadata.safeRaw(raw)
        );
    }

    private MediaMetadata fromFfprobe(String url) throws Exception {
        Process p = new ProcessBuilder(
                ffprobePath, "-v", "error", "-print_format", "json", "-show_format", "-show_streams", url
        ).redirectErrorStream(true).start();

        String json = new String(p.getInputStream().readAllBytes());
        p.waitFor();

        JsonNode root = om.readTree(json);

        String container = val(root, "format.format_name"); // e.g., mov,mp4,m4a,3gp,3g2,mj2
        Long durationMs = null;
        if (root.path("format").has("duration")) {
            try { durationMs = Math.round(1000 * root.path("format").path("duration").asDouble()); }
            catch (Exception ignored) {}
        }

        String vCodec=null, aCodec=null;
        Integer w=null, h=null, rotation=null;
        Double fps=null;

        for (JsonNode s : root.path("streams")) {
            String type = s.path("codec_type").asText();
            if ("video".equals(type)) {
                vCodec = s.path("codec_name").asText(null);
                int wi = s.path("width").asInt(0);
                int he = s.path("height").asInt(0);
                w = wi > 0 ? wi : null;
                h = he > 0 ? he : null;
                if (s.path("tags").has("rotate")) {
                    rotation = s.path("tags").path("rotate").asInt();
                }
                // FPS
                String rate = s.path("r_frame_rate").asText("0/1");
                String[] pr = rate.split("/");
                if (pr.length == 2) {
                    try {
                        double num = Double.parseDouble(pr[0]);
                        double den = Double.parseDouble(pr[1]);
                        fps = den == 0 ? null : num / den;
                    } catch (NumberFormatException ignored){}
                }
            } else if ("audio".equals(type)) {
                aCodec = s.path("codec_name").asText(null);
            }
        }

        Map<String,Object> raw = new LinkedHashMap<>();
        raw.put("ffprobe", root);

        return new MediaMetadata(
                null, null, null, null, null, null,
                null, null, null, null,
                w, h, rotation,
                container, vCodec, aCodec,
                durationMs, fps, rotation,
                null,
                MediaMetadata.safeRaw(raw)
        );
    }

    /* ----------------- misc utils ----------------- */

    private static String val(JsonNode n, String dotted) {
        JsonNode cur = n;
        for (String part : dotted.split("\\.")) cur = cur.path(part);
        return cur.isMissingNode() ? null : cur.asText(null);
    }

    /** ISO 6709 like "+47.6155-122.3463+012.3/" */
    private static double[] parseIso6709(String s){
        if (s == null || s.isBlank()) return null;
        try {
            int i = Math.max(s.indexOf('-',1), s.indexOf('+',1));
            double lat = Double.parseDouble(s.substring(0, i));
            int j = Math.max(s.indexOf('-', i+1), s.indexOf('+', i+1));
            double lon = Double.parseDouble(j>0 ? s.substring(i, j) : s.substring(i));
            return new double[]{lat, lon};
        } catch(Exception e){ return null; }
    }

    private MediaMetadata merge(MediaMetadata a, MediaMetadata b){
        if (a == null) return b != null ? b : MediaMetadata.empty();
        if (b == null) return a;

        // merge raw maps
        Map<String,Object> raw = new LinkedHashMap<>();
        if (a.raw() != null) raw.putAll(a.raw());
        if (b.raw() != null) raw.putAll(b.raw());

        return new MediaMetadata(
                nz(a.dateOriginal(), b.dateOriginal()),
                nz(a.tzMinutes(),   b.tzMinutes()),
                nz(a.lat(), b.lat()),
                nz(a.lon(), b.lon()),
                nz(a.altitudeM(), b.altitudeM()),
                nz(a.headingDeg(), b.headingDeg()),

                nz(a.cameraMake(),  b.cameraMake()),
                nz(a.cameraModel(), b.cameraModel()),
                nz(a.lensModel(),   b.lensModel()),
                nz(a.software(),    b.software()),
                nz(a.widthPx(),     b.widthPx()),
                nz(a.heightPx(),    b.heightPx()),
                nz(a.orientationDeg(), b.orientationDeg()),

                nz(a.container(),   b.container()),
                nz(a.videoCodec(),  b.videoCodec()),
                nz(a.audioCodec(),  b.audioCodec()),
                nz(a.durationMs(),  b.durationMs()),
                nz(a.videoFps(),    b.videoFps()),
                nz(a.videoRotationDeg(), b.videoRotationDeg()),
                nz(a.authenticityAssessment(), b.authenticityAssessment()),
                MediaMetadata.safeRaw(raw)
        );
    }

    private static <T> T nz(T x, T y){ return x != null ? x : y; }

    // If you ever need to parse EXIF date strings with offsets safely:
    @SuppressWarnings("unused")
    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return OffsetDateTime.parse(iso).toInstant(); }
        catch (DateTimeParseException ex) {
            try { return Instant.parse(iso); } catch (Exception ignored) { return null; }
        }
    }
}
