package org.rights.locker.Services;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class FfmpegService {

    public Path transcodeToStandard(Path input, Path output) throws IOException, InterruptedException {
        // Example: 1080p max, ~3 Mbps, strip metadata, faststart for web playback
        String[] cmd = {
                "ffmpeg",
                "-y",                          // overwrite
                "-i", input.toString(),
                "-vf", "scale=min(1920\\,iw):-2",  // keep aspect, cap width at 1920
                "-c:v", "libx264",             // or libx265/av1 (see note below)
                "-preset", "veryfast",         // speed/quality tradeoff
                "-b:v", "3M",
                "-movflags", "+faststart",     // web-friendly
                "-map_metadata", "-1",         // strip EXIF/GPS etc.
                "-c:a", "aac",
                "-b:a", "128k",
                output.toString()
        };

        Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
        try (var in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            in.lines().forEach(line -> LoggerFactory.getLogger(getClass()).info("[ffmpeg] {}", line));
            boolean finished = p.waitFor(5, TimeUnit.MINUTES);
            if (!finished) { p.destroyForcibly(); throw new IOException("ffmpeg timed out"); }
        }
        if (p.waitFor() != 0) throw new IOException("ffmpeg failed, exit=" + p.exitValue());
        return output;
    }

    public Path thumbnail(Path input, Path jpgOut) throws IOException, InterruptedException {
        String[] cmd = {
                "ffmpeg","-y","-ss","00:00:03","-i", input.toString(), "-vframes","1", jpgOut.toString()
        };
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        if (p.waitFor() != 0) throw new IOException("ffmpeg thumbnail failed");
        return jpgOut;
    }

}
